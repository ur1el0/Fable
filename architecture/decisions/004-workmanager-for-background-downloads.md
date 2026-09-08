# ADR 004: WorkManager for Background Chapter Downloads

## Status
Accepted

## Context
Batch downloading serialized chapters (e.g., downloading the next 50 chapters of a novel) requires network fetching, HTML parsing, content sanitization, and disk I/O. If managed via ephemeral `viewModelScope` coroutines, downloads terminate immediately when the user leaves the screen or when Android reclaims memory in low-RAM conditions.

## Decision
We chose **AndroidX WorkManager** as the dedicated background task orchestration engine:
- Batch downloads are enqueued as unique work chains using `ExistingWorkPolicy.APPEND_OR_REPLACE`.
- Tasks execute via `CoroutineWorker` on `Dispatchers.IO`.
- On Android 14+ (API 34+), workers promote to Foreground Services with `ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC` and post a user-visible notification showing live percentage progress and a cancel button.
- System constraints enforce network connectivity (`NetworkType.CONNECTED`) and battery health.

## Consequences
- **Positive:** Guaranteed execution across app restarts, process deaths, and configuration changes.
- **Positive:** Battery and data conservation via system-level constraint monitoring.
- **Negative:** Requires careful handling of Android 13+ `POST_NOTIFICATIONS` runtime permissions and Android 14+ foreground service type declarations.
