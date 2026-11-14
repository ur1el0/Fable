# Fable Android Manga API

This is a standalone FastAPI service for the Android app's MangaDex catalog, chapter manifests, and image proxy. It does not replace or modify the shared Fable-IOS backend. Account, shelf, writing, and prose requests continue to use the shared API.

## Local Setup

From the repository root:

```bash
cd manga-backend
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --host 127.0.0.1 --port 18002
```

The health endpoint is `http://127.0.0.1:18002/api/v1/health`. API tests run with:

```bash
python -m pytest -q
```

The image cache defaults to `/tmp/fable-manga-image-cache`; set `MANGA_IMAGE_CACHE_DIR` to use a different persistent directory. The cache expires entries after one day and is capped at 256 MiB.

## Android Connection

The debug build defaults to `http://10.0.2.2:18002/api/v1/`, which reaches the host from the Android emulator. The existing shared Fable API remains independently configured on port 8001.

For a USB-connected physical phone, the Manga service must be reachable from the phone. Over USB, run `adb reverse tcp:18002 tcp:18002`, then install with `-PfableMangaApiBaseUrl=http://127.0.0.1:18002/api/v1/`. Over Wi-Fi, bind Uvicorn to `0.0.0.0`, allow port 18002 only on a trusted LAN, and use `-PfableMangaApiBaseUrl=http://<computer-lan-ip>:18002/api/v1/`. Configure the shared API separately with `-PfableApiBaseUrl=http://<api-host>:8001/api/v1/` only if that service is also running locally. This development service has no authentication; do not expose it to the public internet.

Release builds require HTTPS URLs for both services: `-PfableReleaseApiBaseUrl=https://<host>/api/v1/` and `-PfableReleaseMangaApiBaseUrl=https://<manga-host>/api/v1/`. Cloud deployment and production hardening are intentionally separate follow-up work.

## API Boundaries

- `GET /api/v1/health`
- `GET /api/v1/public/manga?search=&limit=&offset=`
- `GET /api/v1/public/manga/{manga_id}`
- `GET /api/v1/public/manga/{manga_id}/chapters?limit=`
- `GET /api/v1/images/covers/{manga_id}/{filename}?size=256|512`
- `GET /api/v1/images/pages/{chapter_id}/{page_number}`

Catalog responses do not synchronously probe every manga's chapter feed, and chapter listing does not fetch page manifests. Search and chapter metadata use bounded in-memory caches; image bytes use a bounded disk cache. Page routes resolve a requested page number through a validated MangaDex at-home manifest, do not follow redirects, and enforce response-size and content-type limits.
