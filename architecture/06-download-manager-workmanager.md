# 06 Background Pipeline & On-Demand Content Hydration

> [!IMPORTANT]
> **Status: DEFERRED per ADR 009**  
> The WorkManager bulk-download pipeline, foreground download notification service, and discrete file-based chapter storage (`context.filesDir/chapters/*.fbl`) are currently **deferred**. Fable operates under an **on-demand hydration model**: when a reader opens a story, missing chapters are retrieved from the shared FastAPI backend and persisted directly to the local Room database (`chapters` table). Room acts as the local cache and offline source of truth. The specification below is preserved for historical reference and future background sync evaluations.

---

## 1. Active Architecture: On-Demand Chapter Hydration

Instead of background queueing and bulk downloads:
1. **Cache-First Check:** When `StoryReaderScreen` opens, `RoomStoryRepository.getChapters(storyId)` observes Room. If chapters already reside in the local database, the reader mounts immediately without network latency.
2. **On-Demand Fetch:** If the story requires hydration (e.g. initial read of a Standard Ebooks or Gutenberg title), `RoomStoryRepository.fetchAndCacheChapters(storyId)` requests `/api/v1/public/{provider}/{id}/chapters`.
3. **Atomic Room Upsert:** Retrieved chapters are inserted into Room in a `@Transaction`. Once written to SQLite, they remain 100% available offline for future reading sessions.
4. **Offline Resilience:** If the user opens an already-read or previously loaded book without network connectivity, Room serves the complete chapter text from SQLite.

---

## 2. Deferred Specification: Background Bulk-Download Pipeline (Reference Only)

The architecture below documents the original bulk-download design using **AndroidX WorkManager** if background offline pre-fetching is reintroduced.

```
┌────────────────────────────────────────────────────────┐
│                        User UI                         │
│  "Download Next 10 Chapters" or "Download All Unread"  │
└───────────────────────────┬────────────────────────────┘
                            │ Enqueues Download Work
                            ▼
┌────────────────────────────────────────────────────────┐
│                  WorkManager Enqueue                   │
│  - Unique Work Name: "fable_download_queue"            │
│  - Policy: ExistingWorkPolicy.APPEND_OR_REPLACE        │
│  - Constraints: Network Connected, Battery Not Low     │
└───────────────────────────┬────────────────────────────┘
                            │ Dispatches
                            ▼
┌────────────────────────────────────────────────────────┐
│               DownloadWorker (CoroutineWorker)         │
│  - Runs on Dispatchers.IO                              │
│  - Posts Foreground Notification (Android 14+ DataSync)│
│  - Fetches HTML via Source Extractor                   │
│  - Sanitizes content via Jsoup                         │
│  - Writes to Scoped Storage: context.filesDir/chapters/│
│  - Updates ChapterEntity: downloadState = DOWNLOADED   │
└────────────────────────────────────────────────────────┘
```

### 2.1 Android 14+ Foreground Service Integration
Batch downloads are long-running operations. To ensure Android does not terminate the background process when the user navigates away, `DownloadWorker` was planned to promote itself to a Foreground Service with an explicit service type:

#### Manifest Configuration (`AndroidManifest.xml`)
```xml
<service
    android:name="androidx.work.impl.foreground.SystemForegroundService"
    android:foregroundServiceType="dataSync"
    tools:node="merge" />
```

#### Worker Implementation Pattern (`DownloadWorker.kt`)
```kotlin
override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
    val chapterId = inputData.getString("chapter_id") ?: return@withContext Result.failure()
    val bookId = inputData.getString("book_id") ?: return@withContext Result.failure()

    // 1. Establish Foreground Notification
    val foregroundInfo = createForegroundInfo(currentProgress = 0)
    setForeground(foregroundInfo)

    // 2. Fetch and sanitize chapter
    val chapterContent = sourceRepository.fetchChapterContent(chapterUrl)
    val sanitizedContent = HtmlSanitizer.clean(chapterContent)

    // 3. Write to Scoped Storage sandbox
    val file = storageManager.saveChapterFile(bookId, chapterId, sanitizedContent)

    // 4. Update Room Database state
    chapterDao.updateDownloadState(chapterId, DownloadState.DOWNLOADED, file.absolutePath)

    Result.success()
}
```

### 2.2 Scoped Storage & Path Traversal Guards
All chapter files would be written strictly to app-specific internal storage (`context.filesDir`), ensuring complete data sandboxing:

```kotlin
fun saveChapterFile(bookId: String, chapterId: String, content: String): File {
    val bookDir = File(context.filesDir, "chapters/${sanitizeFilename(bookId)}")
    if (!bookDir.exists()) bookDir.mkdirs()

    val chapterFile = File(bookDir, "${sanitizeFilename(chapterId)}.fbl")
    
    // Canonical path verification against path traversal attacks
    if (!chapterFile.canonicalPath.startsWith(bookDir.canonicalPath)) {
        throw SecurityException("Path traversal attempt in chapter ID: $chapterId")
    }
    
    chapterFile.writeText(content, Charsets.UTF_8)
    return chapterFile
}
```

