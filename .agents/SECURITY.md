# Fable - Data Privacy & Security Policy

This document defines the mandatory data protection protocols, security baselines, and architectural safety boundaries enforced across Fable.

---

## 1. Offline Data Sovereignty & Zero-Telemetry Policy

Fable is designed with a privacy-first, local-first philosophy:
- **Zero Third-Party SDKs**: No analytics platforms (Google Analytics, Firebase Analytics, Mixpanel) or crash-reporting libraries that transmit personal reading data to remote servers.
- **Local Reading Identity**: All library entries, reading histories, custom category tags, and bookmarks reside solely within the device's local SQLite database.
- **No Account Requirement**: Version 1 functions completely without mandatory cloud registration, user accounts, or remote authentication servers.

---

## 2. Scoped Storage & File System Isolation

To prevent unauthorized cross-application data leaks and malicious file writes:
1. **App-Specific Internal Sandboxing**: All downloaded chapter texts, scraped HTML files, and cached cover images must be saved strictly within `context.filesDir` or `context.cacheDir`.
2. **Path Traversal Defense**: External sources can provide arbitrary book titles or chapter names. Before writing any file to disk, the path must be sanitized:
   ```kotlin
   fun getSafeChapterFile(context: Context, bookId: String, chapterId: String): File {
       val safeBookDir = File(context.filesDir, "chapters/${bookId.filter { it.isLetterOrDigit() || it == '_' }}")
       if (!safeBookDir.exists()) safeBookDir.mkdirs()
       
       val targetFile = File(safeBookDir, "${chapterId.filter { it.isLetterOrDigit() || it == '_' }}.fbl")
       // Verify target file is still within safeBookDir (prevents ../../ attacks)
       if (!targetFile.canonicalPath.startsWith(safeBookDir.canonicalPath)) {
           throw SecurityException("Path traversal attempt detected in chapter ID: $chapterId")
       }
       return targetFile
   }
   ```

---

## 3. Scraped Content Sanitization (Script Injection Defense)

External web novel websites may contain malicious scripts, advertisements, or hidden tracking pixels.
- **Mandatory Jsoup Sanitization**: Raw HTML fetched by source extractors must be passed through a strict `Safelist` before storage or rendering:
  - Allowed tags: Paragraphs (`<p>`), headings (`<h1>`-`<h6>`), breaks (`<br>`), basic styling (`<b>`, `<i>`, `<em>`, `<strong>`).
  - Blocked tags: Strictly strip all `<script>`, `<style>`, `<iframe>`, `<embed>`, `<object>`, and `<form>` tags.
  - Attribute stripping: Remove all inline event handlers (`onload`, `onerror`, `onclick`, `onmouseover`).
  - Image handling: Images must either be stripped or routed strictly through the trusted Coil image pipeline.

---

## 4. Network Security & TLS Enforcement

- **No Cleartext Traffic**: `AndroidManifest.xml` must configure `android:usesCleartextTraffic="false"` to prevent unencrypted HTTP connections.
- **TLS 1.3 / 1.2 Enforcement**: The OkHttp client must use modern TLS cipher suites.
- **Responsible Scraping & Rate Limiting**: The networking layer enforces exponential backoff and request throttling (minimum 500ms delay between consecutive requests) to avoid triggering source firewalls or Denial-of-Service (DoS) countermeasures.

---

## 5. Room Database & Local Injection Security

- **Parameterized Queries**: All SQLite interactions must be managed through Room's generated prepared statements (`@Query("SELECT * FROM books WHERE id = :bookId")`). Never concatenate raw strings inside raw SQL queries.
- **Sensitive Key Storage**: Any future encryption keys or user preferences must be managed using AndroidX DataStore combined with Android Keystore.

---

## 6. Android Permissions & Background Services

- **Least Privilege Principle**: Fable requests only the minimum required Android permissions:
  - `android.permission.INTERNET`: Required for source scraping and downloading chapters.
  - `android.permission.ACCESS_NETWORK_STATE`: Required by WorkManager to evaluate network constraints (e.g., Unmetered/Wi-Fi only).
  - `android.permission.POST_NOTIFICATIONS`: Requested at runtime on Android 13+ (API 33+) only when the user initiates a background download.
  - `android.permission.FOREGROUND_SERVICE_DATA_SYNC`: Declared for active chapter download services on Android 14+ (API 34+).
- **No Dangerous Storage Permissions**: The app must NEVER request `READ_EXTERNAL_STORAGE` or `MANAGE_EXTERNAL_STORAGE`.

---

## 7. Build Hygiene & Binary Hardening

- **Log Sanitization**: In release builds (`buildTypes { release { ... } }`), all debug log statements (`Log.d`, `Log.v`) must be removed via ProGuard/R8 rules:
  ```proguard
  -assumenosideeffects class android.util.Log {
      public static *** d(...);
      public static *** v(...);
  }
  ```
- **R8 Code Obfuscation**: Release builds must enable code shrinking and resource optimization to reduce APK attack surfaces.

