# Fable - Technical & Domain Glossary

This document defines standard technical terminology, domain models, and architectural abbreviations used across Fable.

---

## 1. Domain Entities & Core Concepts

- **Book / Novel**: The primary domain entity representing a literary work. Contains metadata (title, author, cover URL, description, status), source origin, and user flags (isFavorite, custom categories).
- **Chapter**: An individual installment belonging to a Book. Contains sequence order, title, URL, release date, download state, and completion flag.
- **Reading Progress**: State record tracking where the reader stopped inside a book: active chapter ID, page number (in Paging mode), scroll offset (in Continuous mode), and last read timestamp.
- **Reading Mode**: The display engine rendering style:
  - **Paging Mode**: Horizontal touch-based page flipping where content is paginated into discrete screen-sized chunks.
  - **Continuous Scroll Mode**: Seamless vertical scrolling of text with infinite scroll physics across chapter boundaries.
- **Library**: The user's collection of favorite books saved for offline tracking, sorting, and automatic chapter update checks.
- **Category**: User-defined tag or folder (e.g., "Reading", "Completed", "Plan to Read", "Sci-Fi") used to organize library books.

---

## 2. Scraping & Source Extraction Pipeline

- **Source**: A remote content provider (e.g., a web novel catalog or web fiction site) identified by a unique `sourceId`.
- **Extractor**: A modular parser class implementing the `Source` contract. Uses OkHttp to make HTTP GET/POST requests and Jsoup to extract structured data from HTML via CSS selectors.
- **Content Sanitizer**: A safety filter running on raw scraped HTML using Jsoup `Safelist` to strip malicious `<script>`, `<style>`, `<iframe>`, and JavaScript event attributes, leaving clean text and formatting tags.
- **Rate Limiter**: A networking throttle ensuring requests to external sources avoid triggering HTTP 429 (Too Many Requests) or IP bans.

---

## 3. Local Persistence & Clean Architecture

- **Room Entity**: A Kotlin data class annotated with `@Entity` representing an SQLite table schema (e.g., `BookEntity`, `ChapterEntity`).
- **DAO (Data Access Object)**: An interface annotated with `@Dao` defining CRUD operations and SQL queries that emit reactive Kotlin `Flow` streams.
- **TypeConverter**: A Room helper class converting non-primitive Kotlin types (e.g., `Instant`, `List<String>`, `ReadingMode`) into SQLite-supported types (TEXT, INTEGER).
- **Repository**: An abstraction layer mediating between data sources (Room database and remote source extractors) and providing a clean API to domain use cases.
- **Use Case / Interactor**: A single-responsibility domain class encapsulating a discrete piece of business logic (e.g., `BookmarkChapterUseCase`, `GetDownloadedChaptersUseCase`).
- **UDF (Unidirectional Data Flow)**: Architectural pattern where UI state flows down from ViewModels to Composables, and events flow up from Composables to ViewModels.

---

## 4. Mobile System & Concurrency

- **StateFlow**: A state-holding observable flow that emits the current and new state updates to collectors. Used in ViewModels to expose UI state to Compose.
- **collectAsStateWithLifecycle**: Android lifecycle-aware Compose extension that collects Kotlin Flows safely, stopping collection when the app goes into the background.
- **WorkManager**: The Android Jetpack background execution library that manages deferrable, guaranteed background tasks even across device reboots.
- **CoroutineWorker**: An implementation of `ListenableWorker` providing native coroutine `doWork()` execution on `Dispatchers.IO`.
- **Foreground Service**: An Android service displaying a persistent user-visible notification to execute active background work (e.g., batch downloading 50 chapters) without OS termination.
- **Coil (Coroutine Image Loader)**: The lightweight, Kotlin-first image loading library used for asynchronous cover and illustration loading, caching, and bitmap transformation.
- **Scoped Storage**: The Android privacy architecture restricting file access to the app's sandboxed private directory (`context.filesDir`) without requiring broad storage permissions.

