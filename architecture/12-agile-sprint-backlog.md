# 12 Agile Sprint Roadmap (Fable v1)

## 1. Iterative Development Sprints

Following the Agile Scrum methodology, Fable v1 engineering progresses across discrete two-week sprints:

- **Sprint 1: Architecture Base & Room Persistence:**
  - Initialize project architecture, agent rules, and ADRs.
  - Configure Room database dependencies and KSP compiler in Gradle version catalogs.
  - Implement Room Entities (`BookEntity`, `ChapterEntity`, `CategoryEntity`, `HistoryEntity`, `DownloadEntity`) and DAOs.
  - Build Clean Architecture domain models and repository interfaces.
  - Implement in-memory SQLite unit test suite verifying cascade deletes and reactive Flow emissions.

- **Sprint 2: Jetpack Compose Reader Core & Ergonomics:**
  - Scaffold `ReaderScreen` with edge-to-edge Compose layout.
  - Implement Paging Mode via `HorizontalPager` with touch tap-to-turn zones.
  - Implement Continuous Scroll Mode via `LazyColumn` with chapter boundary pre-loading.
  - Build `ReaderControlsOverlay` (top app bar, bottom progress scrubber) toggled via center tap.
  - Build `ReaderSettingsBottomSheet` for font size, line spacing, margins, and theme selection.
  - Implement debounced reading progress saving to Room.

- **Sprint 3: Modular Source Extractors & Scraping Pipeline:**
  - Define the universal `Source` interface.
  - Configure OkHttp client with rate-limiting, custom User-Agent, and connection pooling.
  - Implement Jsoup HTML extraction engine with strict `Safelist` sanitization.
  - Create initial reference web novel extractor.
  - Create static HTML test fixtures in `src/test/resources/fixtures/` to verify DOM parsing without network dependencies.

- **Sprint 4: Background Chapter Download Engine:**
  - Implement `DownloadWorker` extending AndroidX `CoroutineWorker`.
  - Configure Android 14+ Foreground Service notification with live progress and cancellation actions.
  - Implement Scoped Storage file writer (`context.filesDir/chapters/`) with path traversal verification.
  - Connect download triggers to Room `DownloadDao` state transitions (`QUEUED`, `DOWNLOADING`, `DOWNLOADED`, `FAILED`).
  - Support batch downloading actions ("Download Next 10 Chapters", "Download All Unread").

- **Sprint 5: Library & Category Management:**
  - Build `LibraryScreen` with 2:3 book card grid layout and category tab filtering.
  - Implement category management dialogs (create, rename, delete, reorder).
  - Build `HistoryScreen` displaying chronological reading progress with one-tap "Resume Reading".
  - Implement local full-text search across titles, authors, and sources.

- **Sprint 6: Theming, Performance & Production Hardening:**
  - Implement Material You dynamic color theming for Android 12+.
  - Integrate AMOLED pure black mode and Sepia paper reading theme.
  - Profile and optimize Compose recompositions using Compose Compiler Metrics and Layout Inspector.
  - Configure Coil image cache memory limits (20% maximum heap) to prevent OOM errors.
  - Configure ProGuard/R8 release rules to strip debug logs and shrink APK size.

