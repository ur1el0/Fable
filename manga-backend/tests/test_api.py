from pathlib import Path

from fastapi.testclient import TestClient
import httpx

from app.main import create_app

MANGA_ID = "11111111-1111-4111-8111-111111111111"
CHAPTER_A = "22222222-2222-4222-8222-222222222222"
CHAPTER_B = "33333333-3333-4333-8333-333333333333"
IMAGE_BYTES = b"\xff\xd8\xfffable-test-image"


def manga_item(manga_id: str = MANGA_ID) -> dict:
    return {
        "id": manga_id,
        "attributes": {
            "title": {"en": "Example Manga"},
            "description": {"en": "A test description"},
        },
        "relationships": [
            {"type": "author", "attributes": {"name": "A. Writer"}},
            {"type": "cover_art", "attributes": {"fileName": "cover.jpg"}},
        ],
    }


def feed_item(chapter_id: str, number: str, pages: int, external_url: str | None = None) -> dict:
    return {
        "id": chapter_id,
        "attributes": {"chapter": number, "pages": pages, "title": "", "externalUrl": external_url},
    }


def make_client(tmp_path: Path) -> tuple[TestClient, list[str]]:
    requested: list[str] = []

    def upstream(request: httpx.Request) -> httpx.Response:
        requested.append(str(request.url))
        path = request.url.path
        if path == "/manga":
            return httpx.Response(200, json={"data": [manga_item()]})
        if path == f"/manga/{MANGA_ID}":
            return httpx.Response(200, json={"data": manga_item()})
        if path == f"/manga/{MANGA_ID}/feed":
            return httpx.Response(
                200,
                json={"data": [
                    feed_item(CHAPTER_A, "1", 8),
                    feed_item(CHAPTER_B, "1", 14),
                    feed_item("44444444-4444-4444-8444-444444444444", "2", 20, "https://example.org/read"),
                    feed_item("55555555-5555-4555-8555-555555555555", "2", 20),
                    feed_item("66666666-6666-4666-8666-666666666666", "Special", 3),
                ]},
            )
        if path.startswith("/at-home/server/"):
            chapter_id = path.rsplit("/", 1)[-1]
            return httpx.Response(
                200,
                json={"baseUrl": "https://cdn.mangadex.network", "chapter": {
                    "hash": f"hash-{chapter_id}", "dataSaver": ["001.jpg", "002.jpg"], "data": ["001.jpg"]
                }},
            )
        if request.url.host == "cdn.mangadex.network" or request.url.host == "uploads.mangadex.org":
            return httpx.Response(200, content=IMAGE_BYTES, headers={"content-type": "image/jpeg"})
        return httpx.Response(404)

    app = create_app(upstream_transport=httpx.MockTransport(upstream), image_cache_dir=tmp_path)
    return TestClient(app), requested


def test_search_returns_fast_catalog_metadata_and_relative_cover_proxy(tmp_path: Path) -> None:
    with make_client(tmp_path)[0] as client:
        response = client.get("/api/v1/public/manga", params={"search": "Example", "limit": 10})

    assert response.status_code == 200
    story = response.json()[0]
    assert story["title"] == "Example Manga"
    assert story["sourceProvider"] == "MANGADEX"
    assert story["contentFormat"] == "MANGA"
    assert story["coverImageUrl"] == f"images/covers/{MANGA_ID}/cover.jpg?size=256"


def test_search_does_not_probe_chapters_for_each_catalog_result(tmp_path: Path) -> None:
    client, requested = make_client(tmp_path)
    with client:
        response = client.get("/api/v1/public/manga", params={"search": "Example"})

    assert response.status_code == 200
    assert len(response.json()) == 1
    assert all("/feed" not in url and "/at-home/" not in url for url in requested)


def test_chapters_filter_external_releases_and_keep_largest_duplicate(tmp_path: Path) -> None:
    client, requested = make_client(tmp_path)
    with client:
        response = client.get(f"/api/v1/public/manga/{MANGA_ID}/chapters")

    assert response.status_code == 200
    chapters = response.json()
    assert [chapter["chapterNumber"] for chapter in chapters] == [1, 2, 5]
    assert chapters[0]["title"] == "Chapter 1"
    assert chapters[0]["pageUrls"] == [f"images/pages/{CHAPTER_B}/{page}" for page in range(1, 15)]
    assert not any("/at-home/server/" in url for url in requested)
    assert f"images/pages/{CHAPTER_A}/1" not in chapters[0]["pageUrls"]
    assert "https://api.mangadex.org/at-home/server/44444444-4444-4444-8444-444444444444" not in requested


def test_page_proxy_validates_manifest_and_caches_image_bytes(tmp_path: Path) -> None:
    client, requested = make_client(tmp_path)
    with client:
        manifest_url = f"/api/v1/public/manga/{MANGA_ID}/chapters"
        assert client.get(manifest_url).status_code == 200
        image_path = f"/api/v1/images/pages/{CHAPTER_B}/1"
        first = client.get(image_path)
        second = client.get(image_path)

    assert first.status_code == second.status_code == 200
    assert first.content == second.content == IMAGE_BYTES
    cdn_fetches = [url for url in requested if "cdn.mangadex.network" in url]
    assert len(cdn_fetches) == 1


def test_page_proxy_rejects_files_not_in_mangadex_manifest(tmp_path: Path) -> None:
    client, requested = make_client(tmp_path)
    with client:
        client.get(f"/api/v1/public/manga/{MANGA_ID}/chapters")
        response = client.get(f"/api/v1/images/pages/{CHAPTER_B}/3")

    assert response.status_code == 404
    assert not any("cdn.mangadex.network" in url for url in requested)


def test_cover_proxy_caches_mangadex_cover_bytes(tmp_path: Path) -> None:
    client, requested = make_client(tmp_path)
    with client:
        assert client.get(f"/api/v1/public/manga/{MANGA_ID}").status_code == 200
        image_path = f"/api/v1/images/covers/{MANGA_ID}/cover.jpg?size=256"
        first = client.get(image_path)
        second = client.get(image_path)

    assert first.status_code == second.status_code == 200
    assert first.content == second.content == IMAGE_BYTES
    cover_fetches = [url for url in requested if "uploads.mangadex.org/covers/" in url]
    assert len(cover_fetches) == 1


def test_page_proxy_rejects_untrusted_manifest_host(tmp_path: Path) -> None:
    requested: list[str] = []

    def upstream(request: httpx.Request) -> httpx.Response:
        requested.append(str(request.url))
        if request.url.path.endswith("/feed"):
            return httpx.Response(200, json={"data": [feed_item(CHAPTER_A, "1", 1)]})
        if request.url.path == f"/at-home/server/{CHAPTER_A}":
            return httpx.Response(200, json={"baseUrl": "https://127.0.0.1", "chapter": {
                "hash": "hash", "data": ["secret.jpg"]
            }})
        return httpx.Response(200, content=IMAGE_BYTES, headers={"content-type": "image/jpeg"})

    client = TestClient(create_app(
        upstream_transport=httpx.MockTransport(upstream), image_cache_dir=tmp_path
    ))
    with client:
        client.get(f"/api/v1/public/manga/{MANGA_ID}/chapters")
        response = client.get(f"/api/v1/images/pages/{CHAPTER_A}/1")

    assert response.status_code == 502
    assert not any("127.0.0.1" in url for url in requested)


def test_page_proxy_returns_not_found_when_upstream_manifest_is_gone(tmp_path: Path) -> None:
    def upstream(request: httpx.Request) -> httpx.Response:
        if request.url.path == f"/at-home/server/{CHAPTER_A}":
            return httpx.Response(404, json={"detail": "not found"})
        return httpx.Response(200, json={})

    client = TestClient(create_app(
        upstream_transport=httpx.MockTransport(upstream), image_cache_dir=tmp_path
    ))
    with client:
        response = client.get(f"/api/v1/images/pages/{CHAPTER_A}/1")

    assert response.status_code == 404


def test_invalid_provider_ids_are_rejected_before_upstream_call(tmp_path: Path) -> None:
    client, requested = make_client(tmp_path)
    with client:
        response = client.get("/api/v1/public/manga/not-an-id/chapters")

    assert response.status_code == 422
    assert requested == []


def test_health_identifies_manga_service(tmp_path: Path) -> None:
    with make_client(tmp_path)[0] as client:
        response = client.get("/api/v1/health")
    assert response.json() == {"status": "healthy", "service": "manga", "provider": "MangaDex"}
