# Fable - Product Requirements Document (PRD)

## 1. Project Overview
**Fable** is a fast, reliable, offline-first Android reader built natively with Kotlin and Jetpack Compose. It is designed to replace existing fragmented web readers with a unified, distraction-free, local-first reading application that respects user privacy and system resources.

---

## 2. Target Audience
- **Serial Fiction & Novel Readers**: Users reading long-running serialized web novels, light novels, and community fiction.
- **Offline & Commuter Readers**: Users who require guaranteed offline access to hundreds of chapters without internet dependency during transit or travel.
- **Power Readers**: Users demanding granular typography ergonomics (custom line heights, fonts, margins, AMOLED black mode) to prevent eye strain.

---

## 3. Core Features & Modules

### 3.1 Local Library & Category Management
- Organize saved books into custom categories (e.g., "Reading", "Plan to Read", "Completed", "Favorites").
- Dynamic sorting by Last Read, Alphabetical, Date Added, and Total Chapters.
- Fast local full-text search across titles and authors in the library.

### 3.2 Immersive Reader Engine
- Dual Reading Modes:
  - **Paging Mode**: Horizontal page flipping with custom page boundaries.
  - **Continuous Scroll Mode**: Smooth vertical scrolling across chapter boundaries.
- Reader customization: Font scale (12sp-32sp), line spacing, paragraph margins, font family selection (Sans, Serif, Mono).
- Color themes: Light, Dark, AMOLED Pure Black, Sepia Paper.
- Center-tap reader controls overlay showing progress, chapter titles, battery, and clock.

### 3.3 Modular Source Architecture
- Decoupled `Source` interface supporting custom extractors for different web sources.
- Safe web scraping via OkHttp and Jsoup with robust error handling for HTTP timeouts and anti-bot responses.
- Automatic content cleaning: Stripping unwanted advertisements, site navigation banners, and malicious scripts from chapter text.

### 3.4 Background Download Manager
- Queue individual chapters or batches (e.g., "Next 10 Chapters", "All Unread Chapters") for background download.
- Managed by AndroidX WorkManager to guarantee task completion even if the app process is terminated.
- Foreground Service notification with live progress tracking, pause, and cancellation.
- Chapters stored safely in app-internal Scoped Storage (`context.filesDir/chapters/`).

### 3.5 Reading History & Progress Tracking
- Exact progress checkpointing (chapter ID, scroll position, page index) updated seamlessly in real time.
- Reading History screen listing recently read titles with one-tap "Resume Reading" action.
- Mark chapters as read/unread manually or automatically upon reaching the end of a chapter.

---

## 4. Non-Functional Requirements

- **Offline Resilience**: Once chapters are downloaded, the reader must be 100% functional with airplane mode enabled.
- **Performance**: Constant 60fps rendering during scrolling and page flips; chapter load time under 150ms.
- **Memory Footprint**: App baseline memory usage below 90MB; strictly bounded image memory caching via Coil to prevent OOM errors.
- **Battery Efficiency**: Zero background CPU wake locks when no active downloads are queued.
- **Data Privacy & Sovereignty**: Zero third-party telemetry, tracking SDKs, or external user logins in v1. All user data remains locally on the user's device.

