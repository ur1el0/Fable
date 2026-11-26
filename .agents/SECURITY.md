# Fable Android — Data Privacy & Security Policy

This document defines the data protection protocols, transport security baselines, and architectural safety boundaries enforced across Fable Android.

---

## 1. Local-First Privacy & Zero Third-Party Telemetry

- **Zero Third-Party Analytics or Ad SDKs**: Fable Android contains no analytics, advertising, or third-party telemetry SDKs.
- **Account-Optional Reading**: Users can browse catalogs, read prose and manga, cache chapters offline, search inside books, save passage highlights, export quote cards, and draft manuscripts locally without creating an account (`__guest__` owner scope in Room).
- **Full Account & Data Erasure (`DELETE /api/v1/auth/me`)**: Signed-in users can permanently delete their account from **Account → Delete account**, which deletes their server account and purges local auth credentials, owned stories, shelf states, and reading stats from Room.

---

## 2. Transport Security & URL Validation

- **No Cleartext Traffic in Release**: `AndroidManifest.xml` sets `android:usesCleartextTraffic="false"`. Cleartext HTTP is permitted only in debug builds via `app/src/debug/res/xml/network_security_config.xml` for local development (`10.0.2.2`, `localhost`, LAN).
- **Compile-Time Release Base URL Gate**: The `:app:validateReleaseApiBaseUrl` Gradle task rejects release builds unless `FABLE_API_BASE_URL` is a valid `https://` URL ending with `/` and not pointing to a loopback or private host.
- **Strict Runtime URL Validation (`ServerUrlValidator`, `MangaApiService`)**:
  - `ServerUrlValidator` blocks embedded credentials (`user:pass@host`), non-HTTP(S) schemes, and malformed hosts.
  - `MangaApiService.sanitizeHttpUrl` validates every cover and page URL with `toHttpUrlOrNull()` before rendering in Coil.
- **Automatic `401 Unauthorized` Recovery**: Expired or revoked bearer tokens are immediately cleared by `RemoteAccountRepository` on `401` responses so stale credentials are never retained.

---

## 3. Room SQLite Integrity & Scoped File Sharing

- **Parameterized SQL Queries**: All queries in `StoryDao`, `ReadingStatsDao`, `WriterDraftDao`, and `HighlightDao` use Room's compile-time verified bind parameters (`:storyId`, `:ownerId`).
- **Scoped Storage & `FileProvider` Sharing**:
  - Quote card PNG images (`QuoteCardExporter`) are rendered into `context.cacheDir/quote_cards/` and shared via `androidx.core.content.FileProvider` (`res/xml/file_paths.xml`) with temporary read permissions (`FLAG_GRANT_READ_URI_PERMISSION`).
  - Fable Android never requests `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, or `MANAGE_EXTERNAL_STORAGE`.

---

## 4. Release Binary Hardening

- Release builds enable R8 minification (`isMinifyEnabled = true`) and resource shrinking (`isShrinkResources = true`) and strip `Log.d` / `Log.v` calls via `app/src/main/keepRules/rules.keep`.
