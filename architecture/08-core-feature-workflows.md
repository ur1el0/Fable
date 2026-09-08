# 08 Core Feature Workflows

## 1. Source Browsing & Library Ingestion Workflow

This workflow coordinates discovering novels on an external web source and saving them to the user's local Room database library.

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant UI as SourceCatalogScreen (Compose)
    participant VM as CatalogViewModel
    participant Extractor as SourceExtractor (OkHttp / Jsoup)
    participant Repo as BookRepository
    participant DB as Room SQLite Database

    User->>UI: Search novel query (e.g., "Shadow Slave")
    UI->>VM: onSearchQuerySubmitted("Shadow Slave")
    VM->>Extractor: searchBooks("Shadow Slave", page = 1)
    Extractor->>Extractor: Execute OkHttp GET & parse DOM via Jsoup
    Extractor-->>VM: Return List<Book> results
    VM-->>UI: Render search result cards
    
    User->>UI: Click "Add to Library" (Favorite)
    UI->>VM: onToggleFavorite(bookId)
    VM->>Repo: saveBookToLibrary(book)
    Repo->>DB: INSERT OR REPLACE INTO books (is_favorite = 1)
    DB-->>Repo: Confirm save
    Repo-->>VM: Flow emits updated book state
    VM-->>UI: Update button state to "In Library"
```

---

## 2. Background Chapter Download Workflow

This workflow coordinates queuing batch downloads, executing background work in WorkManager, writing files to Scoped Storage, and updating Room state.

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant UI as BookDetailScreen (Compose)
    participant VM as BookDetailViewModel
    participant WM as AndroidX WorkManager
    participant Worker as DownloadWorker
    participant Extractor as SourceExtractor
    participant Storage as App Scoped Storage
    participant DB as Room SQLite Database

    User->>UI: Click "Download Next 10 Chapters"
    UI->>VM: onDownloadChaptersRequested(count = 10)
    VM->>WM: Enqueue OneTimeWorkRequest (UniqueWork)
    VM->>DB: Update chapters set downloadState = 'QUEUED'
    
    Note over WM, Worker: WorkManager schedules worker on background thread
    WM->>Worker: doWork()
    Worker->>Worker: setForeground(notification with progress)
    
    loop For each queued chapter
        Worker->>Extractor: getChapterContent(chapterUrl)
        Extractor-->>Worker: Return raw HTML
        Worker->>Worker: Sanitize HTML (Jsoup Safelist)
        Worker->>Storage: Write to context.filesDir/chapters/{bookId}/{chapterId}.fbl
        Worker->>DB: UPDATE chapters SET downloadState = 'DOWNLOADED'
        Worker->>Worker: Update notification progress (e.g. 50%)
    end
    
    Worker-->>WM: Result.success()
    DB-->>UI: Flow emits updated chapter states (show downloaded badges)
```

---

## 3. Reading Session & Progress Checkpointing Workflow

This workflow manages opening a chapter from local storage, rendering the text, and debouncing reading progress saves to SQLite.

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant UI as ReaderScreen (Compose)
    participant VM as ReaderViewModel
    participant Storage as Scoped Storage
    participant DB as Room SQLite Database

    User->>UI: Tap book in Library
    UI->>VM: onOpenBook(bookId)
    VM->>DB: Query reading_history for bookId
    DB-->>VM: Return last chapterId & scrollOffset
    VM->>Storage: Read chapter text from .fbl file
    Storage-->>VM: Return chapter content string
    VM-->>UI: Render reader text & restore scroll position
    
    User->>UI: User scrolls through chapter
    UI->>VM: onScrollOffsetChanged(offset)
    Note over VM: 500ms Debounce Timer active
    VM->>DB: UPSERT INTO reading_history (chapterId, scrollOffset, timestamp)
    DB-->>VM: Confirm save
```

