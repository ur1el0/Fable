# Fable Android Product Requirements

## Product Intent

Fable Android is the native Kotlin and Jetpack Compose client for the shared Fable product (companion to `Fable-IOS`). It supports format-aware story discovery, prose and comic reading, in-book search, passage highlights and shareable quote cards, a personal shelf with explicit offline chapter downloads, accounts, and community publishing while retaining locally cached stories, chapters, drafts, and reading state in Room (`FableDatabase` schema v6).

Android and iOS use the shared Fable FastAPI service (`https://fable-api-cbo8.onrender.com/api/v1/`) for catalogs, provider integrations, accounts, publishing, and cross-device shelf and reading-stat synchronization. Android must not duplicate provider HTML scraping or backend business logic.

## Audience

- Readers who want to discover and read curated prose books (`FABLE`, `GUTENBERG`, `STANDARD_EBOOKS`) and manga/webtoons (`MANGADEX`).
- Readers who need downloaded or previously cached stories, chapters, and highlights to remain available offline.
- Writers who draft manuscripts locally, publish stories through Fable, and edit or delete their published works.

## Product Requirements

### Discovery and story details
- Present the Fable catalog and provider-backed discovery (`FABLE`, `GUTENBERG`, `STANDARD_EBOOKS`, `MANGADEX`) through the shared API with parallel, fault-isolated refresh.
- Separate **Books** (`ContentFormat.PROSE`) and **Manga & Comics** (`ContentFormat.COMIC`) in Explore discovery and search.
- Keep catalog metadata in Room so previously fetched stories remain available when refreshes fail.
- Preserve already cached chapter bodies and comic page lists (`coalesceChapters`) when a summary response or provider failure contains less data.
- Support explicit **Download for offline** and **Remove offline copy** actions on Story Detail (`OfflineCacheCard`) and surface `"Offline"` badges across Library, Explore, and Shelf.

### Reader
- **Prose Reader**: Support Continuous Scroll and Paginated Book layouts, Paper/Sepia/Night themes, Serif/Sans/Mono fonts, adjustable font size (`15sp`–`26sp`), line height (`1.25x`–`1.9x`), per-session screen brightness control (`Auto` or manual slider), and word-offset progress mapping across layout/typography changes.
- **Annotations & Search**: Support cross-chapter keyword search (`InBookSearchEngine`), passage word-range highlights (`TERRACOTTA`, `AMBER`, `SAGE`) with notes persisted in Room (`story_highlights`), Markdown/text highlight export, and 1080px PNG Quote Card sharing (`QuoteCardExporter`).
- **Comic Reader**: Support vertical Webtoon and horizontal Manga layouts, two-finger pinch-to-zoom (`1x`–`4x`), double-tap zoom (`1x <-> 2.5x`), bounded panning, lazy page placeholders, per-page error retry, and chapter transition banners.

### Library and shelf
- Show locally cached stories, featured stories, *"Continue Reading"*, and shelf filters (`Saved`, `In Progress`, `Finished`, `Offline`).
- Keep shelf state (`shelf_states`) and reading statistics (`reading_sessions`, `reading_stats`) scoped by account, with a `__guest__` scope when signed out.
- Reconcile local shelf changes and pending reading sessions with the shared backend when signed in without discarding local data on a failed request.

### Accounts and publishing
- Support registration, sign-in, profile updates, password reset, sign-out, automatic `401` session expiry reset, and permanent account deletion (`DELETE /api/v1/auth/me`) through the shared API.
- Auto-save local writer drafts in Room (`writer_drafts`).
- Allow authenticated users to publish stories (`POST /api/v1/stories`), edit their own published stories (`PATCH /api/v1/stories/{id}` after hydrating full chapter content), and delete their own stories (`DELETE /api/v1/stories/{id}`).
- Provide a debug-only runtime server URL switcher (`DebugServerConfigStore`) in **Account → Developer server** while locking release builds to an explicit compile-time `https://` `FABLE_API_BASE_URL`.
