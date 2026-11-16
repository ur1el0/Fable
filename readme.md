# Fable Android: Federated Literature & Distraction-Free Reader

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2+-purple.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024%2B)-green.svg)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![Room Database](https://img.shields.io/badge/Database-Room%20v3%20(SQLite)-blue.svg)](https://developer.android.com/training/data-storage/room)
[![Backend](https://img.shields.io/badge/Backend-Shared%20FastAPI%20Contract-009688.svg)](https://fastapi.tiangolo.com)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Fable Android** is a native Kotlin and Jetpack Compose literary reader designed as an offline-first companion to Fable-IOS and the shared FastAPI backend. Engineered for typographical beauty and deep reading, Fable federates public-domain literature (**Standard Ebooks** with semantic typography and **Project Gutenberg**) alongside curated **Fable Originals**, complete with on-demand chapter hydration, multi-tenant shelf synchronization, and reading analytics.

---

## 1. System Architecture & Tech Stack

* **Language & Runtime:** Kotlin 2.2+ with Coroutines & StateFlow (Strict Unidirectional Data Flow).
* **User Interface:** 100% Declarative Jetpack Compose with Material Design 3 (Material You), dynamic theming, and edge-to-edge support.
* **Persistence (Room SQLite v3):**
  - Room SQLite database with compile-time KSP query validation.
  - Automated SQLite migrations (`MIGRATION_1_2` and `MIGRATION_2_3`).
  - Account-scoped shelf isolation partitioning guest and authenticated user states.
  - Offline session queue outbox for reading analytics.
* **Networking & Contract Parity:**
  - `FableApiService` powered by OkHttp and `kotlinx.serialization`.
  - Camel-case contract parity with the shared FastAPI backend.
  - Independent query dispatch for federated literature providers with fault isolation.
* **Image Pipeline:** Coil 3 with bounded memory caches for cover illustrations.
* **Security & Privacy:**
  - Strict release API URL validation (enforces HTTPS ending in `/api/v1/`).
  - Cleartext traffic disabled by default in production (`res/xml/network_security_config.xml`).
  - Auto-backup exclusions for secure session tokens and SQLite database files (`res/xml/backup_rules.xml`).
  - Zero third-party telemetry, ads, or behavioral analytics trackers.

---

## 2. Core Feature Highlights

### 2.1 Federated Literature Discovery
- **Multi-Source Catalog:** Unified search and discovery across Standard Ebooks (`STANDARD_EBOOKS`), Project Gutenberg (`GUTENBERG`), and Fable Originals (`FABLE_ORIGINAL`).
- **Cross-Provider Edition Disambiguation:** Same-title works (e.g. Mary Shelley's *Frankenstein* or Kafka's *The Metamorphosis*) retain independent identifiers, provider badges, distinct chapter counts, and separate shelf progress.
- **Provider Fault Isolation:** Upstream outages on one provider display non-intrusive recoverable warning indicators without blocking healthy provider results.

### 2.2 Ambient Reading Engines
- **Paginated Mode (`ReaderLayout.PAGINATED`):** Horizontal book experience powered by Compose `HorizontalPager` with edge tap zones, character-budgeted pagination, and instant progress restoration.
- **Continuous Scroll Mode (`ReaderLayout.SCROLL`):** Smooth vertical kinetic scroll using `LazyColumn` with infinite chapter chaining.
- **Typographic HUD:**
  - 6 Ambient Themes: White, Paper, Sepia (default), Charcoal, Midnight, and OLED Pure Black.
  - 4 Font Families: Serif, Sans, Rounded, Monospace.
  - Scalable font sizing (80% to 150%) and 3 line spacing presets (Compact, Normal, Spacious).
- **On-Demand Chapter Hydration:** Catalog metadata remains lightweight (<250 KB); full chapter bodies are downloaded on demand when opening a book and cached locally in SQLite for offline continuity.

### 2.3 Account, Shelf & Personal Scholarship
- **Multi-Tenant Shelf Isolation:** Bookmarks, reading completion, and last-read chapter pointers are strictly partitioned by `ownerId` (`"guest"` vs authenticated user).
- **Bidirectional Cloud Sync:** Synchronizes shelf changes with the backend via `POST /api/v1/shelf/sync` with Last-Write-Wins (LWW) resolution.
- **Reading Analytics & Journal:** Tracks total minutes read, completed stories count, and consecutive day streaks with an offline session sync outbox.
- **Story Publishing:** Authenticated authors can compose manuscripts with live word counts and publish them directly to the community catalog.

---

## 3. Project Documentation Index

Detailed architectural specifications and engineering standards are maintained in the [`architecture/`](architecture/) directory:

| Specification | Focus |
|---|---|
| **[`architecture/01-system-overview.md`](architecture/01-system-overview.md)** | Product context, Clean Architecture boundaries, and offline invariants |
| **[`architecture/02-ui-layer-compose.md`](architecture/02-ui-layer-compose.md)** | Jetpack Compose UDF state machines, screens, and components |
| **[`architecture/03-domain-and-data-layer.md`](architecture/03-domain-and-data-layer.md)** | Clean Architecture layer separation and domain repository contracts |
| **[`architecture/04-database-schema-room.md`](architecture/04-database-schema-room.md)** | Room Schema Version 3 specifications, 5 tables, and SQLite migrations |
| **[`architecture/05-networking-and-extractors.md`](architecture/05-networking-and-extractors.md)** | `FableApiService` federated endpoints, failure isolation, and deferred scrapers |
| **[`architecture/06-download-manager-workmanager.md`](architecture/06-download-manager-workmanager.md)** | On-demand chapter hydration architecture and deferred WorkManager specification |
| **[`architecture/07-reader-engine-and-rendering.md`](architecture/07-reader-engine-and-rendering.md)** | Dual-mode reading engine, typography settings, and progress mapping |
| **[`architecture/08-core-feature-workflows.md`](architecture/08-core-feature-workflows.md)** | Federated discovery, on-demand hydration, shelf sync, and publishing |
| **[`architecture/09-security-and-data-privacy.md`](architecture/09-security-and-data-privacy.md)** | HTTPS release URL guards, cleartext policy, and backup exclusions |
| **[`architecture/10-testing-and-qa-iso25010.md`](architecture/10-testing-and-qa-iso25010.md)** | Automated test suite matrix, ISO-25010 baselines, and QA criteria |
| **[`architecture/decisions/009-ios-product-and-api-contract.md`](architecture/decisions/009-ios-product-and-api-contract.md)** | **ADR 009:** Authoritative shared product, API contract, and deferred scrapers |

---

## 4. Verification & Testing

Verify local build health and unit test baselines:

```bash
# Run JVM unit tests across API contracts, mappers, repositories, and ViewModels
./gradlew testDebugUnitTest

# Run Android lint checks
./gradlew lintDebug

# Assemble debug APK
./gradlew assembleDebug
```

---

## License

MIT License. See [LICENSE](LICENSE) for details.
