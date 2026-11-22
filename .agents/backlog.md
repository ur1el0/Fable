# Fable Android Engineering Backlog & Status

## Completed Milestones (Refinement Branch `feat/android-v2-refinement`)

- [x] **Multi-Provider Catalog Parity**: Integrated Standard Ebooks (`STANDARD_EBOOKS`) alongside Fable Originals (`FABLE`), Project Gutenberg (`GUTENBERG`), and MangaDex (`MANGADEX`) with parallel, fault-isolated `refreshCatalog()`.
- [x] **Shared Cancellable Transport & URL Hardening**: Unified network calls on `FableHttpClient.executeCancellable()` and added strict URL validation (`ServerUrlValidator`, `MangaApiService.sanitizeHttpUrl`).
- [x] **Runtime Debug Server URL Switcher**: Added `DebugServerConfigStore` and `ServerConfigViewModel` in **Account → Developer server** for debug builds (`BuildConfig.DEBUG`) while enforcing HTTPS in release builds.
- [x] **Story Ownership, Editing & Deletion**: Upgraded Room to v5 (`MIGRATION_4_5`, `ownerUserId`), added `EditStoryScreen` (with full-story pre-hydration), `PATCH /api/v1/stories/{id}`, and `DELETE /api/v1/stories/{id}`.
- [x] **Account Lifecycle & 401 Recovery**: Added automatic `401 Unauthorized` session reset and permanent account deletion (`DELETE /api/v1/auth/me`) with local Room data purge.
- [x] **Explicit Offline Chapter Cache Management**: Added `observeDownloadedStoryIds()`, `downloadStoryForOffline()`, `removeStoryOfflineCopy()`, `OfflineCacheCard`, `"Offline"` story badges, and the Shelf `Offline` filter tab.
- [x] **ViewModel Decomposition & Typed Navigation**: Replaced monolithic `FableViewModel` with `CatalogViewModel`, `ExploreViewModel`, `ReaderViewModel`, `StoryEditorViewModel`, `WriterDraftViewModel`, `AccountViewModel`, and `ServerConfigViewModel`, plus typed `FableNavBackStack`.
- [x] **Adaptive Theme & Reader Ergonomics**: Added `AppAppearance` (`SYSTEM`, `LIGHT`, `DARK`) with WCAG AA dark palette, per-session screen brightness slider (`ReaderWindowBrightnessEffect`), word-offset paginated progress preservation (`ReaderProgressMapper`), and comic pinch-to-zoom (`1x..4x`) + double-tap zoom (`ComicZoomMath`).
- [x] **In-Book Search, Passage Highlights & Quote Cards**: Upgraded Room to v6 (`MIGRATION_5_6`, `story_highlights`), added cross-chapter search (`InBookSearchEngine`), word-range highlight editor (`PassageHighlightEditorSheet`), Markdown/text export (`HighlightExporter`), and 1080px PNG Quote Card sharing (`QuoteCardExporter`).

## Ongoing Operational Checks

- [ ] Execute `:app:connectedDebugAndroidTest` on a physical device or hardware-accelerated emulator whenever environment changes affect Android runtime APIs or SQLite migrations.
- [ ] Perform live end-to-end smoke checks against the deployed Render backend (`https://fable-api-cbo8.onrender.com/api/v1/`) before cutting a release tag.
