# Fable - Troubleshooting Runbook

This document details common failure modes, diagnostic steps, and established resolutions across the Fable Android architecture.

---

## 1. Jetpack Compose Recomposition Loops & UI Jank

### Symptoms
- Reader scroll stutters; dropped frames (>16ms); CPU usage spikes during passive scrolling.
- UI elements re-render continuously even when no state changes occur.

### Root Causes
- Passing unstable parameters (e.g., standard `List<T>` or unstably typed data classes) into Composables.
- Inlining lambda expressions that capture unstable variables or creating new object instances directly inside Composable functions without `remember`.

### Diagnostics
- Enable Compose Compiler Metrics by adding `-Pandroidx.compose.compiler.plugins.kotlin.metricsDestination` to Gradle.
- Run Android Studio Layout Inspector and check the "Recomposition Count" column.

### Resolution
1. Wrap collections in `@Immutable` wrapper classes or use kotlinx-collections-immutable.
2. Annotate domain models with `@Stable` or `@Immutable`.
3. Use `derivedStateOf` when calculating scroll visibility or progress percentage:
   ```kotlin
   val isScrolledPastThreshold by remember {
       derivedStateOf { listState.firstVisibleItemIndex > 0 }
   }
   ```
4. Always specify a unique `key = { it.id }` in `items()` inside `LazyColumn` or `HorizontalPager`.

---

## 2. Room Database Migration Failures

### Symptoms
- Crash on application startup:
  `java.lang.IllegalStateException: Room cannot verify the data integrity. Looks like you've changed schema but forgot to update the version number.`

### Root Cause
- An `@Entity` field was added, removed, or changed without incrementing `@Database(version = N)` and supplying an appropriate `Migration` path.

### Resolution
1. Increment database version: `@Database(entities = [...], version = 2)`.
2. Define a clean SQL migration:
   ```kotlin
   val MIGRATION_1_2 = object : Migration(1, 2) {
       override fun migrate(db: SupportSQLiteDatabase) {
           db.execSQL("ALTER TABLE chapters ADD COLUMN downloadState TEXT NOT NULL DEFAULT 'NOT_DOWNLOADED'")
       }
   }
   ```
3. Add the migration to the database builder: `.addMigrations(MIGRATION_1_2)`.
4. *During early local prototyping only:* If schema preservation is not required, temporary fallback can be set using `.fallbackToDestructiveMigration()`.

---

## 3. WorkManager Android 14+ Foreground Service Crash

### Symptoms
- Crash when initiating background chapter download:
  `android.app.MissingForegroundServiceTypeException: Starting FGS without a type is not supported`

### Root Cause
- Android 14 (API 34) mandates that any foreground service must declare an explicit `foregroundServiceType` in both the manifest and the runtime `ForegroundInfo`.

### Resolution
1. In `AndroidManifest.xml`, declare the type:
   ```xml
   <service
       android:name="androidx.work.impl.foreground.SystemForegroundService"
       android:foregroundServiceType="dataSync"
       tools:node="merge" />
   ```
2. In `DownloadWorker.kt`, specify the type in `ForegroundInfo`:
   ```kotlin
   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
       ForegroundInfo(
           NOTIFICATION_ID,
           notification,
           ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
       )
   } else {
       ForegroundInfo(NOTIFICATION_ID, notification)
   }
   ```

---

## 4. Out of Memory (OOM) via Coil Cover Art or Large Chapters

### Symptoms
- Crash: `java.lang.OutOfMemoryError: Failed to allocate a allocation with ... free bytes and ... until OOM`.

### Root Cause
- Uncapped bitmap caching or loading full-resolution external book cover images into memory without downsampling.

### Resolution
1. Configure Coil's `ImageLoader` with explicit memory cache limits (20-25% of available heap):
   ```kotlin
   ImageLoader.Builder(context)
       .memoryCache {
           MemoryCache.Builder(context)
               .maxSizePercent(0.20)
               .build()
       }
       .diskCache {
           DiskCache.Builder()
               .directory(context.cacheDir.resolve("image_cache"))
               .maxSizeBytes(100L * 1024 * 1024) // 100 MB
               .build()
       }
       .crossfade(true)
       .build()
   ```
2. In Compose `AsyncImage`, always constrain the size:
   ```kotlin
   AsyncImage(
       model = book.coverUrl,
       contentDescription = null,
       modifier = Modifier.size(width = 120.dp, height = 180.dp),
       contentScale = ContentScale.Crop
   )
   ```

---

## 5. Jsoup Web Scraping 403 Forbidden / Anti-Bot Errors

### Symptoms
- Source extractor fails with `org.jsoup.HttpStatusException: HTTP error fetching URL. Status=403`.

### Root Cause
- Target novel hosting website blocks generic HTTP requests that lack standard browser request headers or originate from known bot User-Agents.

### Resolution
1. Configure OkHttp with realistic browser headers:
   ```kotlin
   val request = Request.Builder()
       .url(sourceUrl)
       .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
       .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
       .header("Accept-Language", "en-US,en;q=0.9")
       .build()
   ```
2. Enforce a minimum 500ms delay between consecutive requests in source crawlers.

---

## 6. KSP / Room Stale Generation Failure

### Symptoms
- Unresolved reference to `FableDatabase_Impl` or generated DAOs after changing entity classes.

### Resolution
Stop the Gradle daemon and perform a clean rebuild to clear stale annotation processor caches:
```bash
./gradlew --stop
./gradlew clean assembleDebug
```

