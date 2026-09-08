# 03 Domain & Data Layer Architecture

## 1. Clean Architecture Separation

Fable enforces a strict boundary between business logic (Domain) and infrastructure/persistence logic (Data).

```
┌─────────────────────────────────────────────────────────┐
│                      Domain Layer                       │
│  - Models: Book, Chapter, ReadingProgress, Category     │
│  - Use Cases: GetBookUseCase, DownloadChapterUseCase    │
│  - Contracts: BookRepository, ChapterRepository         │
│  - Pure Kotlin (NO android.* dependencies)              │
└────────────────────────────▲────────────────────────────┘
                             │ Implemented by
┌────────────────────────────┴────────────────────────────┐
│                       Data Layer                        │
│  - Implements Repository Interfaces                     │
│  - Local Data: Room DAOs & SQLite Database              │
│  - File System: App-specific Scoped Storage             │
│  - Remote Data: OkHttp Client & Jsoup Parsers           │
└─────────────────────────────────────────────────────────┘
```

---

## 2. Domain Models & Use Cases

### Domain Entities
- **`Book`**: Core literary work entity:
  ```kotlin
  data class Book(
      val id: String,
      val title: String,
      val author: String,
      val coverUrl: String?,
      val description: String?,
      val sourceId: String,
      val isFavorite: Boolean = false,
      val totalChapters: Int = 0,
      val createdAt: Long,
      val lastReadAt: Long?
  )
  ```
- **`Chapter`**: Individual installment entity:
  ```kotlin
  data class Chapter(
      val id: String,
      val bookId: String,
      val title: String,
      val chapterNumber: Float,
      val url: String,
      val isRead: Boolean = false,
      val isDownloaded: Boolean = false,
      val releaseDate: Long?
  )
  ```
- **`ReadingProgress`**: Position state:
  ```kotlin
  data class ReadingProgress(
      val bookId: String,
      val chapterId: String,
      val pageIndex: Int,
      val scrollOffset: Int,
      val lastReadTime: Long
  )
  ```

### Domain Use Cases
Each Use Case encapsulates a single, testable business requirement:
- `GetFavoriteBooksUseCase`: Emits `Flow<List<Book>>` of library books.
- `GetBookWithChaptersUseCase`: Fetches a book and its full chapter list from Room.
- `ToggleFavoriteUseCase`: Toggles a book's library bookmark state.
- `SaveReadingProgressUseCase`: Persists chapter ID, page index, and scroll offset.
- `DownloadChapterUseCase`: Schedules background download work in WorkManager.

---

## 3. Data Layer Implementation & Repository Pattern

### Repository Contracts & Implementations
1. **`BookRepository`**:
   - `observeFavorites(): Flow<List<Book>>`
   - `getBook(id: String): Flow<Book?>`
   - `saveBook(book: Book): suspend Unit`
   - `toggleFavorite(bookId: String): suspend Unit`
2. **`ChapterRepository`**:
   - `observeChapters(bookId: String): Flow<List<Chapter>>`
   - `getChapterContent(bookId: String, chapterId: String): suspend String`
   - `markChapterRead(chapterId: String, isRead: Boolean): suspend Unit`
3. **`SourceRepository`**:
   - `search(query: String, sourceId: String, page: Int): suspend List<Book>`
   - `fetchChapterList(bookUrl: String, sourceId: String): suspend List<Chapter>`

---

## 4. Concurrency & Coroutine Dispatchers

All operations follow structured concurrency:
- **`Dispatchers.IO`**: Exclusively used for Room database queries, file writing/reading in Scoped Storage, and OkHttp network calls.
- **`Dispatchers.Default`**: Used for CPU-heavy tasks such as Jsoup DOM tree traversal, text parsing, and pagination calculations.
- **`Dispatchers.Main.immediate`**: Used strictly for ViewModel state emissions to Jetpack Compose.

