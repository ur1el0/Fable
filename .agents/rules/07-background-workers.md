---
trigger: always_on
---

# Background Execution & WorkManager Guidelines

These rules govern background task execution, chapter downloading pipelines, and WorkManager lifecycle management in Fable.

---

### 1. CoroutineWorker on Background Dispatchers
- **Rule**: All background workers must extend `androidx.work.CoroutineWorker`.
- **Practice**:
  - Implement suspend `doWork()` and ensure heavy file I/O and network operations execute on `withContext(Dispatchers.IO)`.
  - Handle cooperative coroutine cancellation by checking `isStopped` during long iteration loops.

---

### 2. Explicit Constraint Configuration
- **Rule**: Background downloads must declare system constraints to protect user data and battery.
- **Practice**:
  - Require active network connectivity:
    ```kotlin
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()
    ```
  - Provide an optional user setting to restrict downloads to unmetered Wi-Fi connections (`NetworkType.UNMETERED`).

---

### 3. Android 14+ Foreground Service Standards
- **Rule**: Long-running batch downloads must declare a foreground service with explicit type.
- **Practice**:
  - On Android 14 (API 34+), pass `ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC` when calling `setForeground(foregroundInfo)`.
  - In `AndroidManifest.xml`, declare `<service android:name="androidx.work.impl.foreground.SystemForegroundService" android:foregroundServiceType="dataSync" tools:node="merge" />`.
  - Request the `POST_NOTIFICATIONS` runtime permission on Android 13+ (API 33+) before launching foreground workers.

---

### 4. Work Request Idempotency & Queue Management
- **Rule**: Prevent duplicate download workers from running concurrently for the same chapter.
- **Practice**:
  - Enqueue chapter downloads with unique work names:
    ```kotlin
    workManager.enqueueUniqueWork(
        "download_chapter_${chapter.id}",
        ExistingWorkPolicy.KEEP,
        downloadWorkRequest
    )
    ```
  - Use `ExistingWorkPolicy.KEEP` to ignore duplicate requests if a download is already actively running or queued.

---

### 5. Foreground Notification Hygiene
- **Rule**: Notifications must provide real-time feedback and allow cancellation.
- **Practice**:
  - Display current progress (e.g., "Downloading Chapter 12 of 50 (24%)").
  - Attach a user action button to cancel the ongoing download task cleanly.
  - Automatically dismiss the notification upon download completion or error.

