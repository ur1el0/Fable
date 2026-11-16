# 08 Core Feature Workflows

These flows describe the current Android client and shared backend boundary. Provider-specific work is performed by the shared backend, not by Android-side scrapers.

## 1. Federated Discovery and Provider Filtering

1. The UI exposes filter chips for catalog discovery: All, Fable Originals, Project Gutenberg, and Standard Ebooks.
2. When the user initiates a search or selects a provider filter:
   - Queries to `/api/v1/public/gutenberg` and `/api/v1/public/standard-ebooks` are dispatched independently.
   - If one provider returns an upstream error (e.g. 502/504), healthy provider stories are rendered immediately, and the UI displays a non-blocking error badge or banner for the failing provider.
3. Cross-Provider Edition Disambiguation:
   - Same-title works from different providers (e.g. *Frankenstein* from Gutenberg vs Standard Ebooks) retain distinct story IDs (`sourceProvider` + `providerId`), independent chapter counts, and separate Room cache entries.
   - The UI displays explicit provider badges so the reader can distinguish between standard plain-text archives and high-typography editions.

## 2. Catalog Refresh and Local Cache

1. The UI observes catalog and shelf data from Room via reactive Kotlin `Flow`.
2. The ViewModel requests the repository to refresh the shared catalog or provider results.
3. `FableApiService` makes the asynchronous API request.
4. The repository maps the response and persists it to Room (`stories` table).
5. Room Flow emissions update the UI without UI thread blocking.
6. If the request fails, existing cached stories remain fully accessible and the UI receives a recoverable error state.

## 3. Story and On-Demand Chapter Loading

1. The user opens a story from discovery, the library, or the shelf.
2. The repository loads locally cached story and chapter data from Room.
3. Missing metadata or chapter content is fetched on demand through the shared API (`/api/v1/public/{provider}/{id}/chapters`).
4. Returned chapter data is merged into Room within a `@Transaction` without replacing richer cached content with metadata-only values.
5. The reader observes the persisted chapters and requests subsequent chapters when navigation or continuous reading requires it.

## 4. Account-Scoped Shelf Synchronization

1. Account state supplies the current owner identifier, or the repository uses the guest scope while signed out.
2. Bookmark, completion, and reading-position changes are written to local shelf state in `shelf_states` table.
3. When signed in and online, the repository synchronizes local changes with the shared API (`POST /api/v1/shelf/sync`) using the account and device identity.
4. A failed synchronization leaves local shelf state intact for a later retry.
5. Switching accounts changes the observed shelf scope in Room; one owner's state is strictly isolated from another owner's shelf.

## 5. Reading Sessions and Statistics Outbox

1. Reader lifecycle events update local reading progress and record completed reading sessions in Room (`reading_sessions` table).
2. Pending sessions are retained locally until network connectivity and account session are verified.
3. Upon sync, the repository calls `GET /api/v1/auth/me/reading-stats` and caches returned aggregate stats in `reading_stats`.
4. The Shelf screen observes cached statistics via Room `Flow` and refreshes them upon sync completion.

## 6. Account and Publishing

1. Account actions are initiated by `AccountViewModel` and delegated to `AccountRepository`.
2. The repository calls the shared API and manages bearer tokens via `SecureSessionStore`.
3. Publishing requires an authenticated user and sends the manuscript through `POST /api/v1/stories`.
4. Success or failure is reflected in immutable UI state; provider and publishing logic are completely isolated from the presentation layer.

## Offline Boundary

Previously cached stories, chapters, reader preferences, and local reading state remain available offline. Remote search, uncached content, authentication, publishing, and cross-device synchronization require an active network connection. There is no current WorkManager batch-download workflow.

