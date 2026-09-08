# 02 Presentation Layer (Jetpack Compose)

## 1. UI Architecture & Design System

The user interface of Fable is built entirely in **Jetpack Compose** utilizing Material Design 3 (Material You). The application enforces strict Unidirectional Data Flow (UDF) across all screens.

```
┌────────────────────────────────────────────────────────┐
│                      ViewModel                         │
│  - Holds private MutableStateFlow<UiState>             │
│  - Exposes read-only StateFlow<UiState>                │
│  - Executes business logic via Domain Use Cases        │
└───────────────────────────▲────────────────────────────┘
         Events Up (User    │    State Down (Immutable
         Clicks, Input)     │    UiState Data Class)
┌───────────────────────────┴────────────────────────────┐
│                  Composable Screen                     │
│  - Collects state via collectAsStateWithLifecycle()   │
│  - Purely declarative rendering                        │
│  - Delegates UI events up via lambdas                  │
└────────────────────────────────────────────────────────┘
```

---

## 2. Navigation Hierarchy & Screen Structure

Navigation is managed via **Jetpack Compose Navigation** (`NavHost`):

1. **`LibraryScreen` (`fable://library`):**
   - Main landing screen. Displays books marked as favorites in a flexible 2:3 cover grid or list view.
   - Includes category tabs (e.g., "All", "Currently Reading", "Completed") and quick search filtering.
2. **`BookDetailScreen` (`fable://book/{bookId}`):**
   - Displays cover art, author, description, tags, and the full list of chapters.
   - Actions: "Start Reading", "Favorite / Bookmark", "Download All", and individual chapter download toggles.
3. **`ReaderScreen` (`fable://reader/{bookId}/{chapterId}`):**
   - The primary reading viewport. Features an immersive full-screen canvas supporting Paging or Continuous Scroll modes.
   - Tap-to-toggle overlay controls: Top app bar (back navigation, chapter title) and bottom scrubber (progress slider, settings bottom sheet).
4. **`HistoryScreen` (`fable://history`):**
   - Chronological timeline of recently read books and chapters, displaying reading progress percentage and a one-tap "Resume" button.
5. **`SourceCatalogScreen` (`fable://sources`):**
   - Displays available modular source extractors. Allows users to search remote websites and browse popular catalogs.
6. **`SettingsScreen` (`fable://settings`):**
   - Application preferences: Default reader theme, font choices, download storage management, and cache clearance.

---

## 3. Reader Themes & Ergonomic Palettes

Fable implements four dedicated reading themes:
- **Light Theme:** `#FAFAFA` surface with `#1C1B1F` typography for bright daylight environments.
- **Dark Theme:** `#121212` surface with `#E6E1E5` typography for low-light environments.
- **AMOLED Pure Black Theme:** `#000000` surface with `#CCCCCC` typography for maximum battery savings on OLED panels.
- **Sepia Paper Theme:** `#F4ECD8` surface with `#433422` typography to minimize blue-light eye strain during extended reading sessions.

---

## 4. Recomposition Optimization & Stability

To maintain a consistent 60fps frame rate (<16ms per frame):
- **Immutable UI State:** All UI state models are annotated with `@Immutable` or `@Stable`. Standard collections (`List<T>`) are wrapped or passed as immutable lists.
- **Derived State:** State variables derived from frequent events (such as scroll positions or reading percentages) are wrapped in `derivedStateOf`:
  ```kotlin
  val isScrolledPastHeader by remember {
      derivedStateOf { scrollState.value > 100 }
  }
  ```
- **Stable Item Keys:** In `LazyColumn`, `LazyRow`, and `HorizontalPager`, unique keys are always provided:
  ```kotlin
  items(items = chapters, key = { it.id }) { chapter ->
      ChapterItem(chapter = chapter, onClick = { onSelect(chapter.id) })
  }
  ```
- **Stateless Sub-Components:** Reusable UI components never instantiate or hold ViewModels. They accept plain primitive or domain arguments and emit standard lambda callbacks (`onClick: () -> Unit`).

