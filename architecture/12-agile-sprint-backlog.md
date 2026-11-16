# 12 Delivery Planning & Milestone Log

The former fixed two-week sprint plan described an earlier Android-only scraper and bulk-download product. It is superseded by the shared product/API decision in ADR 009.

## Completed Milestones (Branch: `feat/federated-book-sources`)

1. **ADR 009 Contract Record (`877be2a`):** Recorded the shared federated prose provider contract in ADR 009, establishing that Gutenberg and Standard Ebooks operate through the shared backend with on-demand chapter hydration.
2. **Cross-Provider Edition Identity Tests (`c17f7c8`):** Verified that same-title works across Standard Ebooks and Gutenberg retain distinct story IDs, `sourceProvider`, and `providerId` across Room caching, chapter loading, and shelf reading progress.
3. **UI Provider Filter Chips & Badges (`8857c7f`):** Exposed Standard Ebooks catalog filter chips, source provider labels, and fault-isolated error banners in Jetpack Compose UI.
4. **Standard Ebooks Discovery & Hydration (`14df215`):** Added `FableApiService` and `RoomStoryRepository` support for Standard Ebooks discovery and on-demand chapter loading via the shared API.

## Active Delivery Roadmap

Use `.agents/PHASES.md` for overall delivery phases and `.agents/backlog.md` for current open backlog items. Do not treat the old sprint sequence, extractor checklist, or WorkManager download plan as active requirements.

Work proceeds in focused, verifiable milestones. Runtime validation must distinguish Android client behavior from backend or upstream-provider failures. A feature is not considered device-verified merely because its instrumentation tests compile.

