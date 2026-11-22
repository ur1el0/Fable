# Fable — Technical & Domain Glossary

This document defines standard technical terminology, domain models, and architectural concepts used across Fable Android.

---

## 1. Domain Entities & Content Providers

- **Story**: The primary domain entity (`domain/model/Story.kt`) representing a literary work or manga series. Contains metadata (`id`, `ownerUserId`, `title`, `author`, `genre`, `synopsis`, `readTimeMinutes`, `rating`), provider origin (`StoryProvider`), medium (`ContentFormat`), cover image URL, and account-scoped shelf state (`isBookmarked`, `isCompleted`, `readingProgress`, `lastReadChapterIndex`).
- **Chapter**: An individual installment (`Chapter`) belonging to a `Story`. Contains `index`, `title`, prose `content`, or comic `pages: List<ChapterPage>`.
- **StoryProvider**:
  - `FABLE`: Community-published original stories on the shared Fable backend.
  - `GUTENBERG`: Public-domain classics normalized via `/api/v1/books/gutenberg/*`.
  - `STANDARD_EBOOKS`: Curated typography-grade public-domain editions normalized via `/api/v1/books/standard-ebooks/*`.
  - `MANGADEX`: Sequential art (manga/webtoons) normalized via `/api/v1/manga/*`.
- **ContentFormat**:
  - `PROSE`: Text-based chapters rendered via `ContinuousChapterList` (`ReaderLayout.SCROLL`) or `PaginatedChapterPager` (`ReaderLayout.PAGINATED`).
  - `COMIC`: Image-based chapters rendered via `MangaChapterReader` (`ComicReaderLayout.WEBTOON` or `MANGA`).
- **StoryHighlight**: A persisted passage highlight (`story_highlights` table in Room v6) capturing `storyId`, `chapterIndex`, `paragraphIndex`, `startWordIndex`, `endWordIndex`, `quoteText`, optional `note`, and `HighlightColor` (`TERRACOTTA`, `AMBER`, `SAGE`).

---

## 2. Local Persistence & Clean Architecture

- **`FableDatabase` (Room Schema v6)**: The offline-first SQLite single source of truth containing 7 tables (`stories`, `chapters`, `shelf_states`, `reading_sessions`, `reading_stats`, `writer_drafts`, `story_highlights`) with verified migrations `MIGRATION_1_2` through `MIGRATION_5_6`.
- **`coalesceChapters`**: Non-destructive merge logic in `RoomStoryRepository` that preserves cached prose chapter text and comic page lists when catalog summary endpoints return empty chapter payloads.
- **`FableNavBackStack`**: Typed navigation back stack (`ui/navigation/FableNavigation.kt`) managing top-level `FableTab` roots and drill-down `FableDestination` screens (`StoryDetail`, `StoryReader`, `StoryEdit`).
- **UDF (Unidirectional Data Flow)**: Pattern where immutable UI state flows down from focused ViewModels (`CatalogViewModel`, `ExploreViewModel`, `ReaderViewModel`, `StoryEditorViewModel`, `WriterDraftViewModel`, `AccountViewModel`, `ServerConfigViewModel`) to stateless Compose screens, and user intents flow up via callbacks.
