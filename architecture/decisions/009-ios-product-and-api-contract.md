# ADR 009: Android Product Alignment With Fable-IOS

## Status
Accepted

## Context
The Android repository began with a generic offline novel-reader plan, while Fable-IOS now defines a broader shared product: curated stories, prose and manga catalogs, a personal shelf, writing and publishing, accounts, reader preferences, and backend synchronization. Building Android from the older plan alone would create mismatched domain models and duplicate provider integrations.

## Decision
Android will follow the current Fable-IOS product and FastAPI contracts. The shared domain uses `Story`, `Chapter`, `GenreCategory`, and `Writer`, with provider and content-format values matching the backend. Backend access is grouped by catalog, provider, account, shelf, and publishing workflows.

Room remains the Android source of truth for locally saved content and reading state. Remote catalog data is cached locally, and remote failures must not prevent access to content already saved on the device. Provider-specific network behavior stays behind the Fable backend API.

## Consequences
- Android and iOS use the same backend identifiers, field names, provider values, and user workflows.
- Android-specific storage and UI continue to use Room, Kotlin Flow, ViewModels, and Compose.
- The existing direct web-extractor architecture is reserved for a later extension and is not used for the current provider catalogs.
