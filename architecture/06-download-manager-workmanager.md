# 06 Download Manager & Background Pipeline (WorkManager)

## 1. Background Download Architecture

Fable offloads chapter downloading to **AndroidX WorkManager**. WorkManager provides guaranteed task execution, survives app process termination and device reboots, and respects system constraints (battery and unmetered Wi-Fi).

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

---

## 2. Android 14+ Foreground Service Integration

Batch downloads are long-running operations. To ensure Android does not terminate the background process when the user navigates away, `DownloadWorker` promotes itself to a Foreground Service with an explicit service type:

### Manifest Configuration (`AndroidManifest.xml`)
```xml
<service
    android:name="androidx.work.impl.foreground.SystemForegroundService"
    android:foregroundServiceType="dataSync"
    tools:node="merge" />
```

### Worker Implementation (`DownloadWorker.kt`)
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

---

## 3. Scoped Storage & Path Traversal Guards

All chapter files are written strictly to app-specific internal storage (`context.filesDir`), ensuring complete data sandboxing:

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

