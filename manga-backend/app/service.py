import asyncio
import json
import re
from datetime import datetime, timezone
from urllib.parse import urlsplit
from uuid import UUID, uuid5

import httpx
from fastapi import HTTPException

from app.cache import DiskImageCache, TtlCache

MANGA_NAMESPACE = UUID("e819b12e-84cb-4d9b-a359-52e8964e528b")
MANGADEX_API = "https://api.mangadex.org"
MANGADEX_COVER_HOST = "uploads.mangadex.org"
MANGADEX_PANEL_SUFFIX = ".mangadex.network"
MAX_JSON_BYTES = 2 * 1024 * 1024
MAX_IMAGE_BYTES = 20 * 1024 * 1024
MAX_CHAPTER_PAGES = 500
ID_PATTERN = re.compile(r"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
FILE_PATTERN = re.compile(r"^[A-Za-z0-9][A-Za-z0-9._-]{0,180}$")
ALLOWED_IMAGE_TYPES = {"image/jpeg", "image/png", "image/webp", "image/gif", "image/avif"}
PROVIDER_HEADERS = {"User-Agent": "FableAndroid/1.0 (manga API)"}


class MangaService:
    def __init__(self, client: httpx.AsyncClient, image_cache: DiskImageCache) -> None:
        self.client = client
        self.image_cache = image_cache
        self.json_cache = TtlCache()
        self.upstream_limit = asyncio.Semaphore(5)
        self.manifest_locks = [asyncio.Lock() for _ in range(16)]
        self.image_locks = [asyncio.Lock() for _ in range(32)]

    async def _read_limited(self, response: httpx.Response, limit: int) -> bytes:
        content_length = response.headers.get("content-length")
        if content_length and content_length.isdigit() and int(content_length) > limit:
            raise HTTPException(status_code=502, detail="Manga provider response exceeds the size limit")
        content = bytearray()
        async for chunk in response.aiter_bytes():
            if len(content) + len(chunk) > limit:
                raise HTTPException(status_code=502, detail="Manga provider response exceeds the size limit")
            content.extend(chunk)
        return bytes(content)

    async def _get_json(self, path: str, params: list[tuple[str, str]] | None = None) -> dict:
        async with self.upstream_limit:
            try:
                async with self.client.stream(
                    "GET",
                    f"{MANGADEX_API}{path}",
                    params=params,
                    headers=PROVIDER_HEADERS,
                ) as response:
                    if response.status_code == 404:
                        return {}
                    if response.status_code != 200:
                        raise HTTPException(status_code=502, detail="Manga provider request failed")
                    payload = await self._read_limited(response, MAX_JSON_BYTES)
            except HTTPException:
                raise
            except httpx.TimeoutException as error:
                raise HTTPException(status_code=504, detail="Manga provider timed out") from error
            except httpx.HTTPError as error:
                raise HTTPException(status_code=502, detail="Manga provider is unavailable") from error
        try:
            decoded = json.loads(payload)
        except (UnicodeDecodeError, json.JSONDecodeError) as error:
            raise HTTPException(status_code=502, detail="Manga provider returned invalid JSON") from error
        return decoded if isinstance(decoded, dict) else {}

    async def _cached_json(self, key: str, ttl: int, fetch):
        cached = await self.json_cache.get(key, ttl)
        if cached is not None:
            return cached
        value = await fetch()
        await self.json_cache.set(key, value)
        return value

    @staticmethod
    def _require_id(value: str) -> None:
        if not ID_PATTERN.fullmatch(value):
            raise HTTPException(status_code=422, detail="Invalid MangaDex identifier")

    @staticmethod
    def _story(item: dict) -> dict | None:
        manga_id = item.get("id")
        attributes = item.get("attributes") or {}
        if not manga_id or not attributes:
            return None
        try:
            manga_uuid = UUID(str(manga_id))
        except ValueError:
            return None

        titles = attributes.get("title") or {}
        title = titles.get("en")
        if not title:
            title = next((alt.get("en") for alt in attributes.get("altTitles", []) if alt.get("en")), None)
        if not title:
            title = next(iter(titles.values()), "Untitled Manga")
        synopsis = (attributes.get("description") or {}).get("en", "")
        author = "Manga Artist"
        cover_name = None
        for relation in item.get("relationships", []):
            relation_attributes = relation.get("attributes") or {}
            if relation.get("type") == "author" and relation_attributes.get("name", "").strip():
                author = relation_attributes["name"].strip()
            elif relation.get("type") == "cover_art":
                cover_name = relation_attributes.get("fileName")

        cover_url = None
        if cover_name and FILE_PATTERN.fullmatch(cover_name):
            cover_url = f"images/covers/{manga_uuid}/{cover_name}?size=256"
        now = datetime.now(timezone.utc).isoformat()
        return {
            "id": str(uuid5(MANGA_NAMESPACE, f"mangadex:{manga_uuid}")),
            "title": str(title)[:120],
            "author": author[:80],
            "genre": "Manga",
            "chapter": "Chapter 1",
            "synopsis": str(synopsis)[:4000],
            "content": "",
            "readTimeMinutes": 15,
            "createdAtUtc": now,
            "updatedAtUtc": now,
            "coverImageUrl": cover_url,
            "badgeText": "MANGA",
            "totalChapters": 0,
            "contentFormat": "MANGA",
            "sourceProvider": "MANGADEX",
            "providerId": str(manga_uuid),
        }

    async def search(self, query: str | None, limit: int, offset: int) -> list[dict]:
        normalized_query = query.strip() if query and query.strip() else ""
        cache_key = f"search:{normalized_query}:{limit}:{offset}"
        cached = await self.json_cache.get(cache_key, 300)
        if cached is not None:
            return cached
        params = [
            ("limit", str(min(max(limit, 1), 50))),
            ("offset", str(offset)),
            ("includes[]", "cover_art"),
            ("includes[]", "author"),
            ("availableTranslatedLanguage[]", "en"),
            ("contentRating[]", "safe"),
            ("contentRating[]", "suggestive"),
        ]
        if normalized_query:
            params.extend([("title", normalized_query), ("order[relevance]", "desc")])
        else:
            params.append(("order[followedCount]", "desc"))
        payload = await self._get_json("/manga", params)
        results = [
            story
            for item in payload.get("data", [])
            if (story := self._story(item)) is not None
        ][:limit]
        await self.json_cache.set(cache_key, results)
        return results

    async def get_story(self, manga_id: str) -> dict:
        self._require_id(manga_id)
        async def fetch():
            payload = await self._get_json(
                f"/manga/{manga_id}",
                [("includes[]", "cover_art"), ("includes[]", "author")],
            )
            story = self._story(payload.get("data") or {})
            if story is None:
                raise HTTPException(status_code=404, detail="Manga title not found")
            return story
        return await self._cached_json(f"story:{manga_id}", 600, fetch)

    async def _chapter_manifest(self, chapter_id: str) -> dict:
        self._require_id(chapter_id)

        async def fetch():
            payload = await self._get_json(f"/at-home/server/{chapter_id}")
            if not payload:
                raise HTTPException(status_code=404, detail="Manga chapter has no available image manifest")
            manifest = payload.get("chapter") or {}
            base_url = payload.get("baseUrl")
            digest = manifest.get("hash")
            base = urlsplit(base_url or "")
            try:
                port = base.port
            except ValueError as error:
                raise HTTPException(status_code=502, detail="Manga image manifest is invalid") from error
            if (
                base.scheme != "https"
                or not base.hostname
                or not base.hostname.endswith(MANGADEX_PANEL_SUFFIX)
                or port not in (None, 443)
                or base.username is not None
                or base.password is not None
                or base.query
                or base.fragment
                or not digest
            ):
                raise HTTPException(status_code=502, detail="Manga image manifest is invalid")
            files = manifest.get("dataSaver") or manifest.get("data") or []
            quality = "data-saver" if manifest.get("dataSaver") else "data"
            valid_files = [name for name in files if isinstance(name, str) and FILE_PATTERN.fullmatch(name)]
            if not valid_files:
                return {"base_url": base_url.rstrip("/"), "hash": digest, "quality": quality, "files": []}
            return {"base_url": base_url.rstrip("/"), "hash": digest, "quality": quality, "files": valid_files}
        key = f"manifest:{chapter_id}"
        cached = await self.json_cache.get(key, 1800)
        if cached is not None:
            return cached
        lock = self.manifest_locks[hash(chapter_id) % len(self.manifest_locks)]
        async with lock:
            return await self._cached_json(key, 1800, fetch)

    async def chapters(self, manga_id: str, limit: int) -> list[dict]:
        self._require_id(manga_id)
        cache_key = f"chapters:{manga_id}:{limit}"
        cached = await self.json_cache.get(cache_key, 300)
        if cached is not None:
            return cached
        payload = await self._get_json(
            f"/manga/{manga_id}/feed",
            [
                ("translatedLanguage[]", "en"),
                ("order[chapter]", "asc"),
                ("includeExternalUrl", "0"),
                ("limit", str(min(max(limit * 2, 50), 100))),
            ],
        )
        best_by_number: dict[int, tuple[dict, str, int]] = {}
        for index, chapter in enumerate(payload.get("data", [])):
            chapter_id = chapter.get("id")
            attributes = chapter.get("attributes") or {}
            try:
                page_count = int(attributes.get("pages") or 0)
            except (ValueError, TypeError, OverflowError):
                continue
            if not chapter_id or attributes.get("externalUrl") or page_count <= 0:
                continue
            try:
                self._require_id(chapter_id)
            except HTTPException:
                continue
            raw_number = str(attributes.get("chapter") or index + 1)
            try:
                chapter_number = max(0, int(float(raw_number)))
            except (ValueError, TypeError, OverflowError):
                chapter_number = index + 1
            current = best_by_number.get(chapter_number)
            if current is None or page_count > int((current[0].get("attributes") or {}).get("pages") or 0):
                best_by_number[chapter_number] = (chapter, raw_number, chapter_number)

        candidates = [best_by_number[number] for number in sorted(best_by_number)[:limit]]
        chapters = []
        for chapter, raw_number, chapter_number in candidates:
            attributes = chapter.get("attributes") or {}
            try:
                page_count = min(int(attributes.get("pages") or 0), MAX_CHAPTER_PAGES)
            except (ValueError, TypeError, OverflowError):
                continue
            if page_count <= 0:
                continue
            chapter_id = chapter["id"]
            page_urls = [f"images/pages/{chapter_id}/{index}" for index in range(1, page_count + 1)]
            chapters.append({
                "id": str(uuid5(MANGA_NAMESPACE, f"mangadex_chapter:{chapter_id}")),
                "storyId": str(uuid5(MANGA_NAMESPACE, f"mangadex:{manga_id}")),
                "chapterNumber": chapter_number,
                "title": (attributes.get("title") or "").strip() or f"Chapter {raw_number}",
                "content": "",
                "wordCount": 0,
                "createdAtUtc": datetime.now(timezone.utc).isoformat(),
                "pageUrls": page_urls,
            })
        await self.json_cache.set(cache_key, chapters)
        return chapters

    async def cover_image(self, manga_id: str, filename: str, size: int) -> tuple[bytes, str]:
        self._require_id(manga_id)
        if not FILE_PATTERN.fullmatch(filename):
            raise HTTPException(status_code=404, detail="Manga image not found")
        if size not in {256, 512}:
            raise HTTPException(status_code=422, detail="Unsupported cover size")
        url = f"https://{MANGADEX_COVER_HOST}/covers/{manga_id}/{filename}.{size}.jpg"
        return await self._image(url)

    async def page_image(self, chapter_id: str, page_number: int) -> tuple[bytes, str]:
        self._require_id(chapter_id)
        if page_number < 1 or page_number > MAX_CHAPTER_PAGES:
            raise HTTPException(status_code=404, detail="Manga page not found")
        manifest = await self._chapter_manifest(chapter_id)
        if page_number > len(manifest["files"]):
            raise HTTPException(status_code=404, detail="Manga page not found")
        filename = manifest["files"][page_number - 1]
        url = f"{manifest['base_url']}/{manifest['quality']}/{manifest['hash']}/{filename}"
        host = urlsplit(url).hostname or ""
        if not host.endswith(MANGADEX_PANEL_SUFFIX):
            raise HTTPException(status_code=502, detail="Manga image host is invalid")
        return await self._image(url)

    async def _image(self, url: str) -> tuple[bytes, str]:
        cached = await self.image_cache.get(url)
        if cached is not None:
            return cached
        lock = self.image_locks[hash(url) % len(self.image_locks)]
        async with lock:
            cached = await self.image_cache.get(url)
            if cached is not None:
                return cached
            try:
                async with self.upstream_limit:
                    async with self.client.stream("GET", url, headers=PROVIDER_HEADERS) as response:
                        if response.status_code != 200:
                            raise HTTPException(status_code=502, detail="Manga image provider request failed")
                        media_type = response.headers.get("content-type", "").split(";", 1)[0].lower()
                        if media_type not in ALLOWED_IMAGE_TYPES:
                            raise HTTPException(status_code=502, detail="Manga provider returned an unsupported image")
                        body = await self._read_limited(response, MAX_IMAGE_BYTES)
            except HTTPException:
                raise
            except httpx.TimeoutException as error:
                raise HTTPException(status_code=504, detail="Manga image provider timed out") from error
            except httpx.HTTPError as error:
                raise HTTPException(status_code=502, detail="Manga image provider is unavailable") from error
            await self.image_cache.set(url, body, media_type)
            return body, media_type
