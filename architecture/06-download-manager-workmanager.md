# 06 Offline Chapter Caching & Download Management

> **Architecture Note (ADR 009):** Earlier prototype documents proposed a WorkManager HTML-scraping queue writing `.fbl` files to `context.filesDir`. Under the shared Fable product architecture (ADR 009 & ADR 010), chapter content is fetched as structured JSON from the shared Fable API and persisted directly in **Room (`chapters` table in `FableDatabase` v6)**.

---

## 1. On-Demand & Explicit Offline Caching Pipeline

Fable Android supports two complementary offline caching mechanisms backed by `RoomStoryRepository`:

1. **Automatic On-Demand Chapter Caching**:
   - Opening a story in `StoryReaderScreen` triggers `RoomStoryRepository.refreshStory(storyId)` (and `fetchMangaChapterPages` for `ContentFormat.COMIC`).
   - Fetched prose chapter bodies (`ChapterEntity.content`) and comic page descriptors (`ChapterEntity.pagesJson`) are saved in Room inside a transaction.
   - Reopening the story offline renders immediately from Room.

2. **Explicit One-Tap Story Offline Download (`downloadStoryForOffline`)**:
   - From `StoryDetailScreen` (`OfflineCacheCard`), the user can tap **Download for offline**.
   - `CatalogViewModel.downloadStoryForOffline(storyId)` invokes `RoomStoryRepository.downloadStoryForOffline(storyId)`:
     - For **Prose** stories (`FABLE`, `GUTENBERG`, `STANDARD_EBOOKS`), it hydrates all chapters from the provider's chapter endpoint and verifies that non-blank chapter content is stored in Room.
     - For **Comic** stories (`MANGADEX`), it refreshes the chapter list and hydrates the page descriptor lists for the story's chapters.
   - Once cached, `StoryDao.observeDownloadedStoryIds()` reactively emits the `storyId`, displaying an `"Offline"` badge on `StoryRow`, `LibraryScreen`, `ExploreScreen`, `ShelfScreen` (including the **Offline** shelf filter tab), and `StoryDetailScreen`.

---

## 2. Offline Cache Removal (`removeStoryOfflineCopy`)

Users can reclaim local SQLite space without losing their reading progress or shelf bookmarks:

- Tapping **Remove offline copy** in `StoryDetailScreen` calls `CatalogViewModel.removeStoryOfflineCopy(storyId)` $\rightarrow$ `RoomStoryRepository.removeStoryOfflineCopy(storyId)`.
- `StoryDao.clearOfflineCache(storyId)` clears `content = ''` and `pagesJson = '[]'` on the story's `chapters` rows inside a transaction while preserving the `stories` metadata row and `shelf_states` progress.
