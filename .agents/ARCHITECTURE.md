# Fable - System Architecture

## 1. High-Level Architecture
Fable follows an offline-first Clean Architecture pattern with Unidirectional Data Flow (UDF):
- **UI Layer (Presentation)**: Purely declarative user interface built with Jetpack Compose and Material 3, orchestrated by ViewModels exposing immutable `StateFlow<UiState>`.
- **Domain Layer**: Pure Kotlin business logic encapsulating Use Cases (e.g., `GetBookWithChaptersUseCase`, `DownloadChapterUseCase`), business models, and repository interfaces.
- **Data Layer**: Single source of truth backed by a local Room (SQLite) database, accompanied by app-internal Scoped Storage for downloaded chapters and cached images.
- **Source Extractor Layer**: Modular web-scraping extractors implementing standard contracts using OkHttp and Jsoup to parse novels and chapters from remote web sources.
- **Background Task Pipeline**: AndroidX WorkManager orchestrating reliable, constraint-aware background downloads and chapter updates.

## 2. Mobile Infrastructure & Runtime Environment
- **Platform**: Android Native (Kotlin 2.2+)
- **SDK Target**: Min SDK 24 (Android 7.0 Nougat) to Target SDK 36 (Android 16)
- **Build System**: Gradle Kotlin DSL with centralized dependency management via `gradle/libs.versions.toml`
- **Asynchronous Concurrency**: Kotlin Coroutines & Asynchronous Reactive Streams via `StateFlow` / `SharedFlow`

## 3. Core Technologies
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Compose BOM 2026.02.01, Material 3, Compose Navigation)
- **Local Persistence**: Room SQLite with KSP schema verification
- **Networking**: OkHttp 4.x / 5.x with custom User-Agent interception and rate-limiting
- **Content Parsing**: Jsoup for robust HTML parsing, CSS-selector DOM querying, and tag sanitization
- **Image Pipeline**: Coil 3.x with disk and memory LRU caching
- **Background Execution**: AndroidX WorkManager (`CoroutineWorker`)

## 4. Security Architecture
- **Offline Data Sovereignty**: All user library data, reading progress, and custom categories reside strictly on-device with zero telemetry or third-party tracking.
- **Storage Sandboxing**: All chapters and cached media are stored inside app-specific internal storage (`context.filesDir`), immune to cross-app inspection on Android 10+.
- **HTML Sanitization**: External web chapter HTML is cleansed via Jsoup `Safelist` to strip `<script>`, `<style>`, `<iframe>`, and JavaScript event attributes before rendering.
- **Safe Network Layer**: Enforces TLS 1.3/HTTPS and blocks cleartext traffic by default in `AndroidManifest.xml`.

## 5. Source Extractor System
- **Decoupled Contracts**: Extractors implement a universal `Source` interface (`getPopularBooks()`, `searchBooks()`, `getChapterList()`, `getChapterContent()`).
- **Resilient Fallbacks**: If a source layout changes or fails due to network limitations, cached local chapters remain 100% accessible offline without app crashes.

