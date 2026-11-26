# 01 System Overview & Architecture

## 1. Architectural Pattern: Offline-First Clean Architecture + MVVM + UDF

Fable Android implements **Offline-First Clean Architecture** paired with **Model-View-ViewModel (MVVM)** and strict **Unidirectional Data Flow (UDF)**.

The local **Room SQLite database (`FableDatabase`, schema version 6)** serves as the **Single Source of Truth (SSOT)** for cached stories, chapters, shelf progress, reading sessions, reading stats, writer drafts, and passage highlights. Remote operations against the shared Fable FastAPI backend (`FableApiService`, `MangaApiService`) fetch normalized JSON contracts, persist or reconcile them into Room, and let Jetpack Compose screens observe reactive Kotlin `Flow` streams.

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        Presentation Layer (Jetpack Compose)                │
│  FableApp + Typed FableNavBackStack (FableTab, FableDestination)           │
│  Screens: Library, Explore, Shelf, Write, EditStory, Account,              │
│           StoryDetail, StoryReader (Prose + Manga/Webtoon + Annotations)   │
└───────────────────┬───────────────────────────────────────▲────────────────┘
                    │ User Intents / Events                 │ Immutable StateFlow<UiState>
                    ▼                                       │
┌────────────────────────────────────────────────────────────────────────────┐
│                     Focused Domain-Scoped ViewModels                       │
│  CatalogViewModel · ExploreViewModel · ReaderViewModel                     │
│  StoryEditorViewModel · WriterDraftViewModel · AccountViewModel            │
│  ServerConfigViewModel                                                     │
└───────────────────┬───────────────────────────────────────▲────────────────┘
                    │ Calls Suspend / Flow Contracts        │ Domain Models & Flows
                    ▼                                       │
┌────────────────────────────────────────────────────────────────────────────┐
│                        Domain Layer (Pure Kotlin)                          │
│  Models: Story, Chapter, StoryHighlight, ReaderPreferences, AppAppearance, │
│          AccountSession, ReadingStatsSummary, WriterDraft                  │
│  Engines: ReaderTextPaginator, ReaderProgressMapper, InBookSearchEngine,   │
│           HighlightExporter, ComicZoomMath                                 │
│  Repositories: StoryRepository, HighlightRepository, AccountRepository,    │
│                ReadingStatsRepository, WriterDraftRepository,              │
│                ReaderPreferencesRepository, ServerConfigRepository         │
└───────────────────┬───────────────────────────────────────▲────────────────┘
                    │ Implemented By                        │ Emits Room Flows
                    ▼                                       │
┌────────────────────────────────────────────────────────────────────────────┐
│                         Data Layer (Repositories & I/O)                    │
│  RoomStoryRepository · RoomHighlightRepository · RemoteAccountRepository   │
│  RoomReadingStatsRepository · RoomWriterDraftRepository                    │
│  SharedPreferencesReaderPreferencesRepository · DebugServerConfigStore     │
└─────────┬─────────────────────────────────────────────────┬────────────────┘
          │ Local Persistence (SSOT)                        │ Remote REST JSON API
          ▼                                                 ▼
┌──────────────────────────────────────┐   ┌─────────────────────────────────┐
│   Room SQLite Database (Schema v6)   │   │  Shared Fable FastAPI Service   │
│  stories · chapters · shelf_states   │   │  FableApiService + MangaApi     │
│  reading_sessions · reading_stats    │   │  Shared OkHttp (FableHttpClient)│
│  writer_drafts · story_highlights    │   │  Fable · Gutenberg · Standard   │
│  Migrations 1->2->3->4->5->6         │   │  Ebooks · MangaDex · Auth/Sync  │
└──────────────────────────────────────┘   └─────────────────────────────────┘
```

---

## 2. Core Architectural Principles

1. **Strict Layer Isolation**:
   - **Presentation Layer (`ui/`)**: Jetpack Compose screens, modular reader sheets, typed navigation (`FableNavigation.kt`), and focused ViewModels. Never accesses Room DAOs, `SharedPreferences`, or HTTP clients directly.
   - **Domain Layer (`domain/`)**: Pure Kotlin models, repository interfaces, and deterministic reading/search/zoom calculation engines (`ReaderTextPaginator`, `ReaderProgressMapper`, `InBookSearchEngine`, `HighlightExporter`, `ComicZoomMath`). Free of Android UI or Room dependencies.
   - **Data Layer (`data/`)**: Implements domain repository interfaces, coordinates Room transactions (`withTransaction`), protects non-empty local chapters from summary payload truncation (`coalesceChapters`), and communicates with the shared Fable API via `FableHttpClient`.
2. **Decomposed ViewModels (No Monolithic God ViewModel)**:
   - Responsibilities are partitioned across `CatalogViewModel`, `ExploreViewModel`, `ReaderViewModel`, `StoryEditorViewModel`, `WriterDraftViewModel`, `AccountViewModel`, and `ServerConfigViewModel`.
3. **Cooperative Coroutine Cancellation & Structured Concurrency**:
   - All network requests execute through `FableHttpClient.executeCancellable()`, bridging Kotlin coroutine cancellation directly to `okhttp3.Call.cancel()`.
   - `RoomStoryRepository.refreshCatalog()` uses `supervisorScope` to refresh Fable Originals, Project Gutenberg, Standard Ebooks, and MangaDex in parallel while isolating upstream provider failures.
4. **Shared Backend Contract (ADR 009 & ADR 010)**:
   - Android does not scrape third-party HTML directly. All catalogs (`FABLE`, `GUTENBERG`, `STANDARD_EBOOKS`, `MANGADEX`), accounts, publishing, and shelf/stats synchronization flow through `/api/v1/*`.
