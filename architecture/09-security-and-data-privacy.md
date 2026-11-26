# 09 Security & Data Privacy Architecture

## 1. Local-First Privacy & Zero Third-Party Telemetry

- **No Analytics or Ad SDKs**: Fable Android includes zero third-party analytics, crash-tracking, or advertising SDKs.
- **Account-Optional Reading**: Discovery, reading, offline chapter caching, in-book search, passage highlights, quote cards, and local writer drafts work without signing into an account (`__guest__` owner scope in Room).
- **Complete Account Deletion (`DELETE /api/v1/auth/me`)**: Authenticated users can permanently delete their account from **Account → Delete account**. Upon server confirmation, `AccountViewModel` and `RemoteAccountRepository` purge local auth tokens, user-owned stories (`StoryDao.purgeAccountData`), account shelf states, and reading stats (`ReadingStatsDao.clearOwnerData`).

---

## 2. Transport Security & URL Validation

- **Release HTTPS Enforcement**:
  - `AndroidManifest.xml` disables cleartext traffic in release builds, while `app/src/debug/res/xml/network_security_config.xml` permits local development traffic (`10.0.2.2`, `localhost`, LAN) strictly in debug builds.
  - The Gradle task `:app:validateReleaseApiBaseUrl` fails any release build whose `FABLE_API_BASE_URL` does not start with `https://`, end with `/`, or points to a loopback/private address.
- **Strict Server & Asset URL Validation (`ServerUrlValidator`, `MangaApiService`)**:
  - `ServerUrlValidator` blocks embedded URL credentials (`user:pass@host`), non-HTTP(S) schemes, and invalid hosts.
  - `MangaApiService.sanitizeHttpUrl` validates every remote cover and comic page URL via `toHttpUrlOrNull()`, rejecting malformed or non-HTTP(S) schemes before they reach Coil.
- **Automatic `401 Unauthorized` Session Reset**:
  - If an authenticated request receives `401 Unauthorized`, `RemoteAccountRepository` clears stale persisted credentials and transitions `sessionState` to `SignedOut` so the UI never remains stuck in a broken authenticated state.

---

## 3. SQLite / Room Injection Safety & Scoped File Sharing

- **Parameterized Room Queries**: All database queries in `StoryDao`, `ReadingStatsDao`, `WriterDraftDao`, and `HighlightDao` use Room compile-time verified parameterized SQL statements (`:storyId`, `:ownerId`).
- **Scoped Quote Card Sharing (`FileProvider`)**:
  - Generated PNG quote cards (`QuoteCardExporter`) are written exclusively to the app's private cache directory (`context.cacheDir/quote_cards/`) and shared with external apps via `androidx.core.content.FileProvider` (`res/xml/file_paths.xml`) with temporary `FLAG_GRANT_READ_URI_PERMISSION`.
  - Fable Android never requests `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, or `MANAGE_EXTERNAL_STORAGE`.
