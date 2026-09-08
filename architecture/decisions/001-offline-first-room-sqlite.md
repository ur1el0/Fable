# ADR 001: Offline-First Architecture & Room SQLite Single Source of Truth

## Status
Accepted

## Context
Fable is designed as a personal, local-first reading application where users read long-running serialized novels during transit or offline environments. The application requires:
- Guaranteed access to library books, chapters, and bookmarks without internet dependency.
- Reactive UI updates when library state, download status, or reading progress changes.
- Transaction safety to prevent database corruption during concurrent background downloads and user interactions.

## Decision
We chose **AndroidX Room (over embedded SQLite)** as the absolute single source of truth for the application.
- All observable queries return Kotlin Coroutines `Flow<T>`, ensuring reactive UI state synchronization.
- All database write and mutation operations are declared as `suspend` functions and offloaded from the Main (UI) thread.
- Multi-row updates and cascading operations are strictly wrapped in `@Transaction`.
- Remote content fetched from web sources is written into Room first before being observed by the presentation layer.

## Consequences
- **Positive:** 100% offline resilience once data is saved locally; deterministic UI state; zero stale UI cache bugs.
- **Positive:** Compile-time SQL verification via KSP prevents syntax errors and schema mismatches before build time.
- **Negative:** Requires disciplined schema version management and explicit `Migration` classes as database tables evolve.
