from contextlib import asynccontextmanager
from pathlib import Path

import httpx
from fastapi import FastAPI, Query, Request, Response

from app.cache import DiskImageCache
from app.service import MangaService


def create_app(
    *,
    upstream_transport: httpx.AsyncBaseTransport | None = None,
    image_cache_dir: str | Path | None = None,
) -> FastAPI:
    @asynccontextmanager
    async def lifespan(application: FastAPI):
        client = httpx.AsyncClient(
            timeout=httpx.Timeout(connect=5.0, read=15.0, write=5.0, pool=5.0),
            limits=httpx.Limits(max_connections=10, max_keepalive_connections=5),
            follow_redirects=False,
            transport=upstream_transport,
        )
        application.state.manga = MangaService(client, DiskImageCache(image_cache_dir))
        yield
        await client.aclose()

    application = FastAPI(title="Fable Android Manga API", version="1.0.0", lifespan=lifespan)

    @application.get("/api/v1/health")
    async def health():
        return {"status": "healthy", "service": "manga", "provider": "MangaDex"}

    @application.get("/api/v1/public/manga")
    async def search_manga(
        request: Request,
        search: str | None = Query(default=None, max_length=120),
        limit: int = Query(default=20, ge=1, le=50),
        offset: int = Query(default=0, ge=0, le=100_000),
    ):
        return await request.app.state.manga.search(search, limit, offset)

    @application.get("/api/v1/public/manga/{manga_id}")
    async def get_manga(manga_id: str, request: Request):
        return await request.app.state.manga.get_story(manga_id)

    @application.get("/api/v1/public/manga/{manga_id}/chapters")
    async def get_manga_chapters(
        manga_id: str,
        request: Request,
        limit: int = Query(default=20, ge=1, le=100),
    ):
        return await request.app.state.manga.chapters(manga_id, limit)

    @application.get("/api/v1/images/covers/{manga_id}/{filename}")
    async def get_cover(manga_id: str, filename: str, request: Request, size: int = Query(default=256)):
        body, media_type = await request.app.state.manga.cover_image(manga_id, filename, size)
        return Response(body, media_type=media_type, headers={"Cache-Control": "public, max-age=86400", "X-Content-Type-Options": "nosniff"})

    @application.get("/api/v1/images/pages/{chapter_id}/{page_number}")
    async def get_page(chapter_id: str, page_number: int, request: Request):
        body, media_type = await request.app.state.manga.page_image(chapter_id, page_number)
        return Response(body, media_type=media_type, headers={"Cache-Control": "public, max-age=86400", "X-Content-Type-Options": "nosniff"})

    return application


app = create_app()
