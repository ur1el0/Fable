# Fable - Master Engineering Backlog

## Priority 1: Foundation & Data Layer
- [ ] Setup Room SQLite dependencies and KSP compiler in `gradle/libs.versions.toml` and `app/build.gradle.kts`.
- [ ] Define Room Entities: `BookEntity`, `ChapterEntity`, `CategoryEntity`, `HistoryEntity`, `DownloadEntity`.
- [ ] Define DAOs: `BookDao`, `ChapterDao`, `CategoryDao`, `HistoryDao`, `DownloadDao`.
- [ ] Create `FableDatabase` with TypeConverters (`ReadingModeConverter`, `DateConverter`).
- [ ] Implement Clean Architecture domain models (`Book`, `Chapter`, `ReadingProgress`) and repository interfaces.
- [ ] Build in-memory Room database unit tests verifying CRUD and reactive Flow emissions.

---

## Priority 2: Offline Reader Engine (UI Layer)
- [ ] Scaffold `ReaderScreen` with edge-to-edge Compose layout.
- [ ] Implement **Paging Mode** via `HorizontalPager` with touch tap-to-turn zones.
- [ ] Implement **Continuous Scroll Mode** via `LazyColumn` with seamless chapter boundary transitions.
- [ ] Build `ReaderControlsOverlay` (top navigation bar, bottom scrubber, battery/time status).
- [ ] Build `ReaderSettingsBottomSheet` (font scale, line height, paragraph margin, themes).
- [ ] Implement four reading color schemes: Light, Dark, AMOLED Black, Sepia.
- [ ] Integrate automatic reading progress saving to Room on scroll/page turn events.

---

## Priority 3: Modular Source Extractors & Scraping
- [ ] Define `Source` contract interface (`searchBooks`, `getPopularBooks`, `getChapterList`, `getChapterContent`).
- [ ] Configure OkHttp client with rate-limiting, custom User-Agent, and connection pooling.
- [ ] Implement Jsoup HTML parsing engine with strict `Safelist` sanitization (stripping scripts and ads).
- [ ] Implement reference web novel extractor.
- [ ] Add static HTML unit test fixtures to verify DOM selectors against live website changes.

---

## Priority 4: Background Download Pipeline
- [ ] Implement `DownloadWorker` extending AndroidX `CoroutineWorker`.
- [ ] Setup foreground service notification displaying live download progress and cancel action.
- [ ] Implement Scoped Storage chapter file writer (`context.filesDir/chapters/`).
- [ ] Connect download actions to Room `DownloadDao` state transitions (`QUEUED`, `DOWNLOADING`, `COMPLETED`, `FAILED`).
- [ ] Support batch download commands ("Download Next 5/10/All Chapters").

---

## Priority 5: Library & Category Management
- [ ] Build `LibraryScreen` with 2:3 book card grid layout.
- [ ] Implement category tab filtering and category creation dialog.
- [ ] Build `HistoryScreen` showing chronological reading history with quick "Resume" button.
- [ ] Implement local full-text search across titles, authors, and genres in the library.

---

## Priority 6: Theming, Polish & Performance
- [ ] Add Material You dynamic color support for Android 12+.
- [ ] Profile Compose recompositions and apply stability optimizations (`@Immutable`, `derivedStateOf`).
- [ ] Configure Coil image caching limits (20% max heap) to prevent OOM errors.
- [ ] Configure ProGuard/R8 rules to strip debug logs and shrink release APK.

