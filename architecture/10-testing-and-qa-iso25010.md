# 10 Testing Protocols & Quality Assurance (ISO/IEC 25010)

## 1. QA Methodology & Test Matrix

Fable Android enforces an automated quality assurance discipline spanning JVM unit and contract tests, repository and database isolation tests, and Compose UI instrumentation tests.

### 1.1 Automated Test Suite Matrix

| Test Class | Test Layer | Target / Subject Under Test | Verified Behavior |
| :--- | :--- | :--- | :--- |
| `FableApiServiceTest` | Data (Remote) | `FableApiService` & DTO contracts | Correct JSON parsing, camelCase aliases, and error handling for Gutenberg, Standard Ebooks, and auth endpoints. |
| `RoomStoryRepositoryGutenbergIntegrationTest` | Data (Repository) | `RoomStoryRepository` | Cache-first catalog lookup, on-demand chapter hydration, and non-destructive Room cache updates. |
| `RoomStoryRepositoryShelfScopeTest` | Data (Repository) | `RoomStoryRepository` | Multi-tenant shelf isolation: ensures guest and authenticated user shelves never bleed state across sign-in/sign-out. |
| `ShelfSyncPlannerTest` | Data (Repository) | `ShelfSyncPlanner` | Last-Write-Wins (LWW) conflict resolution and payload generation for `/api/v1/shelf/sync`. |
| `RoomReadingStatsRepositoryTest` | Data (Repository) | `RoomReadingStatsRepository` | Reading session outbox queueing, local session persistence, and aggregate statistics updates. |
| `StoryShelfStateMapperTest` | Data (Local) | `RoomMappers` & `ShelfSyncMappers` | Bidirectional entity-to-domain mapping between `StoryEntity`, `ShelfStateEntity`, and `Story`. |
| `ReadingStatsDtoTest` | Data (Remote) | `ReadingStatsDtos` | Serialization and deserialization parity with backend `/api/v1/auth/me/reading-stats`. |
| `ReaderProgressMapperTest` | Domain (Logic) | `ReaderProgressMapper` | Deterministic bidirectional conversion between overall progress percentage, active chapter percentage, and page indices. |
| `ReaderPagePackerTest` | Domain (Logic) | `ReaderPagePacker` | Accurate text pagination, character budgeting, and boundary split calculations for Paginated Mode. |
| `AccountViewModelTest` | Presentation | `AccountViewModel` | Reactive state flow for registration, login, profile retrieval, and session logout. |
| `FableViewModelChapterLoadingTest` | Presentation | `FableViewModel` | Reader state management, on-demand hydration triggers, and loading indicator transitions. |
| `FableViewModelProviderSearchTest` | Presentation | `FableViewModel` | Independent provider search dispatch, provider chip filtering, and partial error state handling. |
| `ShelfStateMigrationTest` | Android Test (DAO) | `FableDatabase` (`MIGRATION_1_2`) | SQLite schema migration from v1 to v2: creates `shelf_states` and copies existing shelf data under `guest` scope. |
| `ReadingStatsDaoTest` | Android Test (DAO) | `ReadingStatsDao` | In-memory SQLite verification of session queueing and stats upserts. |
| `SharedPreferencesReaderPreferencesRepositoryTest` | Android Test (Local) | `ReaderPreferences` | Persistence and restoration of font, scale, theme, spacing, and layout options. |
| `ContinuousChapterListTest` | Android Test (UI) | `ContinuousChapterList` | Compose rendering and scroll behavior in Continuous Scroll Mode. |
| `ExploreScreenTest` | Android Test (UI) | `ExploreScreen` | Provider filter chip selection, search input, and story card clicks. |
| `ReaderControlsOverlayTest` | Android Test (UI) | `ReaderControlsOverlay` | Center tap toggles top/bottom reader chrome without interfering with text reading. |
| `ReaderTapToTurnTest` | Android Test (UI) | `StoryReaderScreen` | Left/right edge tap zones trigger page turning in Paginated Mode. |
| `StoryReaderProgressRestorationTest` | Android Test (UI) | `StoryReaderScreen` | Restoring previously saved reading progress on chapter open. |

---

## 2. ISO/IEC 25010 Quality Evaluation

Fable Android is systematically evaluated against the ISO/IEC 25010 software product quality standard:

### 2.1 Functional Suitability
- **Completeness:** Provides end-to-end literature workflows: federated discovery (Standard Ebooks, Gutenberg, Originals), on-demand chapter reading, personal shelf, writing studio, and account management.
- **Correctness:** Room database operations use SQLite transactions with foreign key cascades, guaranteeing that chapter caches and shelf entries are never orphaned.

### 2.2 Performance Efficiency
- **Time Behavior:** Local chapter load time from Room SQLite is under 50ms; Compose 60fps rendering during continuous fling scrolling (<16ms frame budget).
- **Resource Utilization:** Baseline memory footprint under 60MB; Coil image cache strictly bounded; zero CPU background wake locks when reading or idle.

### 2.3 Usability & Accessibility
- **Ergonomics:** Paginated and Continuous Scroll layouts, adjustable typography (80%–150% font scale, 3 line spacings), and 6 distinct reading themes (White, Paper, Sepia, Charcoal, Midnight, OLED).
- **Accessibility (a11y):** All clickable controls adhere to the standard 48x48dp touch target minimum. Meaningful `contentDescription` attributes on all interactive and non-decorative components.

### 2.4 Reliability & Fault Isolation
- **Federated Resilience:** Independent queries to Standard Ebooks and Gutenberg isolate upstream latency or HTTP 502/504 errors, ensuring healthy provider results are always delivered.
- **Offline Determinism:** Previously hydrated stories and chapters remain 100% readable without an active internet connection. Failed sync requests leave local shelf state and reading session queues intact for automatic retry.

### 2.5 Security
- **Data Protection:** Scoped Storage isolation prevents unauthorized cross-app data access; Jsoup sanitization prevents arbitrary script execution in reader layouts.

