# 12 Delivery Planning

The former fixed two-week sprint plan described an earlier Android-only scraper and bulk-download product. It is superseded by the shared product/API decision in ADR 009.

Use .agents/PHASES.md for delivery phases and .agents/backlog.md for current open work. Do not treat the old sprint sequence, extractor checklist, or WorkManager download plan as active requirements.

Work proceeds in small milestones with a focused verification boundary. Runtime validation must distinguish Android client behavior from backend or upstream-provider failures. A feature is not considered device-verified merely because its instrumentation tests compile.

