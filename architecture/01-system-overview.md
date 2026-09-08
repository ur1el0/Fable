# 01 System Overview

## 1. Project Context & Objectives

**Fable** is a native, offline-first Android reader built with Kotlin and Jetpack Compose. It is designed to replace fragmented, web-based, or ad-heavy reading applications with a fast, private, and distraction-free local reading environment for serialized novels and community fiction.

The core objectives of the system are:
- **Local-First Architecture:** The user's device is the ultimate authority. All library catalogs, reading histories, custom categories, and downloaded chapters are stored locally in a Room SQLite database.
- **Modular Source Architecture:** Decouple novel discovery and web scraping behind a unified `Source` extractor contract, allowing new web sources to be integrated without altering the core reader or database logic.
- **Ergonomic Reading Experience:** Provide an adaptive reader engine supporting both horizontal paging and seamless vertical continuous scrolling, customizable typography (font families, scale, line spacing, margins), and high-contrast color palettes (Light, Dark, AMOLED Pure Black, Sepia).
- **Reliable Background Downloads:** Leverage AndroidX WorkManager to reliably download, sanitize, and cache chapters in the background while adhering to device battery, network, and memory constraints.
- **Zero-Telemetry Privacy:** Zero third-party analytics SDKs, trackers, or advertising libraries. All user data belongs exclusively to the user.

---

## 2. High-Level Architectural Scope

Fable enforces a strict Clean Architecture boundary divided into three primary layers:

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                   │
│         (Jetpack Compose, Material 3, ViewModels)       │
└────────────────────────────┬────────────────────────────┘
                             │ Observes StateFlow / Dispatches Events
                             ▼
┌─────────────────────────────────────────────────────────┐
│                       Domain Layer                      │
│        (Use Cases, Business Models, Repository Interfaces)│
└────────────────────────────▲────────────────────────────┘
                             │ Implemented by
┌────────────────────────────┴────────────────────────────┐
│                        Data Layer                       │
│    (Room SQLite DB, Scoped Storage, OkHttp/Jsoup Source)│
└─────────────────────────────────────────────────────────┘
```

- **Presentation Layer (`com.example.fable.ui`):** Built entirely in Jetpack Compose. ViewModels expose immutable `StateFlow<UiState>` instances and process user interactions via Unidirectional Data Flow (UDF).
- **Domain Layer (`com.example.fable.domain`):** Pure Kotlin business logic. Contains entities (`Book`, `Chapter`, `ReadingProgress`), Use Cases (`GetBookWithChaptersUseCase`, `DownloadChapterUseCase`), and repository contracts. Has zero dependencies on the Android framework.
- **Data Layer (`com.example.fable.data`):** Implements domain repository interfaces. Encapsulates Room database entities/DAOs, internal file storage writers, OkHttp network clients, and Jsoup HTML parsers.
- **Background Pipeline (`com.example.fable.worker`):** Contains WorkManager `CoroutineWorker` implementations executing long-running downloads and chapter sync tasks.

---

## 3. Offline Philosophy & Non-Functional Baselines

1. **Local Single Source of Truth:** Remote web sources are treated strictly as secondary content suppliers. When chapters or book details are fetched, they are immediately persisted to Room; the UI strictly observes reactive database queries via Kotlin `Flow`.
2. **Deterministic Offline Availability:** If an internet connection is severed or a remote website goes offline, every book and chapter previously saved or downloaded in Fable remains fully readable without warnings, popups, or application crashes.
3. **Performance Budget:** 
   - Strict 60fps rendering during continuous scrolling and page flipping (<16ms frame budget).
   - Cold startup time under 800ms.
   - Chapter opening latency under 150ms from local storage.

