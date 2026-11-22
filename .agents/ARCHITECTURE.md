# Fable Android Architecture Guide

This document summarizes the active architecture of Fable Android (`com.example.fable`). See [`architecture/01-system-overview.md`](file:///home/dokja/.local/share/fable-worktrees/android-refinement/architecture/01-system-overview.md) through [`architecture/11-build-and-ci-cd.md`](file:///home/dokja/.local/share/fable-worktrees/android-refinement/architecture/11-build-and-ci-cd.md) and [`PRODUCT.md`](file:///home/dokja/.local/share/fable-worktrees/android-refinement/PRODUCT.md) for deep dives.

---

## 1. Layer Breakdown

### Presentation Layer (`app/src/main/java/com/example/fable/ui/`)
- **App Shell & Typed Navigation**:
  - `FableApp.kt` coordinates the 5 top-level tabs (`FableTab`: `LIBRARY`, `EXPLORE`, `SHELF`, `WRITE`, `ACCOUNT`) and drill-down destinations (`FableDestination`: `TabRoot`, `StoryDetail`, `StoryReader`, `StoryEdit`) using `FableNavBackStack` (`ui/navigation/FableNavigation.kt`).
- **Decomposed Domain-Scoped ViewModels (`ui/viewmodel/`)**:
  - `CatalogViewModel`: Catalog state, offline downloaded story IDs (`observeDownloadedStoryIds`), pull-to-refresh, and explicit offline download/removal (`downloadStoryForOffline`, `removeStoryOfflineCopy`).
  - `ExploreViewModel`: Format-aware discovery (`ContentFormat.PROSE` vs `COMIC`), provider filtering (`FABLE`, `GUTENBERG`, `STANDARD_EBOOKS`, `MANGADEX`), and debounced cancellable search.
  - `ReaderViewModel`: Chapter hydration, debounced reading progress (`350ms`), reading session recording, reader display preferences, and Room passage highlights.
  - `StoryEditorViewModel`: Story publishing, pre-edit full chapter hydration, owner story updates (`PATCH /stories/{id}`), and owner story deletion (`DELETE /stories/{id}`).
  - `WriterDraftViewModel`: Local Room draft auto-save (`writer_drafts`).
  - `AccountViewModel`: Authentication, profile management, password reset, account deletion (`DELETE /auth/me`), reading stats sync, and 401 session expiry reset.
  - `ServerConfigViewModel`: Debug-only runtime server URL switcher (`DebugServerConfigStore`).
- **Screens & Reader Components (`ui/screens/`)**:
  - `LibraryScreen.kt`, `ExploreScreen.kt`, `ShelfScreen.kt`, `WriteScreen.kt`, `EditStoryScreen.kt`, `AccountScreen.kt`, `StoryDetailScreen.kt`, `StoryReaderScreen.kt`.
  - Modular reader components in `ui/screens/components/`: `ContinuousChapterList.kt`, `PaginatedChapterPager.kt`, `MangaChapterReader.kt`, `ReaderControlsOverlay.kt`, `ReaderChaptersSheet.kt`, `ReaderDisplayOptionsSheet.kt`, `ReaderAnnotationSheets.kt`, and `QuoteCardExporter.kt`.

### Domain Layer (`app/src/main/java/com/example/fable/domain/`)
- **Models (`domain/model/`)**: `Story`, `Chapter`, `ChapterPage`, `ContentFormat`, `StoryProvider`, `StoryHighlight`, `HighlightColor`, `ReaderPreferences`, `AppAppearance`, `AccountSession`, `ReadingStatsSummary`, `ReadingSessionRecord`, `WriterDraft`, `StorySubmission`.
- **Reading & Annotation Engines (`domain/reading/`)**: `ReaderTextPaginator`, `ReaderProgressMapper`, `InBookSearchEngine`, `HighlightExporter`, `ComicZoomMath`.
- **Repository Interfaces (`domain/repository/`)**: `StoryRepository`, `HighlightRepository`, `AccountRepository`, `ReadingStatsRepository`, `WriterDraftRepository`, `ReaderPreferencesRepository`, `ServerConfigRepository`.

### Data Layer (`app/src/main/java/com/example/fable/data/`)
- **Room Database (`FableDatabase`, schema version 6)**:
  - 7 tables: `stories` (with `ownerUserId`), `chapters`, `shelf_states`, `reading_sessions`, `reading_stats`, `writer_drafts`, `story_highlights`.
  - Migrations: `MIGRATION_1_2`, `MIGRATION_2_3`, `MIGRATION_3_4`, `MIGRATION_4_5`, `MIGRATION_5_6` with exported JSON schemas in `app/schemas/com.example.fable.data.local.FableDatabase/1.json`..`6.json`.
- **Remote Transport (`data/remote/`)**:
  - `FableHttpClient`: Shared `OkHttpClient` with bounded timeouts (`10s` connect, `20s` read/write, `30s` call) and coroutine-aware `executeCancellable()`.
  - `FableApiService` & `MangaApiService`: Normalized REST clients for Fable Originals, Project Gutenberg, Standard Ebooks, MangaDex, auth, publishing, and shelf/stats sync.
  - `ServerUrlValidator` & `DebugServerConfigStore`: Runtime server URL validation and debug switching.
