# 05 Networking & Shared Fable API Integration

## 1. Shared Backend Architecture (No Client-Side HTML Scraping)

Per **ADR 009** and **ADR 010**, Fable Android does **not** scrape third-party websites or parse HTML via Jsoup on the device. All catalog discovery, public-domain book ingestion, manga metadata/pages, authentication, story publishing, and cross-device synchronization flow through the shared FastAPI backend (`https://fable-api-cbo8.onrender.com/api/v1/`).

```
┌──────────────────────────────────────────────────────────────────────┐
│                         Fable Android Client                         │
│  RoomStoryRepository · RemoteAccountRepository · ReadingStatsRepo    │
└──────────────────┬───────────────────────────────────┬───────────────┘
                   │                                   │
                   ▼                                   ▼
      ┌─────────────────────────┐         ┌─────────────────────────┐
      │     FableApiService     │         │     MangaApiService     │
      │  /api/v1/stories/*      │         │  /api/v1/manga/*        │
      │  /api/v1/books/*        │         │  Strict URL Validation  │
      │  /api/v1/auth/*         │         └────────────┬────────────┘
      │  /api/v1/me/*           │                      │
      └────────────┬────────────┘                      │
                   └─────────────────┬─────────────────┘
                                     ▼
                    ┌─────────────────────────────────┐
                    │         FableHttpClient         │
                    │  Shared OkHttpClient Singleton  │
                    │  connect=10s, read/write=20s,   │
                    │  call=30s · executeCancellable  │
                    └────────────────┬────────────────┘
                                     ▼
                    ┌─────────────────────────────────┐
                    │   Shared Fable FastAPI Backend  │
                    │  Fable Originals · Gutenberg    │
                    │  Standard Ebooks · MangaDex     │
                    └─────────────────────────────────┘
```

---

## 2. Shared Cancellable OkHttp Transport (`FableHttpClient`)

`data/remote/FableHttpClient.kt` provides a shared `OkHttpClient` instance with bounded timeouts:
- `connectTimeout`: 10 seconds
- `readTimeout`: 20 seconds
- `writeTimeout`: 20 seconds
- `callTimeout`: 30 seconds

All requests run on `Dispatchers.IO` via `OkHttpClient.executeCancellable(request)`, which uses `suspendCancellableCoroutine` to invoke `call.cancel()` immediately whenever the calling Kotlin coroutine (such as a superseded search in `ExploreViewModel`) is cancelled.

---

## 3. Integrated API Endpoints

### `FableApiService`
- **Fable Originals & Story Management**:
  - `GET /stories` — Fetch catalog stories.
  - `GET /stories/{id}` — Fetch full story detail and chapters.
  - `POST /stories` — Publish a new story (authenticated).
  - `PATCH /stories/{id}` — Update an owned story (`UpdateStoryRequestDto`, authenticated owner).
  - `DELETE /stories/{id}` — Delete an owned story (authenticated owner).
- **Project Gutenberg**:
  - `GET /books/gutenberg/discover`, `GET /books/gutenberg/search?q=...`, `GET /books/gutenberg/{externalId}/chapters`
- **Standard Ebooks**:
  - `GET /books/standard-ebooks/discover`, `GET /books/standard-ebooks/search?q=...`, `GET /books/standard-ebooks/{externalId}`, `GET /books/standard-ebooks/{externalId}/chapters`
- **Authentication & Account Lifecycle**:
  - `POST /auth/register`, `POST /auth/login`, `POST /auth/logout`, `POST /auth/password-reset`
  - `GET /auth/me`, `PATCH /auth/me`, `DELETE /auth/me`
- **Shelf & Reading Stats Synchronization**:
  - `GET /me/shelf`, `PUT /me/shelf/{storyId}`, `DELETE /me/shelf/{storyId}`
  - `POST /me/reading-sessions`, `GET /me/stats`

### `MangaApiService`
- `GET /manga/discover`, `GET /manga/search?q=...`, `GET /manga/{id}`, `GET /manga/{id}/chapters`, `GET /manga/{id}/chapters/{chapterId}/pages`
- Validates cover and page image URLs (`sanitizeHttpUrl`) so only valid `http`/`https` URLs with a non-blank host reach Coil or the reader UI.

---

## 4. Runtime Debug Server URL Switcher & Release Enforcement

- **`ServerUrlValidator`**: Normalizes user-entered URLs to end with `/api/v1/`, validates scheme/host, blocks credentials in URLs, and enforces `https://` in release mode (`requireHttps = true`).
- **`DebugServerConfigStore`**: Allows developers in `BuildConfig.DEBUG` builds to switch between the hosted Render API, Android Emulator localhost (`http://10.0.2.2:8000/api/v1/`), or a LAN server IP at runtime from **Account → Developer server**.
- **Release Hardening**: In release builds (`BuildConfig.DEBUG == false`), runtime URL switching is disabled and `:app:validateReleaseApiBaseUrl` verifies at build time that `FABLE_API_BASE_URL` uses `https://` and does not point to a local/private address.
