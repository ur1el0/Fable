---
trigger: always_on
---

# Reader Engine & Shared API Content Guidelines

These rules govern the prose and comic reader engines, in-book search, passage highlights, and shared `/api/v1/` content hydration in Fable Android.

---

### 1. Shared API Content Hydration (No Client-Side HTML Scraping)
- **Rule**: All story, book, and comic catalogs (`FABLE`, `GUTENBERG`, `STANDARD_EBOOKS`, `MANGADEX`) are fetched through `FableApiService` and `MangaApiService` using `FableHttpClient.executeCancellable()`.
- **Practice**:
  - Never write ad-hoc network calls inside UI components or ViewModels.
  - Validate external image/cover URLs via `MangaApiService.sanitizeHttpUrl` and API base URLs via `ServerUrlValidator`.
  - When refreshing catalogs (`RoomStoryRepository.refreshCatalog`), always preserve non-empty cached chapter content and comic page lists via `coalesceChapters`.

---

### 2. Format-Aware Reader Rendering
- **Rule**: Dispatch reader layouts by `story.contentFormat`.
- **Practice**:
  - **`ContentFormat.PROSE`**: Render via `ContinuousChapterList` (`ReaderLayout.SCROLL`) or `PaginatedChapterPager` (`ReaderLayout.PAGINATED`) backed by `ReaderTextPaginator`.
  - **`ContentFormat.COMIC`**: Render via `MangaChapterReader` (`ComicReaderLayout.WEBTOON` or `MANGA`) with two-finger pinch-to-zoom (`1x..4x`), double-tap zoom (`1x <-> 2.5x`), bounded panning (`ComicZoomMath`), fixed-aspect loading placeholders, and per-page retry controls.

---

### 3. Word-Offset Progress Persistence
- **Rule**: Reading progress must be preserved across font size, line height, and scroll/paginated layout switches without blocking the UI thread.
- **Practice**:
  - Use `ReaderProgressMapper` to map between normalized chapter progress (`0f..1f`), scroll offsets, paginated page indices, and chapter word offsets (`firstWordIndex`).
  - Debounce progress saves in `ReaderViewModel` (`350ms`) before writing to `shelf_states` in Room.

---

### 4. In-Book Search, Passage Highlights & Quote Cards
- **Rule**: Passage highlights and notes must persist in Room (`story_highlights`, schema v6) and support exact word ranges.
- **Practice**:
  - Use `InBookSearchEngine` for word tokenization (`tokenizeWords`), word-range slicing (`sliceWordRange`), and cross-chapter keyword search (`searchStory`).
  - Render inline highlights via `buildHighlightedParagraph` and export 1080px PNG quote cards via `QuoteCardExporter` into `context.cacheDir/quote_cards/` shared through `FileProvider`.
