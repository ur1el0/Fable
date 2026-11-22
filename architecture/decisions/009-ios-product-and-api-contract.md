# ADR 009: Align Fable Android with the Shared Fable-IOS Product and API Contract

## Status

Accepted — supersedes the earlier scraper, Jsoup, and WorkManager-focused ADRs where they conflict.

## Context

Fable has a companion iOS product and shared backend architecture in `Fable-IOS`. Earlier Android planning documents in this repository described a standalone web-novel scraper with Jsoup extractors, WorkManager batch downloads, and file-per-chapter storage.

The actual Android application in `app/src/main/java/com/example/fable` implements the shared Fable product model:

- Library, Explore, Shelf, Write, Edit Story, Story Detail, Story Reader, and Account flows.
- A shared FastAPI contract under `/api/v1/` (`https://fable-api-cbo8.onrender.com/api/v1/`).
- Room persistence (`FableDatabase` schema v6) for stories (`ownerUserId`), chapters, account-scoped shelf states, reading sessions, reading stats, local writer drafts, and passage highlights.
- Catalog and content support for Fable Originals (`FABLE`), Project Gutenberg (`GUTENBERG`), Standard Ebooks (`STANDARD_EBOOKS`), and MangaDex (`MANGADEX`).
- Authenticated story publishing (`POST /stories`), owner editing (`PATCH /stories/{id}`), owner deletion (`DELETE /stories/{id}`), account lifecycle (`DELETE /auth/me`), and shelf/reading-stat synchronization.

Maintaining two contradictory product definitions makes review, testing, and agent-assisted work unreliable.

## Decision

Fable Android will track the shared Fable product and `/api/v1/` backend contract established alongside `Fable-IOS`, while keeping Android-native Kotlin, Jetpack Compose, Room, and Material 3 implementation idioms.

Specifically:

1. The shared FastAPI service owns external provider integrations (`GUTENBERG`, `STANDARD_EBOOKS`, `MANGADEX`), content normalization, authentication, publishing, and cross-device synchronization.
2. The Android app owns local Room caching (`FableDatabase` v6), reader presentation (prose scroll/paginated + comic webtoon/manga with pinch-to-zoom), in-book search, passage highlights, quote cards, explicit offline chapter cache download/removal, local writer draft auto-save, and resilient reconciliation between local and remote state.
3. Decomposed domain-scoped ViewModels (`CatalogViewModel`, `ExploreViewModel`, `ReaderViewModel`, `StoryEditorViewModel`, `WriterDraftViewModel`, `AccountViewModel`, `ServerConfigViewModel`) and a typed navigation back stack (`FableNavBackStack`) keep presentation state modular and testable.
4. Direct Android HTML scraping, Jsoup extractors, and WorkManager bulk-download queues are out of scope unless a future ADR explicitly reintroduces them.

## Consequences

- Product requirements, architecture docs, phases, and backlog items must describe the shared Fable client architecture.
- Provider resilience means handling remote API and upstream provider failures without losing cached Room data (`coalesceChapters`) or local reading state.
- Release builds must configure an explicit `https://` `FABLE_API_BASE_URL`; debug builds default to `https://fable-api-cbo8.onrender.com/api/v1/` and provide a runtime server URL switcher (`DebugServerConfigStore`) for emulator (`http://10.0.2.2:8000/api/v1/`) or LAN testing.
