# Fable - Development Phases

This document outlines the macro-level roadmap for the Fable project from inception to production release.

---

## Phase 1: Foundation & Data Architecture (Active)
- Establish project architecture guidelines, agent protocols, and ADRs.
- Configure dependency injection baseline and Gradle version catalogs (`libs.versions.toml`).
- Implement Room Database (`FableDatabase`), Entities (`BookEntity`, `ChapterEntity`, `CategoryEntity`, `HistoryEntity`), and DAOs.
- Implement Clean Architecture Domain models and Repository interfaces.
- Write unit tests for Room database operations and schema migrations using in-memory SQLite.

## Phase 2: Offline Reader Core & Rendering Engine
- Build the core Jetpack Compose Reader screen supporting edge-to-edge immersive reading.
- Implement dual reading modes:
  - **Paging Mode**: Horizontal chunked layout with gesture-based page flipping.
  - **Continuous Scroll Mode**: Seamless vertical scroll physics with infinite list loading.
- Build Reader Controls Overlay (top/bottom bars) toggled via center screen tap.
- Build Reader Settings Bottom Sheet (font scale, line height, paragraph margin, typography font selection, themes: Light, Dark, AMOLED, Sepia).
- Implement reading progress checkpointing and persistent scroll state.

## Phase 3: Modular Source Extractors & Networking
- Define the universal `Source` interface (`searchBooks`, `getPopularBooks`, `getChapterList`, `getChapterContent`).
- Configure OkHttp client with custom User-Agent rotation, interceptors, and exponential-backoff rate limiting.
- Implement HTML parsing and content extraction engine using Jsoup with strict `Safelist` sanitization.
- Create initial reference source extractor implementation (e.g., standard web novel catalog).
- Unit test extraction pipelines against static HTML fixtures in `src/test/resources/fixtures/`.

## Phase 4: Download Pipeline & Background WorkManager
- Implement chapter download queue management in Room (`DownloadEntity`).
- Configure AndroidX WorkManager with `DownloadWorker` running on `Dispatchers.IO`.
- Implement Scoped Storage file management for downloaded chapter text and HTML (`context.filesDir/chapters/`).
- Attach Android Foreground Service notification with live download progress, pause, and cancel actions.
- Support batch downloading (e.g., "Download Next 10 Chapters", "Download All Unread").

## Phase 5: Library & Category Management
- Build the Library screen with grid and list views for favorite books.
- Implement category management (create, rename, reorder, assign books to categories).
- Build Reading History screen with last-read timestamps and quick "Resume Reading" action.
- Implement full-text local library search and sorting filters (alphabetical, last read, total chapters).

## Phase 6: Settings, Theming & Performance Optimization
- Implement Material You dynamic theming and user-selectable color palettes.
- Profile and optimize Compose recompositions using Compose Compiler Metrics (`reportsDestination`).
- Optimize memory footprints: Coil bitmap downsampling, memory cache limits, and garbage collection tuning.
- Configure ProGuard/R8 rules for release APK/AAB shrinking and code obfuscation.

## Phase 7: Future Extensions & Ecosystem (Post-v1)
- Optional encrypted cloud backup and multi-device reading progress synchronization.
- External dynamic plugin system for community-contributed source extractors.
- Support for offline local document formats (EPUB, CBZ, PDF).

