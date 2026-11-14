# ADR 009: Android Product Alignment With Fable-IOS

## Status
Accepted

## Context
The Android repository began with a generic offline novel-reader plan, while Fable-IOS now defines a broader shared product: curated stories, prose and manga catalogs, a personal shelf, writing and publishing, accounts, reader preferences, and backend synchronization. Building Android from the older plan alone would create mismatched domain models and duplicate provider integrations.

## Decision
Android follows the current Fable-IOS product and FastAPI contracts for shared product workflows. The shared domain uses `Story`, `Chapter`, `GenreCategory`, and `Writer`, with provider and content-format values matching the backend. Backend access is grouped by catalog, account, shelf, and publishing workflows.

Room remains the Android source of truth for locally saved content and reading state. Remote catalog data is cached locally, and remote failures must not prevent access to content already saved on the device. Provider-specific network behavior stays behind an API boundary.

### Manga Provider Boundary

Manga catalog and chapter requests use the shared FastAPI API, which owns the MangaDex provider integration for both mobile clients. Android uses the same API base URL for Manga and other workflows, stores fetched catalog and chapter metadata in Room, and accepts image URLs only from HTTPS MangaDex cover and chapter hosts with the expected path format. Image bytes are delivered by MangaDex's public CDN rather than through a second Fable service.

## Consequences
- Android and iOS use the same shared backend identifiers, field names, provider values, and user workflows; Manga data preserves the existing MangaDex provider identifiers and stable story IDs.
- Android-specific storage and UI continue to use Room, Kotlin Flow, ViewModels, and Compose.
- Android builds require only the shared Fable API URL; there is no separately hosted Manga service to provision or keep awake.
- Manga image URLs are validated in the remote data source before they reach image loading, and unsupported hosts or path formats fail closed.
- The existing direct web-extractor architecture is reserved for a later extension and is not used for other current provider catalogs.
