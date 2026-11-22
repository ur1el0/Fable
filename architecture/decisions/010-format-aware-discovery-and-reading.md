# ADR 010: Format-Aware Discovery and Reading Mechanics

## Status

Accepted.

## Context

Fable supports two distinct reading mediums through the shared `/api/v1/` backend:

1. **Prose books and stories (`ContentFormat.PROSE`)** from Fable Originals (`FABLE`), Project Gutenberg (`GUTENBERG`), and Standard Ebooks (`STANDARD_EBOOKS`).
2. **Sequential art (`ContentFormat.COMIC`)** from MangaDex (`MANGADEX`).

Combining prose books and manga into an undifferentiated list or forcing both through a single reader layout degrades usability: prose readers expect typographic controls, paginated book mode, in-book search, and passage highlights, whereas manga and webtoon readers expect vertical strip or horizontal page-flip layouts, two-finger pinch-to-zoom, lazy image placeholders, and per-page retry controls.

## Decision

1. **Format-Aware Discovery (`ExploreScreen` & `ExploreViewModel`)**:
   - The Explore screen separates **Books** (`ContentFormat.PROSE`) from **Manga & Comics** (`ContentFormat.COMIC`) with dedicated format toggles and provider chips (`All Books`, `Fable`, `Gutenberg`, `Standard Ebooks` for prose; `MangaDex` for comics).
2. **Format-Aware Reader Dispatch (`StoryReaderScreen`)**:
   - `ContentFormat.PROSE` stories render via `ContinuousChapterList` (scroll mode) or `PaginatedChapterPager` (paginated book mode) with word-offset progress mapping (`ReaderProgressMapper`), cross-chapter keyword search (`InBookSearchEngine`), Room passage highlights (`story_highlights`), and 1080px PNG quote card export (`QuoteCardExporter`).
   - `ContentFormat.COMIC` stories render via `MangaChapterReader` supporting `ComicReaderLayout.WEBTOON` (vertical strip) and `ComicReaderLayout.MANGA` (horizontal pager), two-finger pinch-to-zoom (`1x..4x`), double-tap zoom (`1x <-> 2.5x`), bounded panning (`ComicZoomMath`), fixed-aspect loading placeholders, per-page error retry, and chapter transition banners.
3. **Shared Cancellable Transport & Parallel Catalog Sync**:
   - All network calls share `FableHttpClient` (`executeCancellable`) so cancelled search or reader coroutines immediately abort in-flight OkHttp calls.
   - `RoomStoryRepository.refreshCatalog()` fetches all four providers concurrently in a fault-isolated `supervisorScope`.

## Consequences

- Prose and comic features evolve cleanly without regressing each other's layout or gesture handling.
- Explicit offline chapter caching (`downloadStoryForOffline` / `removeStoryOfflineCopy`) supports both prose chapter text (`content`) and comic page descriptors (`pagesJson`).
