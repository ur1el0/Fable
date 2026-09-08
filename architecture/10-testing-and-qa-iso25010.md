# 10 Testing Protocols & Quality Assurance (ISO/IEC 25010)

## 1. QA Methodology & Test Matrix

Fable enforces a strict automated quality assurance discipline spanning unit tests, database verification, and Compose UI component tests.

### 1.1 Automated Test Suite Matrix

| Test Suite | Scope & Target | Framework / Tools | Verified Behavior |
| :--- | :--- | :--- | :--- |
| **TC-ROOM-01** | `BookDao` & `ChapterDao` | Room In-Memory SQLite | Inserting books and cascading chapter deletion works without referential errors. |
| **TC-ROOM-02** | `HistoryDao` | Room In-Memory + Turbine | Reading progress emissions emit reactively across Flow collectors. |
| **TC-USECASE-01** | `GetBookWithChaptersUseCase` | JUnit 4 / Kotlin Test | Aggregates book metadata and ordered chapter lists accurately. |
| **TC-USECASE-02** | `SaveReadingProgressUseCase` | JUnit 4 + Test Dispatcher | Debounced scroll positions are correctly saved to repository. |
| **TC-EXTRACT-01** | `SourceExtractor` | Jsoup + Static HTML Fixture | Parses chapter links and titles from static HTML snapshots without network. |
| **TC-EXTRACT-02** | `HtmlSanitizer` | Jsoup Safelist | Strips `<script>`, `<iframe>`, and malicious `onerror` event triggers cleanly. |
| **TC-WORKER-01** | `DownloadWorker` | WorkManager Test Driver | Successfully transitions chapter download state from QUEUED to DOWNLOADED. |
| **TC-COMPOSE-01** | `ReaderControlsOverlay` | Compose Testing Rule | Center tap properly toggles visibility of top and bottom reader chrome. |

---

## 2. ISO/IEC 25010 Quality Evaluation

Fable is systematically evaluated against the ISO/IEC 25010 software product quality standard:

### 2.1 Functional Suitability
- **Completeness:** Provides end-to-end reading capabilities: source discovery, library management, background downloads, and reading progress tracking.
- **Correctness:** Room database operations use SQLite ACID transactions to ensure reading history and bookmarks are never corrupted.

### 2.2 Performance Efficiency
- **Time Behavior:** Reader chapter loading latency under 150ms from local storage; 60fps rendering during continuous scrolling (<16ms frame budget).
- **Resource Utilization:** Baseline memory footprint under 90MB; Coil image cache strictly capped at 20% of maximum heap. Zero CPU background wake locks when downloads are idle.

### 2.3 Usability & Accessibility
- **Ergonomics:** Paging and vertical scroll modes, adjustable typography (font scale, line height, paragraph spacing), and four distinct color themes (Light, Dark, AMOLED, Sepia).
- **Accessibility (a11y):** All clickable controls adhere to the standard 48x48dp touch target minimum. Meaningful `contentDescription` attributes on all non-decorative icons.

### 2.4 Reliability (Offline Resilience)
- **Fault Tolerance:** If remote sources change layout or return HTTP 429/503 errors, previously downloaded chapters remain 100% accessible offline without app crashes.
- **Recoverability:** WorkManager automatically reschedules interrupted chapter downloads when network connectivity is restored.

### 2.5 Security
- **Data Protection:** Scoped Storage isolation prevents unauthorized cross-app data access; Jsoup sanitization prevents arbitrary script execution in reader layouts.

