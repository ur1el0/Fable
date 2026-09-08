# 07 Reader Engine & Content Rendering

## 1. Dual-Mode Reading Engine

The core reader in Fable supports two distinct reading modes to accommodate varied user reading styles:

### 1.1 Paging Mode (Horizontal Book Experience)
- **Implementation:** Built using Compose `HorizontalPager`.
- **Pagination Logic:** Chapter text is segmented into screen-bounded pages based on viewport height and measured font metrics.
- **Gesture Tap Zones:**
  - **Left 33%:** Navigate to previous page.
  - **Center 33%:** Toggle reader controls overlay (app bar, bottom scrubber).
  - **Right 33%:** Navigate to next page.

### 1.2 Continuous Scroll Mode (Web/Scroll Experience)
- **Implementation:** Built using Compose `LazyColumn`.
- **Infinite Chapter Chaining:** When the user scrolls near the end of the current chapter, the next chapter's content is pre-loaded seamlessly at the bottom of the list.
- **Scroll Physics:** Native Android friction-based fling scrolling with smooth kinetic physics.

---

## 2. Typography & Layout Customization

The reader engine exposes fine-grained typographic controls:

```kotlin
data class ReaderTypographySettings(
    val fontSizeSp: Int = 16,
    val lineHeightMultiplier: Float = 1.5f,
    val paragraphSpacingDp: Int = 12,
    val horizontalMarginDp: Int = 20,
    val fontFamilyType: ReaderFont = ReaderFont.SERIF,
    val themePalette: ReaderTheme = ReaderTheme.SEPIA
)
```

- **Font Families:**
  - `ReaderFont.SERIF`: Literata / Merriweather for classic book novel typography.
  - `ReaderFont.SANS`: Inter / Roboto for modern clarity.
  - `ReaderFont.MONO`: Monospace for technical excerpts or dialogues.

---

## 3. Reading Progress Persistence & Debouncing

During rapid scrolling, writing progress to SQLite on every frame causes disk I/O thrashing and UI jank. Fable enforces a **500ms debounce** pipeline:

```kotlin
class ReaderViewModel(
    private val saveReadingProgressUseCase: SaveReadingProgressUseCase
) : ViewModel() {
    private val progressEvents = MutableSharedFlow<ReadingProgress>()

    init {
        viewModelScope.launch {
            progressEvents
                .debounce(500L) // Debounce rapid scroll updates
                .distinctUntilChanged()
                .collect { progress ->
                    saveReadingProgressUseCase(progress)
                }
        }
    }

    fun onScrollPositionChanged(bookId: String, chapterId: String, offset: Int) {
        progressEvents.tryEmit(ReadingProgress(bookId, chapterId, 0, offset, System.currentTimeMillis()))
    }
}
```

---

## 4. Memory Management & Viewport Virtualization

- **Lazy Layout Recycling:** Compose `LazyColumn` and `HorizontalPager` virtualize off-screen elements, ensuring that chapters containing 20,000+ words consume minimal RAM.
- **Immediate DOM Garbage Collection:** The reader never binds raw Jsoup `Document` trees to Composables. Parsed text is converted to plain strings or lightweight `AnnotatedString` structures immediately upon chapter load.

