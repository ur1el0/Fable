# 08 Core Feature Workflows

## 1. Multi-Source Discovery & Parallel Catalog Refresh

1. **Startup & Pull-to-Refresh (`CatalogViewModel.refreshCatalog`)**:
   - Calls `RoomStoryRepository.refreshCatalog()`, which uses `supervisorScope` to fetch Fable Originals, Project Gutenberg, Standard Ebooks, and MangaDex discovery feeds in parallel.
   - Each provider's results are upserted into Room while `coalesceChapters` preserves any previously downloaded chapter bodies or comic page descriptors.
   - If an external provider (e.g., Gutenberg, Standard Ebooks, or MangaDex) fails or times out, Fable Originals and cached Room stories still update and render without failing the entire refresh.
2. **Format-Aware Explore Search (`ExploreViewModel`)**:
   - Switching between **Books** (`ContentFormat.PROSE`) and **Manga & Comics** (`ContentFormat.COMIC`) filters the local catalog immediately and scopes remote search queries to the selected provider (`All Books`, `Fable`, `Gutenberg`, `Standard Ebooks`, or `MangaDex`).
   - Rapid query edits cancel the previous coroutine job, which cancels the underlying in-flight OkHttp call via `FableHttpClient.executeCancellable()`.

---

## 2. Reading, Offline Download & Session Sync Workflow

1. **Opening a Story (`StoryDetailScreen` $\rightarrow$ `StoryReaderScreen`)**:
   - If the story's chapters are already cached in Room (`storyId in downloadedStoryIds`), the reader renders immediately offline.
   - Otherwise, `ReaderViewModel` triggers `RoomStoryRepository.refreshStory(storyId)` (and `fetchMangaChapterPages` for comic chapters) and persists the hydrated chapters to Room.
2. **Explicit Offline Management (`OfflineCacheCard`)**:
   - Tapping **Download for offline** downloads all chapters for the story into Room and marks the story with an `"Offline"` badge across Library, Explore, and Shelf (`Offline` filter tab).
   - Tapping **Remove offline copy** clears the cached chapter bodies (`StoryDao.clearOfflineCache`) while preserving story metadata and shelf progress.
3. **Progress & Reading Session Recording**:
   - Scroll/page changes are debounced (`350ms`) and written to `shelf_states` (`pendingSync = true` when offline).
   - Exiting the reader or backgrounding the app records a `ReadingSessionRecord` (`durationSeconds >= 5`, `wordsRead`) into `reading_sessions` and `reading_stats`, then attempts background sync with `/api/v1/me/reading-sessions`.

---

## 3. Author Manuscript, Publishing & Story Edit/Delete Workflow

1. **Draft Auto-Save (`WriteScreen` + `WriterDraftViewModel`)**:
   - Edits to title, genre, synopsis, chapter title, or manuscript body are debounced and persisted to `writer_drafts` in Room so work is never lost on process death.
2. **Publishing (`StoryEditorViewModel.publishStory`)**:
   - Validates required fields and minimum manuscript length, parses `Chapter N:` markers into discrete chapters, and sends `POST /api/v1/stories`.
   - Inserts the newly created `Story` (with `ownerUserId = currentUser.id`) into Room and clears the local draft.
3. **Editing & Deleting Owned Stories (`EditStoryScreen`, `StoryDetailScreen`, `AccountScreen`)**:
   - Only stories where `story.provider == StoryProvider.FABLE` and `story.ownerUserId == currentUser.id` expose **Edit** and **Delete** controls.
   - Before editing, `StoryEditorViewModel.loadStoryForEdit(storyId)` calls `storyRepository.refreshStory(storyId)` if local chapter bodies are blank so summary responses never overwrite full manuscripts.
   - Saving calls `PATCH /api/v1/stories/{id}` (`UpdateStoryRequestDto`) and updates Room; deleting calls `DELETE /api/v1/stories/{id}` and removes the story and its shelf state from Room.
