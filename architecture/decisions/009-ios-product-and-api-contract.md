# ADR 009: Android Product Alignment With Fable-IOS

## Status
Accepted

## Context
The Android repository began with a generic offline novel-reader plan, while Fable-IOS now defines a broader shared product: curated stories, prose and manga catalogs, a personal shelf, writing and publishing, accounts, reader preferences, and backend synchronization. Building Android from the older plan alone would create mismatched domain models and duplicate provider integrations.

## Decision
Android follows the current Fable-IOS product and FastAPI contracts for shared product workflows. The shared domain uses `Story`, `Chapter`, `GenreCategory`, and `Writer`, with provider and content-format values matching the backend. Backend access is grouped by catalog, account, shelf, and publishing workflows.

Room remains the Android source of truth for locally saved content and reading state. Remote catalog data is cached locally, and remote failures must not prevent access to content already saved on the device. Provider-specific network behavior stays behind an API boundary.

### Approved Android Manga Exception

The Android MangaDex integration is an explicit exception to the shared-provider rule. Manga catalog, chapter-manifest, and MangaDex image-proxy requests use the standalone FastAPI service in `manga-backend/`, configured independently from the shared Fable API. This isolates MangaDex latency and image delivery from the shared iOS service and from non-Manga Android workflows. The iOS backend remains unchanged. Android still persists Manga catalog/chapter data in Room, and the local service does not replace the shared API for accounts, shelves, writing, prose, or other product behavior.

## Consequences
- Android and iOS use the same shared backend identifiers, field names, provider values, and user workflows; Manga data preserves the existing MangaDex provider identifiers and stable story IDs.
- Android-specific storage and UI continue to use Room, Kotlin Flow, ViewModels, and Compose.
- The Manga service is separately configurable and deployable; release builds require its HTTPS URL. It does not change the iOS API or introduce Manga provider calls into the Compose/UI layer.
- The existing direct web-extractor architecture is reserved for a later extension and is not used for other current provider catalogs.
