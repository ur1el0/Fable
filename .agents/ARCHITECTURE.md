# Fable Android - System Architecture

## 1. High-Level Architecture
Fable Android follows an offline-first Clean Architecture pattern with Unidirectional Data Flow (UDF), operating as the native Android companion to Fable-IOS and the shared FastAPI backend (per ADR 009):
- **UI Layer (Presentation)**: Purely declarative user interface built with Jetpack Compose and Material 3, orchestrated by ViewModels exposing immutable `StateFlow<UiState>`.
- **Domain Layer**: Pure Kotlin business models (`Story`, `Chapter`, `UserProfile`, `ReadingStats`, `ReaderPreferences`) and repository contracts (`StoryRepository`, `AccountRepository`, `ReadingStatsRepository`, `ReaderPreferencesRepository`).
- **Data Layer**: Single source of truth backed by a local Room SQLite database (Schema Version 3 across 5 tables), paired with `FableApiService` for backend communication.
- **Federated Ingestion**: Ingests curated literature from Standard Ebooks, Project Gutenberg, and Fable Originals through the shared FastAPI service with independent query dispatch and fault isolation.
- **Deferred Subsystems**: Direct client-side web scraping (Jsoup) and WorkManager batch downloads are deferred per ADR 009 in favor of on-demand chapter hydration.

## 2. Mobile Infrastructure & Runtime Environment
- **Platform**: Android Native (Kotlin 2.2+)
- **SDK Target**: Min SDK 24 (Android 7.0 Nougat) to Target SDK 36 (Android 16)
- **Build System**: Gradle Kotlin DSL with centralized dependency management via `gradle/libs.versions.toml`
- **Asynchronous Concurrency**: Kotlin Coroutines & Asynchronous Reactive Streams via `StateFlow` / `Flow`

## 3. Core Technologies
- **Language**: Kotlin 2.2+
- **UI Framework**: Jetpack Compose (Compose BOM 2026.02.01, Material 3, Navigation)
- **Local Persistence**: Room SQLite v3 with KSP schema exports (`stories`, `chapters`, `shelf_states`, `reading_sessions`, `reading_stats`)
- **Networking**: OkHttp with `kotlinx.serialization` communicating with the shared FastAPI backend
- **Image Pipeline**: Coil 3 with bounded memory LRU caching
- **Reader Engine**: Dual-mode rendering (Horizontal Paginated Mode via `HorizontalPager` and Continuous Scroll Mode via `LazyColumn`)

## 4. Security & Privacy Architecture
- **Offline Data Sovereignty**: Cached stories, hydrated chapters, reader preferences, and local reading state reside strictly on-device with zero telemetry or third-party tracking.
- **Account-Scoped Shelf Isolation**: Room partitions shelf states and reading progress by `ownerId` (`"guest"` vs verified user ID).
- **Network Security**: Strict HTTPS release build URL validation, cleartext traffic disabled in production, and auto-backup exclusions for session credentials and SQLite databases.

## 5. Federated Source Architecture
- **Contract Parity**: Follows the shared FastAPI contract for Gutenberg and Standard Ebooks.
- **Cross-Provider Edition Disambiguation**: Deterministic composite IDs (`sourceProvider` + `providerId`) guarantee works sharing titles across providers maintain distinct cache entries and separate shelf progress.
- **Fault-Tolerant Hydration**: On-demand chapter retrieval caches full text into SQLite, ensuring uninterrupted offline reading.

