# Fable — Product Specification

Fable is a mobile reading and community-publishing application built for two companion native clients (**Fable-IOS** in SwiftUI and **Fable Android** in Kotlin + Jetpack Compose) backed by a shared FastAPI service (`services/fable-api`). It combines a typography-first reading interface with format-aware discovery across four content providers (**Fable Originals**, **Project Gutenberg**, **Standard Ebooks**, and **MangaDex**), local-first offline reading and annotations, and a mobile writing workflow with local draft persistence.

---

## 1. Product Philosophy & Core Principles

1. **Reading Is the Primary Activity**
   - Every navigation path leads quickly to the reader.
   - Controls recede while reading; typography, spacing, and contrast take priority over chrome.
2. **Local-First Reliability with Cloud Continuity**
   - Opening the app, reading cached chapters, flipping pages, highlighting passages, and editing drafts work immediately from local Room/SQLite storage without waiting on network round-trips.
   - When signed in and online, Fable synchronizes shelf progress, reading sessions, and published stories with the shared backend without ever overwriting non-empty local chapter caches or drafts on transient network failures.
3. **Format-Aware Discovery & Reading**
   - Prose (`PROSE`) and sequential art (`COMIC`) have distinct reading mechanics and provider boundaries.
   - Discovery separates **Books** (`FABLE`, `GUTENBERG`, `STANDARD_EBOOKS`) from **Manga & Comics** (`MANGADEX`), and the reader switches automatically between prose layouts (Continuous Scroll / Paginated Book Page) and comic layouts (Vertical Webtoon / Horizontal Manga Pager with pinch-to-zoom).
4. **Honest Provider & Capability Boundaries**
   - Only Fable Originals can be published, edited, or deleted by their authenticated author.
   - External catalog stories (Project Gutenberg, Standard Ebooks, MangaDex) are read-only catalogs normalized through the shared Fable API (`https://fable-api-cbo8.onrender.com/api/v1/`).

---

## 2. Information Architecture & Primary Navigation

Fable uses a 5-tab top-level structure with a typed navigation back stack (`FableNavBackStack`) for drill-down screens (`StoryDetail`, `StoryReader`, `StoryEdit`):

| Tab | Purpose | Key Capabilities |
|---|---|---|
| **Library** | Curated home & active reading entry point | Featured hero card, *"Continue Reading"* rail, offline availability pills, pull-to-refresh catalog sync, curated shelves |
| **Explore** | Format-aware multi-provider search & discovery | Format toggle (**Books** vs **Manga & Comics**), source chips (`All Books`, `Fable`, `Gutenberg`, `Standard Ebooks`, `MangaDex`), trending tags, featured writers, clearable search |
| **Shelf** | Personal reading queue & reading analytics | Filter tabs (`Saved`, `In Progress`, `Finished`, `Offline`), reading streak / minutes / completion cards, account-scoped sync banner |
| **Write** | Mobile manuscript studio & publishing | Auto-saved local drafts (`WriterDraftEntity`), live word count & estimated read time, chapter builder (`Chapter 1:` markers), genre suggestions, one-tap publish |
| **Account** | Identity, author studio, appearance, & developer diagnostics | Sign in / Register / Reset password / Sign out / Delete account, global `AppAppearance` (`System`, `Light`, `Dark`), author's published stories with Edit & Delete actions, debug-only runtime server URL switcher |

---

## 3. Core Functional Capabilities

### 3.1 Multi-Source Discovery & Catalog Resilience
- **Providers (`StoryProvider`)**:
  - `FABLE`: Community-published original stories hosted on the Fable API.
  - `GUTENBERG`: Public-domain classics normalized through `/api/v1/books/gutenberg/*`.
  - `STANDARD_EBOOKS`: Curated typography-grade public-domain editions normalized through `/api/v1/books/standard-ebooks/*`.
  - `MANGADEX`: Manga and webtoon catalogs normalized through `/api/v1/manga/*`.
- **Parallel & Fault-Isolated Refresh**:
  - `RoomStoryRepository.refreshCatalog()` queries Fable Originals, Gutenberg, Standard Ebooks, and MangaDex concurrently while preserving non-empty cached chapters (`coalesceChapters`) if a summary endpoint returns empty chapter bodies or an upstream provider times out.

### 3.2 Format-Aware Reader Engine
- **Prose Reader (`ContentFormat.PROSE`)**:
  - **Layouts**: Continuous vertical scroll (`ContinuousChapterList`) and horizontal book pagination (`PaginatedChapterPager`) with word-offset progress mapping (`ReaderProgressMapper`) that preserves exact reading position across font, size, line-height, and layout switches.
  - **Typography & Surface Themes**: Paper, Sepia, and Night surfaces; Serif, Sans, and Mono font families; adjustable font size (`15sp`–`26sp`), line spacing (`1.25x`–`1.9x`), and per-session screen brightness slider (`Auto` system brightness or manual `5%`–`100%`).
  - **In-Book Search**: Cross-chapter keyword search (`InBookSearchEngine`) with match counts, context snippets, and one-tap jump to the matching chapter and paragraph.
  - **Highlights, Notes & Quote Cards**: Long-press any paragraph to select an exact word range (Start/End word steppers), choose a highlight swatch (`TERRACOTTA`, `AMBER`, `SAGE`), attach an optional note, persist to Room (`story_highlights`), export all book highlights as Markdown/text, or render and share a high-resolution 1080px PNG Quote Card (`QuoteCardExporter`).
- **Manga & Webtoon Reader (`ContentFormat.COMIC`)**:
  - **Layouts**: Seamless vertical Webtoon strip and horizontal Manga page-turner (`MangaChapterReader`).
  - **Zoom & Pan**: Two-finger pinch-to-zoom (`1x`–`4x`), bounded pan gestures, double-tap center zoom toggle (`1x <-> 2.5x`), and an accessible `"250% · Reset"` HUD pill.
  - **Lazy Page Loading & Chapter Transitions**: Fixed-aspect placeholders prior to image decode, per-page retry controls on failure, automatic scroll/page restoration, and end-of-chapter transition banners.

### 3.3 Offline Chapter Cache Management
- Readers can explicitly download all chapters of a story for offline reading (`downloadStoryForOffline`) or remove downloaded chapter bodies (`removeStoryOfflineCopy`) while keeping story metadata and shelf progress intact.
- Stories with cached chapters display an `"Offline"` badge across Library, Explore, Shelf (including the dedicated `Offline` filter tab), and Story Detail (`OfflineCacheCard`).

### 3.4 Author Publishing & Story Lifecycle
- **Drafts**: Manuscripts are auto-saved locally in Room (`writer_drafts`) with debounced persistence and explicit `Clear draft` confirmation.
- **Publishing**: Validated against title, genre, synopsis, and chapter body rules before `POST /api/v1/stories`.
- **Editing & Deletion**: Story owners (`story.ownerUserId == session.user.id`) can edit published stories (`EditStoryScreen` $\rightarrow$ `PATCH /api/v1/stories/{id}`, hydrating full chapter content first so summary payloads never overwrite manuscripts) or permanently delete them (`DELETE /api/v1/stories/{id}`) with local Room cascade cleanup.

### 3.5 Account Lifecycle & Privacy
- Supports registration, sign-in, profile updates, password reset, sign-out, and permanent account deletion (`DELETE /api/v1/auth/me`) which purges local credentials, account-owned stories, shelf states, and reading stats.
- Unauthenticated `401` responses automatically clear stale local credentials and return the session state to `SignedOut` with an actionable message.
