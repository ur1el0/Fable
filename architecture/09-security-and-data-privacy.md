# 09 Security & Data Privacy Architecture

## 1. Threat Modeling for an Offline-First Connected Client

Fable Android processes untrusted external public-domain content, communicates with a shared backend REST API, and stores private user reading state and authentication tokens locally. The primary threat vectors are:

1. **Insecure Transport & Cleartext Leakage:** Network snooping or man-in-the-middle attacks intercepting bearer tokens or reading data on public networks.
2. **Release Build Configuration Drift:** Debug local API URLs (`10.0.2.2`, `http://localhost`) accidentally leaking into production release builds.
3. **Cross-Tenant Data Contamination:** Guest reading history, bookmarks, or progress leaking into authenticated user accounts (or vice versa) across sign-in/sign-out cycles.
4. **Credential Leakage in Backups:** Plaintext API session tokens or device identities being uploaded to unencrypted cloud backups (Android Auto Backup).
5. **Malicious HTML / Script Injection:** Untrusted chapter content containing hidden script or DOM manipulation payloads.

---

## 2. Security Defense Implementations

### 2.1 Release API URL Guard
Release builds strictly forbid cleartext HTTP URLs and localhost addresses. The build configuration enforces that `fableReleaseApiBaseUrl` must use `https://` and terminate in `/api/v1/`:
```kotlin
if (!isUrlValidForRelease(url)) {
    throw IllegalStateException("Release builds must configure an HTTPS URL ending in /api/v1/")
}
```

### 2.2 Network Security Configuration & Cleartext Policy
In `res/xml/network_security_config.xml`:
- Production release builds enforce `cleartextTrafficPermitted="false"`.
- Local debug builds explicitly permit the Android emulator host bridge loopback (`10.0.2.2`) exclusively on debug configurations.

### 2.3 Auto-Backup Exclusions (`backup_rules.xml`)
Android Auto Backup is restricted to prevent sensitive session state and device tokens from being exported to Google Drive:
- `data/local/secure_session_store` is excluded from backup.
- `FableDatabase` SQLite files (`*.db`, `*.db-wal`, `*.db-shm`) are excluded from auto-backup to preserve account isolation on restored devices.

### 2.4 Multi-Tenant Shelf Owner Isolation
The Room persistence layer partitions user shelf states and reading progress by `ownerId`:
- Signed-out users are bound strictly to `ownerId = "guest"`.
- Authenticated users operate under their verified user ID.
- When switching accounts or signing out, Room queries filter by the new `ownerId`, guaranteeing that one user's private reading habits are never displayed on another user's device screen.

### 2.5 Secure Session Token Vault (`SecureSessionStore`)
Authentication bearer tokens and device identity UUIDs are stored in private app-scoped preferences (`Context.MODE_PRIVATE`) and sanitized upon explicit logout. Token digests are never transmitted in cleartext or logged to Android Logcat.

### 2.6 Zero Unsolicited Telemetry
Fable contains zero advertising, third-party analytics, or behavioral tracking SDKs. Network traffic occurs strictly in response to user actions (catalog browsing, chapter loading, account sync).

