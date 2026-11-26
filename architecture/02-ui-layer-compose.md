# 02 UI Layer & Jetpack Compose Architecture

## 1. Navigation & Screen Topology

`FableApp.kt` coordinates top-level navigation and drill-down destinations using the typed navigation state holder in `ui/navigation/FableNavigation.kt`:

- **`FableTab`**:
  - `LIBRARY` (`LibraryScreen.kt`) — Featured stories, *"Continue Reading"* rail, pull-to-refresh catalog sync, and curated shelves.
  - `EXPLORE` (`ExploreScreen.kt`) — Format-aware discovery toggle (**Books** vs **Manga & Comics**), provider filter chips (`All Books`, `Fable`, `Gutenberg`, `Standard Ebooks`, `MangaDex`), debounced search, trending tags, and clickable featured writers.
  - `SHELF` (`ShelfScreen.kt`) — Reading queue filtered by `Saved`, `In Progress`, `Finished`, and `Offline`, paired with reading streak and reading time cards.
  - `WRITE` (`WriteScreen.kt`) — Mobile writer studio with local Room draft auto-save, live word/character count, estimated reading time, chapter marker insertion (`Chapter N:`), and story publishing.
  - `ACCOUNT` (`AccountScreen.kt`) — Authentication (`Sign In`, `Create Account`, `Reset Password`), profile management, global `AppAppearance` selector (`System`, `Light`, `Dark`), author's published stories (`Edit` / `Delete`), account deletion, and debug-only runtime server URL switcher.
- **`FableDestination` & `FableNavBackStack`**:
  - `FableDestination.TabRoot(tab: FableTab)`
  - `FableDestination.StoryDetail(storyId: String)` (`StoryDetailScreen.kt`)
  - `FableDestination.StoryReader(storyId: String, chapterIndex: Int?)` (`StoryReaderScreen.kt`)
  - `FableDestination.StoryEdit(storyId: String)` (`EditStoryScreen.kt`)
  - `FableNavBackStack` persists across configuration changes and process recreation via `FableNavBackStack.Saver` and supports targeted cleanup via `popStory(storyId)` when a story is deleted.

---

## 2. Decomposed ViewModel Responsibilities

| ViewModel | Primary State & Responsibility |
|---|---|
| `CatalogViewModel` | Observes local Room stories and downloaded story IDs (`observeDownloadedStoryIds()`), drives catalog refresh, and manages per-story offline chapter download/removal (`OfflineDownloadUiState`). |
| `ExploreViewModel` | Manages format-aware discovery (`ContentFormat.PROSE` vs `COMIC`), provider filters, debounced search across Fable/Gutenberg/Standard Ebooks/MangaDex, and cancels superseded searches. |
| `ReaderViewModel` | Hydrates missing story chapters, debounces progress updates (350ms), records reading sessions (`ReadingSessionRecord`), manages reader typography/brightness/theme preferences, and coordinates Room passage highlights (`HighlightRepository`). |
| `StoryEditorViewModel` | Validates and publishes new stories (`WriteScreen`), hydrates full chapter bodies before opening `EditStoryScreen` so summary responses never overwrite manuscripts, and executes owner story updates (`PATCH /stories/{id}`) and deletions (`DELETE /stories/{id}`). |
| `WriterDraftViewModel` | Auto-saves in-progress manuscripts to `writer_drafts` in Room with debounced writes and explicit draft clearing. |
| `AccountViewModel` | Manages sign-in, registration, profile updates, password reset, sign-out, account deletion (`DELETE /auth/me`), reading stats observation, and automatic 401 session expiry recovery. |
| `ServerConfigViewModel` | Exposes and validates the runtime debug API base URL (`ServerUrlValidator`) in debug builds (`BuildConfig.DEBUG`). |

---

## 3. Adaptive Theme & WCAG AA Dark Palette

`ui/theme/Color.kt` and `ui/theme/Theme.kt` implement an editorial warm-paper light palette and a high-contrast WCAG AA dark palette driven by `ReaderPreferences.appAppearance` (`AppAppearance.SYSTEM`, `LIGHT`, `DARK`):

- **Light Palette**: Warm paper background (`#FBF8F3`), surface (`#F4EFE6`), deep ink text (`#18181B`), muted text (`#52525B`), indigo primary (`#3949AB`).
- **Dark Palette (WCAG AA compliant)**: Deep obsidian background (`#0B0F17`), elevated surface (`#111824`), crisp primary text (`#F1F5F9`), readable secondary text (`#A5B4C8`), luminous periwinkle primary (`#7890FF`).
- **Reader Surface Override**: Inside `StoryReaderScreen`, the reader canvas applies its dedicated reading surface (`ReaderTheme.PAPER`, `SEPIA`, or `NIGHT`) and optional per-session window brightness (`ReaderWindowBrightnessEffect`).
