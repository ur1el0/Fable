# 07 Reader Engine & Content Rendering

## 1. Dual-Mode Reading Engine

The core reader in Fable supports two distinct reading modes to accommodate varied user reading styles across literature and comics:

### 1.1 Paginated Mode (`ReaderLayout.PAGINATED`)
- **Implementation:** Built using Jetpack Compose `HorizontalPager`.
- **Pagination Logic:** Chapter text is segmented into screen-bounded pages via `ReaderPagePacker` based on character packing limits, font scale multipliers, and layout boundaries.
- **Gesture Navigation:** Horizontal swipe or tapping left/right edge zones navigates between pages.
- **Progress Restoration:** `ReaderProgressMapper.pageIndexForProgress()` deterministically maps stored reading percentages to exact page indices when reopening chapters.

### 1.2 Continuous Scroll Mode (`ReaderLayout.SCROLL`)
- **Implementation:** Built using Compose `LazyColumn` via `ContinuousChapterList`.
- **Infinite Chapter Chaining:** Seamlessly pre-loads the next chapter when scrolling near chapter boundaries.
- **Scroll Position Restoration:** Restores scroll offset using `ReaderProgressMapper.scrollOffsetForProgress()`.
- **Scroll Physics:** Native Android friction-based fling scrolling with smooth kinetic physics.

---

## 2. Typographic Customization & Reader Preferences

The reader engine exposes fine-grained typographic controls persisted in `SharedPreferencesReaderPreferencesRepository`:

```kotlin
data class ReaderPreferences(
    val font: ReaderFont = ReaderFont.SERIF,
    val fontScalePercent: Int = 100, // 80% to 150%, 5% step
    val theme: ReaderTheme = ReaderTheme.SEPIA,
    val lineSpacing: ReaderLineSpacing = ReaderLineSpacing.NORMAL,
    val layout: ReaderLayout = ReaderLayout.SCROLL
)
```

- **Font Families (`ReaderFont`):**
  - `SERIF`: Serif typography modeled for classic literary immersion.
  - `SANS`: Clean, contemporary sans-serif.
  - `ROUNDED`: Soft, friendly typography.
  - `MONO`: Monospaced typography for technical excerpts.
- **Themes (`ReaderTheme`):**
  - `WHITE`: Stark clean high-key daylight reading.
  - `PAPER`: Soft textured paper background.
  - `SEPIA`: Warm amber/cream parchment tone (default).
  - `CHARCOAL`: Soft dark gray for low-light environments.
  - `MIDNIGHT`: Deep navy dark mode.
  - `OLED`: Pure `#000000` AMOLED zero-luminance background.
- **Line Spacing (`ReaderLineSpacing`):**
  - `COMPACT` (1.2x), `NORMAL` (1.5x), `SPACIOUS` (1.8x).

---

## 3. Reading Progress & Session Outbox Persistence

To ensure zero UI jank and avoid SQLite write thrashing:
1. **Debounced Local Progress:** Reading progress events are debounced before updating `shelf_states` in Room.
2. **Deterministic Progress Mapping (`ReaderProgressMapper`):** Converts between overall story progress percentage ($0-100$) and active chapter progress percentage:
   $$\text{overallPercent} = \left\lfloor \frac{(\text{chapterNumber} - 1) + \text{chapterPercent} / 100}{\text{totalChapters}} \times 100 \right\rfloor$$
3. **Reading Session Recording:** When the reader pauses or exits, the elapsed time is recorded in `reading_sessions` (`ReadingSessionEntity`) with timestamp and completion status, queued for background synchronization with the shared API.

---

## 4. Viewport Virtualization & Memory Hygiene

- **Compose Viewport Virtualization:** `LazyColumn` and `HorizontalPager` virtualize off-screen text items so 50,000+ word novels consume minimal heap memory (<20MB resident).
- **Decoupled Bitmap Loading:** Cover illustrations load via Coil with bounded memory caches, avoiding Out-Of-Memory (OOM) crashes on low-end devices.

