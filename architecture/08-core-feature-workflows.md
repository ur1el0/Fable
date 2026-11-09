# 08 Core Feature Workflows

These flows describe the current Android client and shared backend boundary. Provider-specific work is performed by the shared backend, not by Android-side scrapers.

## 1. Catalog Refresh and Local Cache

1. The UI observes catalog and shelf data from Room.
2. The ViewModel asks the repository to refresh the shared catalog or provider results.
3. FableApiService makes the API request.
4. The repository maps the response and persists it to Room.
5. Room Flow emissions update the UI.
6. If the request fails, existing cached stories remain available and the UI receives an error state.

## 2. Story and On-Demand Chapter Loading

1. The user opens a story from discovery, the library, or the shelf.
2. The repository loads locally cached story and chapter data.
3. Missing metadata or chapter content is fetched through the shared API when needed.
4. Returned chapter data is merged into Room without replacing richer cached content with metadata-only values.
5. The reader observes the persisted chapters and requests another chapter when navigation or continuous reading requires it.

## 3. Account-Scoped Shelf Synchronization

1. Account state supplies the current owner identifier, or the repository uses the guest scope while signed out.
2. Bookmark, completion, and reading-position changes are written to local shelf state.
3. When signed in and online, the repository synchronizes local changes with the shared API using the account and device identity.
4. A failed synchronization leaves local shelf state intact for a later retry.
5. Switching accounts changes the observed shelf scope; one owner's state must not be shown as another owner's shelf.

## 4. Reading Sessions and Statistics

1. Reader lifecycle events update local reading progress and record completed reading sessions in Room.
2. Pending sessions are retained locally until the account is available and the API sync succeeds.
3. The repository stores returned aggregate reading statistics locally.
4. The Shelf screen observes cached statistics and can refresh them when appropriate.

## 5. Account and Publishing

1. Account actions are initiated by AccountViewModel and delegated to the account repository.
2. The repository calls the shared API and manages session state through the session store.
3. Publishing requires an authenticated user and sends the story through the shared API.
4. Success or failure is reflected in immutable UI state; provider and publishing logic are not implemented in the presentation layer.

## Offline Boundary

Previously cached stories, chapters, reader preferences, and local reading state remain available offline. Remote search, uncached content, authentication, publishing, and cross-device synchronization do not. There is no current WorkManager batch-download workflow.

