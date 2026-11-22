# 03 Domain & Data Layer Architecture

## 1. Domain Layer (`com.example.fable.domain`)

The domain layer is pure Kotlin and defines the core models, deterministic calculation engines, and repository interfaces:

### Core Domain Models (`domain/model/`)
- **`Story` & `Chapter`**: Format-aware (`ContentFormat.PROSE`, `ContentFormat.COMIC`) and provider-aware (`StoryProvider.FABLE`, `GUTENBERG`, `STANDARD_EBOOKS`, `MANGADEX`) models with ownership metadata (`ownerUserId`), shelf flags (`isBookmarked`, `isCompleted`, `readingProgress`, `lastReadChapterIndex`), prose body (`content`), and comic page descriptors (`pages: List<ChapterPage>`).
- **`StoryHighlight` & `HighlightColor`**: Passage highlight model supporting exact word ranges (`paragraphIndex`, `startWordIndex`, `endWordIndex`), color swatches (`TERRACOTTA`, `AMBER`, `SAGE`), optional reader notes, and timestamps.
- **`ReaderPreferences` & `AppAppearance`**: User preferences for `ReaderTheme` (`PAPER`, `SEPIA`, `NIGHT`), `ReaderFont` (`SERIF`, `SANS`, `MONO`), `ReaderLayout` (`SCROLL`, `PAGINATED`), `ComicReaderLayout` (`WEBTOON`, `MANGA`), font size (`15f..26f`), line height (`1.25f..1.9f`), `appAppearance` (`SYSTEM`, `LIGHT`, `DARK`), and optional `readerBrightness` (`0.05f..1.0f` or `null` for system auto).
- **`AccountSession`, `AuthUser`, `ReadingStatsSummary`, `ReadingSessionRecord`, `WriterDraft`, `StorySubmission`**.

### Deterministic Reading & Annotation Engines (`domain/reading/`)
- **`ReaderTextPaginator`**: Splits prose chapters into normalized paragraphs and balanced screen-sized pages (`ReaderPageSlice`) with global word offsets (`firstWordIndex`, `wordCount`).
- **`ReaderProgressMapper`**: Converts between chapter-level normalized progress (`0f..1f`), scroll item offsets, paginated page indices, and word indices so switching font size, line height, or scroll/paginated layout preserves the user's exact reading location.
- **`InBookSearchEngine`**: Tokenizes paragraphs into words (`tokenizeWords`), slices word ranges (`sliceWordRange`), and executes fast cross-chapter keyword searches (`searchStory`) returning contextual snippets and word offsets.
- **`HighlightExporter`**: Formats a story's highlights and notes into shareable plain text or Markdown documents.
- **`ComicZoomMath`**: Computes clamped zoom scales (`1f..4f`), double-tap zoom transitions (`1f <-> 2.5f`), and viewport-bounded pan offsets for comic pages.

---

## 2. Data Layer Repositories (`com.example.fable.data.repository`)

| Interface | Implementation | Key Responsibilities |
|---|---|---|
| `StoryRepository` | `RoomStoryRepository` | Observes stories and offline download IDs from Room; refreshes Fable, Gutenberg, Standard Ebooks, and MangaDex catalogs concurrently; hydrates prose and manga chapters on demand; executes story publish/edit/delete; manages explicit offline chapter download and removal; reconciles account-scoped `shelf_states`. |
| `HighlightRepository` | `RoomHighlightRepository` | Observes, upserts, and deletes passage highlights in `story_highlights` (`HighlightDao`). |
| `AccountRepository` | `RemoteAccountRepository` | Persists auth tokens and account profile locally; performs sign-in, registration, profile update, password reset, sign-out, and account deletion (`DELETE /auth/me`); automatically resets session state on `401 Unauthorized` and coordinates local account data purge. |
| `ReadingStatsRepository` | `RoomReadingStatsRepository` | Records local reading sessions first (`reading_sessions`), updates optimistic local summaries (`reading_stats`), and syncs pending sessions (`POST /me/reading-sessions`) with the backend. |
| `WriterDraftRepository` | `RoomWriterDraftRepository` | Persists and observes local manuscript drafts (`writer_drafts`). |
| `ReaderPreferencesRepository` | `SharedPreferencesReaderPreferencesRepository` | Persists reader typography, layout, brightness, and global `AppAppearance` settings. |
| `ServerConfigRepository` | `DebugServerConfigStore` | Stores and validates debug runtime API base URL overrides (`ServerUrlValidator`); strictly disabled in release builds. |
