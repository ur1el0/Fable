# 01 System Overview

## Product Context

Fable Android is the native client for the shared Fable product. Its user-facing workflows include story discovery, a local library and shelf, prose and manga reading, accounts, and community publishing. The shared Fable FastAPI service owns provider integrations, shared catalog behavior, account operations, publishing, and cross-device synchronization.

ADR 009 is the authoritative product and API decision. This document describes the current implementation; source code and API contract tests remain authoritative when implementation details change.

## Architecture

### Presentation

Jetpack Compose screens are composed by FableApp. ViewModels expose immutable StateFlow UI state and call domain repository interfaces. The current app uses explicit dependency construction in FableApplication rather than a generated dependency-injection framework.

### Domain

The domain includes Story, Chapter, provider/content-format identifiers, reader preferences, user profiles, and reading statistics. Repository interfaces define story, account, preference, and statistics operations.

### Data

- Room persists stories, chapters, account-scoped shelf state, pending reading sessions, and cached reading statistics.
- SharedPreferences stores reader preferences.
- The session and device identity stores provide API-client credentials and device identity.
- FableApiService implements the shared backend contract.
- Repositories coordinate API calls with Room so fetched content is cached and local reading state survives remote failures.

### Backend Boundary

Android calls the shared Fable API for curated and provider catalogs, story and chapter retrieval, accounts, publishing, shelf synchronization, and reading statistics. Provider-specific scraping and business rules do not belong in the Android client.

## Data and Offline Contract

Room is authoritative for content already cached on the device and locally recorded reading state. Catalog refreshes and chapter fetches update Room, which the UI observes. Chapters are fetched on demand; catalog search does not download every chapter body.

Offline availability is limited to data already stored locally. Uncached discovery, chapters, account actions, publishing, and cross-device synchronization require a connection. Network or provider failures must not erase richer cached content or pending local reading sessions.

## Runtime Configuration

The API base URL is supplied through the Gradle property fableApiBaseUrl. The emulator development default uses the emulator host bridge and is not reachable from a physical phone. Device testing must configure a LAN or deployed URL reachable from the device. Production configuration must use HTTPS.

## Verification Baseline

JVM tests cover API contracts, mappings, repository synchronization, reader progress, and account state. Instrumentation tests cover Compose interactions and Android persistence behavior. A successful compile of instrumentation tests is not equivalent to running them on a device.

## Deferred Designs

The original direct-source extractor pipeline, WorkManager download queue, and separate chapter-file storage design are not current architecture. Do not implement them without a new product decision and an updated ADR. Older architecture proposals that conflict with ADR 009 or current source are historical references, not implementation instructions.

