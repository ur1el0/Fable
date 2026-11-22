# Fable Android Engineering Phases

## Phase 1: Shared Backend Alignment & Format-Aware Foundation (Completed)
- Aligned Android domain and data layers with the shared `/api/v1/` FastAPI backend (`ADR 009` & `ADR 010`).
- Integrated all four content providers: **Fable Originals** (`FABLE`), **Project Gutenberg** (`GUTENBERG`), **Standard Ebooks** (`STANDARD_EBOOKS`), and **MangaDex** (`MANGADEX`).
- Implemented shared cancellable OkHttp transport (`FableHttpClient.executeCancellable`), strict URL validation (`MangaApiService.sanitizeHttpUrl`), and parallel fault-isolated catalog refresh (`RoomStoryRepository.refreshCatalog`).
- Fixed 401 session expiry state reset, word-offset paginated reader progress preservation across font/layout changes, and webtoon lazy placeholders/retry/scroll restoration.

## Phase 2: Architecture Decomposition & Story/Account Lifecycle (Completed)
- Added runtime debug server URL switcher (`DebugServerConfigStore`, `ServerUrlValidator`, `ServerConfigViewModel`) with strict release HTTPS enforcement.
- Upgraded Room to schema v5 (`MIGRATION_4_5`) with `ownerUserId`, story editing (`PATCH /stories/{id}` with pre-edit full chapter hydration), story deletion (`DELETE /stories/{id}`), permanent account deletion (`DELETE /auth/me`), and explicit offline chapter download/removal (`downloadStoryForOffline`, `removeStoryOfflineCopy`).
- Replaced the monolithic `FableViewModel` with focused domain-scoped ViewModels (`CatalogViewModel`, `ExploreViewModel`, `ReaderViewModel`, `StoryEditorViewModel`, `WriterDraftViewModel`, `AccountViewModel`, `ServerConfigViewModel`) and a typed navigation back stack (`FableNavBackStack`).

## Phase 3: Reader Parity, Annotations & UI/UX Polish (Completed)
- Added global `AppAppearance` (`SYSTEM`, `LIGHT`, `DARK`) with a WCAG AA high-contrast dark palette (`Color.kt`, `Theme.kt`) and per-session reader screen brightness control (`ReaderWindowBrightnessEffect`).
- Added comic two-finger pinch-to-zoom (`1x..4x`), double-tap zoom (`1x <-> 2.5x`), bounded panning (`ComicZoomMath`), and end-of-chapter transition banners in `MangaChapterReader`.
- Upgraded Room to schema v6 (`MIGRATION_5_6`) with `story_highlights` (`HighlightEntity`, `HighlightDao`, `RoomHighlightRepository`), word-range passage highlight editor (`PassageHighlightEditorSheet`), cross-chapter in-book search (`InBookSearchEngine`, `ReaderSearchSheet`), Markdown/text highlight export (`HighlightExporter`), and 1080px PNG Quote Card generation & `FileProvider` sharing (`QuoteCardExporter`).
- Polished Library (pull-to-refresh, metadata/offline pills), Explore (clearable search, clickable trending writers, active search progress indicator), Shelf (`Offline` filter tab, bordered stat cards), Write (estimated reading time), and Account (appearance selector, password visibility toggles).

## Phase 4: Documentation Synchronization & Runtime Verification (Completed)
- Synchronized `PRODUCT.md`, `readme.md`, `architecture/01..12.md`, `architecture/decisions/009..010.md`, and `.agents/*` with the Room v6 schema, decomposed ViewModels, and full feature set.
- Verified JVM unit test suite (`115` tests), Android Lint (`lintDebug`), debug/release APK assembly, and Android instrumentation compilation/runtime checks.
