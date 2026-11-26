# 07 Reader Engine & Rendering Pipeline

## 1. Format-Aware Reader Dispatch (`StoryReaderScreen`)

`StoryReaderScreen.kt` inspects `story.contentFormat` and dispatches rendering to the appropriate engine while sharing a unified Chrome HUD (`ReaderControlsOverlay`), table-of-contents drawer (`ReaderChaptersSheet`), and display preferences sheet (`ReaderDisplayOptionsSheet`):

- **`ContentFormat.PROSE`**:
  - **Continuous Scroll Mode (`ContinuousChapterList.kt`)**: Virtualized `LazyColumn` rendering chapter headers, paragraph items with inline highlight spans (`buildHighlightedParagraph`), long-press passage selection, and chapter footer navigation.
  - **Paginated Book Mode (`PaginatedChapterPager.kt`)**: `HorizontalPager` backed by `ReaderTextPaginator.paginateParagraphs()`, slicing chapter paragraphs into balanced screen-sized pages (`ReaderPageSlice`) based on current font size, line height, and viewport constraints.
- **`ContentFormat.COMIC`**:
  - **Manga & Webtoon Engine (`MangaChapterReader.kt`)**: Supports seamless vertical strip scrolling (`ComicReaderLayout.WEBTOON`) and horizontal page flipping (`ComicReaderLayout.MANGA`) with two-finger pinch-to-zoom (`1x`–`4x`), bounded panning, center double-tap zoom (`1x <-> 2.5x`), lazy fixed-aspect placeholders, per-page error retry, and end-of-chapter transition banners.

---

## 2. Word-Offset Progress Preservation (`ReaderProgressMapper`)

Switching between `SCROLL` and `PAGINATED` layouts or resizing typography (`fontSizeSp`, `lineHeightMultiplier`) does not lose the reader's position:

1. Every paginated `ReaderPageSlice` records its starting global word offset (`firstWordIndex`) and `wordCount` within the chapter.
2. When font metrics change in `PAGINATED` mode, `StoryReaderScreen` captures the active `anchorWordIndex`, re-paginates the chapter, and maps `anchorWordIndex` back to the new target page via `ReaderProgressMapper.pageIndexForWordOffset()`.
3. Reading progress (`0.0f..1.0f`) and `lastReadChapterIndex` are debounced (`350ms`) in `ReaderViewModel` before persisting to `shelf_states` in Room.

---

## 3. In-Book Search, Passage Highlights & Quote Cards

- **In-Book Search (`InBookSearchEngine.kt` + `ReaderSearchSheet`)**:
  - Searches across all hydrated chapters of a prose story, returning case-insensitive matches with chapter titles, paragraph indices, word offsets, and surrounding context snippets.
  - Tapping a search result jumps directly to the target chapter and scrolls/pages to the matching paragraph.
- **Passage Range Selection & Room Highlights (`ReaderAnnotationSheets.kt`)**:
  - Long-pressing any paragraph opens `PassageHighlightEditorSheet` with **Start word** and **End word** steppers (`InBookSearchEngine.tokenizeWords` / `sliceWordRange`), 3 editorial highlight swatches (`TERRACOTTA`, `AMBER`, `SAGE`), and an optional note field.
  - Highlights persist in `story_highlights` (`FableDatabase` v6) and render inline via `AnnotatedString` background/underline spans (`buildHighlightedParagraph`).
- **Highlight Export & 1080px PNG Quote Cards (`HighlightExporter.kt`, `QuoteCardExporter.kt`)**:
  - Users can copy or share all highlights for a book as Markdown or plain text.
  - Any highlight can be previewed in `QuoteCardShareSheet` and exported as a 1080px high-resolution PNG quote card rendered via Android `Canvas` and `StaticLayout`, shared safely through `androidx.core.content.FileProvider`.

---

## 4. Per-Session Screen Brightness (`ReaderWindowBrightnessEffect`)

`ReaderDisplayOptionsSheet` includes a screen brightness slider (`5%`–`100%`) and an **Auto** chip (`readerBrightness = null`). `ReaderWindowBrightnessEffect` in `StoryReaderScreen.kt` applies the override to ` activity.window.attributes.screenBrightness` while reading and restores the original window brightness on disposal.
