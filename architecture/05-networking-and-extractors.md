# 05 Networking & Federated API Pipeline

## 1. Product API Architecture (ADR 009 Authority)

Fable Android communicates with the shared FastAPI backend using a contract-first architecture implemented via `FableApiService`, backed by an optimized `OkHttpClient` and `kotlinx.serialization`.

The Android client does not scrape external websites directly. All catalog search, public-domain prose federation, chapter parsing, user authentication, publishing, and shelf synchronization are coordinated through the shared REST API. Room remains the single source of truth locally for cached metadata, hydrated chapters, and reading state.

```
┌────────────────────────────────────────────────────────┐
│                   Fable Android UI                     │
│          (ExploreScreen / LibraryScreen / Reader)      │
└───────────────────────────┬────────────────────────────┘
                            │ Room Flow / Repository Call
                            ▼
┌────────────────────────────────────────────────────────┐
│                 RoomStoryRepository                    │
│   - Queries Room first (Cache-First Read)              │
│   - Dispatches remote fetches to FableApiService       │
│   - Merges responses into Room atomically              │
└───────────────────────────┬────────────────────────────┘
                            │ Asynchronous HTTP Requests
                            ▼
┌────────────────────────────────────────────────────────┐
│                   FableApiService                      │
│        (OkHttpClient + kotlinx.serialization)          │
└───────────────────────────┬────────────────────────────┘
                            │ REST / JSON (camelCase aliases)
                            ▼
┌────────────────────────────────────────────────────────┐
│                Shared FastAPI Backend                  │
│  ├─ /api/v1/stories (Curated Catalog & Publishing)     │
│  ├─ /api/v1/public/gutenberg (Project Gutenberg)       │
│  ├─ /api/v1/public/standard-ebooks (Standard Ebooks)   │
│  ├─ /api/v1/shelf/sync (Multi-Tenant Shelf Sync)       │
│  └─ /api/v1/auth/* (Accounts & Reading Stats)          │
└────────────────────────────────────────────────────────┘
```

---

## 2. Shared API Endpoint Groups

### 2.1 Curated Literature & Catalog Discovery
- `GET /api/v1/stories?genre={genre}&search={query}&contentFormat={format}`: Fetches curated catalog stories.
- `GET /api/v1/stories/{storyId}`: Story metadata and chapter list.
- `GET /api/v1/stories/{storyId}/chapters/{chapterId}`: Detailed chapter content.
- `GET /api/v1/genres`: Discovered category list.
- `GET /api/v1/authors/top`: Featured catalog and community authors.
- `GET /api/v1/updates`: Real-time catalog change timestamps.

### 2.2 Federated Public-Domain Literature Providers
- **Project Gutenberg:**
  - `GET /api/v1/public/gutenberg?topic={topic}&search={search}`: Catalog search and topic discovery.
  - `GET /api/v1/public/gutenberg/{id}/chapters`: On-demand plain-text source parsing and chapter hydration.
- **Standard Ebooks:**
  - `GET /api/v1/public/standard-ebooks?topic={topic}&search={search}`: Typography-first public domain catalog.
  - `GET /api/v1/public/standard-ebooks/story?book_id={slug}`: Resolves metadata for a single work by slug.
  - `GET /api/v1/public/standard-ebooks/chapters?book_id={slug}`: Semantic XHTML chapter extraction on demand.

### 2.3 Provider Failure Isolation
The repository dispatches requests to `/public/gutenberg` and `/public/standard-ebooks` independently. Upstream latency or provider outages (HTTP 502/504) from one provider do not block or discard results from the other. The UI receives partial results accompanied by a recoverable error state.

### 2.4 Account, Shelf Synchronization & Publishing
- `POST /api/v1/auth/register` & `POST /api/v1/auth/login`: User session management.
- `GET /api/v1/auth/me`: Authenticated profile and stats.
- `POST /api/v1/shelf/sync`: Account-scoped shelf and reading checkpoint synchronization.
- `GET /api/v1/auth/me/reading-stats`: Aggregate reading analytics.
- `POST /api/v1/stories`: Publish community stories.

---

## 3. OkHttp Network Client Configuration

```kotlin
val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .writeTimeout(15, TimeUnit.SECONDS)
    .addInterceptor { chain ->
        val request = chain.request().newBuilder()
            .header("Accept", "application/json")
            .header("User-Agent", "Fable-Android/${BuildConfig.VERSION_NAME}")
            .build()
        chain.proceed(request)
    }
    .build()
```

---

## 4. Deferred Direct-Scraping Specification

The legacy client-side extractor interface (`Source`) and Jsoup HTML web scraper pipeline are **DEFERRED** per ADR 009. All content extraction is owned by the shared backend to ensure cross-platform parity with iOS, reduce mobile battery and memory usage, and centralize rate-limiting compliance. Direct scrapers must not be reintroduced without an updated ADR.

