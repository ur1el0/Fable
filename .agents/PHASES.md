# Fable Android Delivery Phases

This roadmap reflects the current shared Fable product and Android code. It has no calendar estimates. The actionable open work is listed in .agents/backlog.md.

## Current State

The app has the main product foundations and flows in code:

- Compose screens for Library, Explore, Write, Shelf, story details, the reader, and account access.
- A shared FastAPI client for catalog and provider discovery, story/chapter retrieval, accounts, publishing, shelf sync, and reading statistics.
- Room-backed story/chapter caching, account-scoped shelf state, reading-session outbox, and cached reading statistics.
- On-demand chapter hydration, reader progress restoration, scroll and paginated layouts, and persistent reader preferences.
- JVM tests, lint, debug APK assembly, and Android-test compilation pass on the current merged main.

This is not a release-readiness claim. Instrumentation tests have not yet been run on a connected device in the current environment.

## Phase 1: Runtime Integration and Reliability

Current phase. Validate the app against a reachable backend on an emulator or physical device. Exercise health, catalog discovery, chapter loading, sign-in, shelf synchronization, publishing, and offline reopening of cached content. Distinguish Android transport/configuration failures from backend or provider outages.

Only add code when a reproducible client defect or user-visible reliability gap is confirmed.

## Phase 2: Product Completion

After runtime validation, compare the implemented Android user flows with the current shared product/API contract and Fable iOS behavior. Select small, user-facing gaps based on observed parity differences and user priorities. Keep any new backend behavior in the shared backend rather than reimplementing it in Android.

## Phase 3: Release Readiness

Validate accessibility, screen-size behavior, reader performance, offline data retention, database migrations, production API configuration, and release build handling. Record measurable results instead of adopting unverified performance targets.

## Deferred

Direct website extractors, scraping, plugin systems, WorkManager bulk downloads, and unrelated file-format support are not active phases. Reconsider them only through an explicit product decision and an updated architecture record.

