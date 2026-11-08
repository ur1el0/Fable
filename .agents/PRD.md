# Fable Android Product Requirements

## Product Intent

Fable Android is the native Kotlin and Jetpack Compose client for the shared Fable product. It supports story discovery, reading, a personal shelf, accounts, and community publishing while retaining locally cached stories, chapters, and reading state on the device.

Android and iOS use the shared Fable FastAPI service for catalogs, provider integrations, accounts, publishing, and cross-device shelf and reading-stat synchronization. Android must not duplicate provider scraping or backend business logic.

## Audience

- Readers who want to discover and read curated prose and manga.
- Readers who need previously cached stories and chapters to remain available offline.
- Writers who create an account and publish stories through Fable.

## Product Requirements

### Discovery and story details

- Present the Fable catalog and provider-backed discovery through the shared API.
- Support the provider and content-format identifiers defined by the backend contract.
- Keep catalog metadata in Room so previously fetched stories remain available when refreshes fail.
- Fetch chapter content on demand. Catalog searches must not download full chapter bodies.
- Preserve already cached chapter content when a partial response or provider failure contains less data.

### Reader

- Support the implemented scroll and paginated layouts, reader themes, font choices, font scaling, and line-spacing settings.
- Persist reader preferences locally.
- Restore reading position and load missing chapters on demand.
- Persist fetched chapters and reading progress locally so cached content can be reopened offline.
- Support chapter navigation for prose and provider-supplied image pages where available.

### Library and shelf

- Show locally cached stories and their reading state.
- Support bookmark, completion, progress, and last-read chapter state.
- Keep shelf state scoped by account, with a guest scope when signed out.
- Reconcile local shelf changes with the shared backend when signed in without discarding local data on a failed request.

### Accounts and publishing

- Support registration, sign-in, profile updates, password reset, and sign-out through the shared API.
- Require an authenticated account to publish stories.
- Show account-published stories and surface API failures as recoverable UI state.
- Keep credentials and account state behind the data layer; presentation code must not call network endpoints directly.

### Reading statistics

- Record reading sessions locally before attempting synchronization.
- Retry pending session synchronization on a later app-driven sync opportunity.
- Cache account reading statistics locally for display when available.

## Offline and Network Boundaries

- Room is the source of truth for story and chapter content already cached on the device, reader preferences, and locally recorded reading state.
- Remote catalog discovery, uncached chapters, authentication, publishing, and cross-device synchronization require network access.
- Offline support does not imply that uncached provider content is available without a connection.
- A provider outage must not erase cached stories, cached chapters, or pending local reading state.
- The emulator debug URL is not a physical-device URL. Physical-device testing must use a reachable LAN or deployed API endpoint; production endpoints must use HTTPS.

## Quality Requirements

- Keep presentation, domain contracts, and data implementations separated.
- Expose immutable UI state and keep database/network work off the main thread.
- Cover API contracts, mapping, repository reconciliation, reader progress, and account state with deterministic tests.
- Run instrumentation tests on an emulator or device before claiming runtime validation. Compilation alone is not runtime validation.
- Do not claim a release-ready offline experience until cached-content behavior has been exercised with networking disabled on a device.

## Explicitly Deferred

These items belong to a future product decision, not the current Android implementation plan:

- Android-side website scraping, source plugins, or provider-specific extraction.
- A WorkManager batch-download queue or a separate chapter-file storage pipeline.
- Cloud backup beyond the existing shared account and synchronization contracts.
- New formats or integrations not present in the shared product/API contract.

