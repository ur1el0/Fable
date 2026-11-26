# Fable Android

Fable Android is the native **Kotlin** and **Jetpack Compose** client for the shared Fable reading and community-publishing platform (companion to **Fable-IOS**). It connects to the shared FastAPI backend (`https://fable-api-cbo8.onrender.com/api/v1/`) while using **Room (SQLite schema v6)** as an offline-first single source of truth for cached stories, chapters, shelf progress, reading sessions, writer drafts, and passage highlights.

---

## Key Capabilities

- **Multi-Source Format-Aware Discovery (`ExploreScreen`)**:
  - **Books (`ContentFormat.PROSE`)**: Fable Originals (`FABLE`), Project Gutenberg (`GUTENBERG`), and Standard Ebooks (`STANDARD_EBOOKS`).
  - **Manga & Comics (`ContentFormat.COMIC`)**: MangaDex (`MANGADEX`) discovery, chapter hydration, and page streaming.
  - Parallel, fault-isolated catalog refresh (`RoomStoryRepository.refreshCatalog`) that preserves cached chapter bodies when summary responses or upstream errors occur.
- **Prose & Comic Reader Engine (`StoryReaderScreen`, `MangaChapterReader`)**:
  - **Prose Modes**: Continuous vertical scroll and horizontal paginated book mode with word-offset progress mapping (`ReaderProgressMapper`) across typography and layout changes.
  - **Comic Modes**: Vertical webtoon strip and horizontal manga pager with 2-finger pinch-to-zoom (`1x`–`4x`), double-tap zoom (`1x <-> 2.5x`), bounded panning, lazy page placeholders, per-page retry, and end-of-chapter transitions.
  - **Display & Ergonomics**: Paper, Sepia, and Night reader themes; Serif, Sans, and Mono fonts; adjustable text size (`15sp`–`26sp`), line height (`1.25x`–`1.9x`), per-session screen brightness control (`Auto` or manual slider), and global app appearance (`System`, `Light`, `Dark`) with a WCAG AA high-contrast dark palette.
  - **In-Book Search, Highlights & Quote Cards**: Cross-chapter keyword search (`InBookSearchEngine`), passage word-range highlights (`TERRACOTTA`, `AMBER`, `SAGE`) with notes stored in Room (`story_highlights`), Markdown/text highlight export (`HighlightExporter`), and 1080px PNG Quote Card generation and sharing (`QuoteCardExporter`).
- **Offline Chapter Cache & Personal Shelf (`ShelfScreen`, `StoryDetailScreen`)**:
  - Explicit **Download for offline** and **Remove offline copy** controls (`OfflineCacheCard`) with `"Offline"` badges across Library, Explore, and Shelf (including a dedicated `Offline` shelf filter tab).
  - Account-scoped shelf states (`shelf_states`) and local-first reading session tracking (`reading_sessions`, `reading_stats`) with background synchronization.
- **Writer Studio & Story Lifecycle (`WriteScreen`, `EditStoryScreen`)**:
  - Auto-saved local drafts (`writer_drafts`), live word count and estimated read time, multi-chapter marker parsing (`Chapter 1:`), and story publishing.
  - Story owners can edit published manuscripts (`PATCH /api/v1/stories/{id}` with full-content pre-hydration) or delete stories (`DELETE /api/v1/stories/{id}`).
- **Account Management & Debug Runtime Server Switcher (`AccountScreen`)**:
  - Registration, sign-in, profile editing, password reset, automatic `401` session expiry recovery, and permanent account deletion (`DELETE /api/v1/auth/me`).
  - Debug builds (`BuildConfig.DEBUG`) expose a runtime server URL switcher (`DebugServerConfigStore` + `ServerUrlValidator`) to switch between the hosted Render API, Android Emulator localhost (`http://10.0.2.2:8000/api/v1/`), or a physical-device LAN IP without recompiling. Release builds enforce a compile-time HTTPS `FABLE_API_BASE_URL` and disable runtime URL switching.

---

## Architecture Overview

Fable Android follows Clean Architecture with strict Unidirectional Data Flow (UDF):

- **Presentation Layer (`app/src/main/java/com/example/fable/ui/`)**:
  - Typed navigation back stack (`FableNavBackStack`, `FableTab`, `FableDestination`).
  - Focused domain ViewModels (`CatalogViewModel`, `ExploreViewModel`, `ReaderViewModel`, `StoryEditorViewModel`, `WriterDraftViewModel`, `AccountViewModel`, `ServerConfigViewModel`).
  - Stateless Material 3 Compose screens and modular reader/annotation sheets.
- **Domain Layer (`app/src/main/java/com/example/fable/domain/`)**:
  - Pure Kotlin domain models (`Story`, `Chapter`, `StoryHighlight`, `ReaderPreferences`, `AppAppearance`, `AccountSession`, `ReadingStatsSummary`, `WriterDraft`), repository contracts, and reading engines (`ReaderTextPaginator`, `ReaderProgressMapper`, `InBookSearchEngine`, `HighlightExporter`, `ComicZoomMath`).
- **Data Layer (`app/src/main/java/com/example/fable/data/`)**:
  - **Room v6 Database (`FableDatabase`)**: 7 tables (`stories`, `chapters`, `shelf_states`, `reading_sessions`, `reading_stats`, `writer_drafts`, `story_highlights`) with tested schema migrations (`MIGRATION_1_2` through `MIGRATION_5_6`) and exported schemas under `app/schemas/`.
  - **Remote API (`FableApiService`, `MangaApiService`, `FableHttpClient`, `ServerUrlValidator`)**: Shared cancellable OkHttp transport (`connectTimeout = 10s`, `readTimeout = 20s`, `writeTimeout = 20s`, `callTimeout = 30s`) with `kotlinx.serialization`.

Detailed architecture documentation lives in [`PRODUCT.md`](file:///home/dokja/.local/share/fable-worktrees/android-refinement/PRODUCT.md), [`.agents/ARCHITECTURE.md`](file:///home/dokja/.local/share/fable-worktrees/android-refinement/.agents/ARCHITECTURE.md), and [`architecture/01-system-overview.md`](file:///home/dokja/.local/share/fable-worktrees/android-refinement/architecture/01-system-overview.md).

---

## Build, Test & Verification Commands

Set `ANDROID_HOME` and `ANDROID_SDK_ROOT` to your local Android SDK (e.g. `/home/dokja/Android/Sdk`):

```bash
# Run all JVM unit tests, Android Lint, debug APK build, and compile androidTest sources
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin

# Run instrumentation tests on a connected device or running emulator
./gradlew :app:connectedDebugAndroidTest

# Validate release build configuration, R8 shrinking, and HTTPS API base URL check
./gradlew :app:assembleRelease -PfableReleaseApiBaseUrl=https://fable-api-cbo8.onrender.com/api/v1/
```

### Configuring the API Base URL

- **Debug Default**: `https://fable-api-cbo8.onrender.com/api/v1/` (override at build time via `-PfableDebugApiBaseUrl=...` or `FABLE_DEBUG_API_BASE_URL`, or at runtime in **Account → Developer server**).
- **Release Builds**: Require an explicit `https://` URL ending with `/` via `-PfableReleaseApiBaseUrl=...` or `FABLE_RELEASE_API_BASE_URL` (validated by the `:app:validateReleaseApiBaseUrl` Gradle task).
