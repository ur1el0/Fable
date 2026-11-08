# Fable Android Open Backlog

This is the open-work list for the implementation described by ADR 009. Do not copy unchecked items from the original scraper/download-manager roadmap into active work.

## Priority 0: Runtime Integration

- [ ] Connect an Android emulator or physical device and configure a backend URL reachable from that device.
- [ ] Exercise backend health, catalog search, story details, and an on-demand chapter fetch from the Android app.
- [ ] Sign in, change shelf state, verify account-scoped synchronization, then sign out and confirm the guest shelf remains isolated.
- [ ] Publish a test story through the UI and verify the resulting account-owned story.
- [ ] Open a cached story and chapter with networking disabled; verify reading position and local state remain usable.
- [ ] Run the instrumentation suite on a device or emulator and record runtime failures separately from compile failures.

## Priority 1: Confirmed Reliability Gaps

- [ ] Reproduce provider catalog cold-cache failures and confirm the Android UI preserves cached results and presents a recoverable error.
- [ ] Verify partial catalog or chapter refreshes never replace richer cached metadata or content with empty fields.
- [ ] Exercise process recreation during reading and pending reading-session synchronization.
- [ ] Review account changes and shelf synchronization for stale-owner state on device.

## Priority 2: Release Readiness

- [ ] Verify production API configuration uses a reachable HTTPS endpoint and debug-only local URLs do not leak into release builds.
- [ ] Review accessibility semantics, touch targets, font scaling, and narrow-screen layouts on device.
- [ ] Measure reader scrolling and chapter-open behavior on representative hardware before setting performance targets.
- [ ] Validate Room migrations from each supported prior schema and document the upgrade path.

## Deferred by Product Scope

- Direct Android scraping, extractor plugins, and provider-specific clients.
- WorkManager batch downloads and a separate chapter-file storage subsystem.
- New account, backup, or provider behavior that is not part of the shared API contract.

