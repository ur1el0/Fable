# ADR 008: Scoped Storage Sandboxing & Chapter Persistence

## Status
Accepted

## Context
Downloaded chapter contents (sanitized HTML, plain text, and metadata) must be stored persistently on the user's device for guaranteed offline reading. Storing files in external public directories (e.g., `/sdcard/Download` or public root storage) requires invasive storage permissions (`READ_EXTERNAL_STORAGE` or `MANAGE_EXTERNAL_STORAGE`), exposes private reading history to other installed applications, and violates modern Android Scoped Storage baselines (API 30+).

## Decision
We chose **Internal Scoped Storage Sandboxing** as the persistence mechanism for all downloaded chapters:
- All chapter files are saved strictly within app-internal private storage: `context.filesDir/chapters/{bookId}/{chapterId}.fbl`.
- Path traversal verification (`targetFile.canonicalPath.startsWith(baseDir.canonicalPath)`) is strictly enforced before every file write to eliminate directory-climbing exploits (`../../`).
- The application requests **zero external storage permissions** in `AndroidManifest.xml`.

## Consequences
- **Positive:** Maximum privacy and security; downloaded reading material is sandboxed and completely inaccessible to other third-party apps on the device.
- **Positive:** Complies 100% with Google Play Scoped Storage policies on Android 11 through Android 16 (API 30 to 36).
- **Positive:** Clean lifecycle management; files are automatically purged by the Android OS if the user uninstalls the application.
- **Negative:** Users cannot directly access raw `.fbl` chapter files through generic third-party file manager apps without an explicit in-app export feature.

