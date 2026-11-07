---
trigger: always_on
---

# Jetpack Compose UI Standards

These guidelines govern all UI implementation in Fable to ensure 60fps rendering, battery efficiency, and accessible user ergonomics.

---

### 1. Purely Declarative & Stateless Composables
- **Rule**: Keep Composable functions purely declarative and stateless whenever possible.
- **Practice**:
  - Separate stateful screen containers from stateless visual components.
  - Stateful screen: Retrieves ViewModel, collects UI state using `collectAsStateWithLifecycle()`, and binds event handlers.
  - Stateless component: Accepts state parameters and emits event callbacks (`onClick: () -> Unit`). Never inject ViewModels into child components.

---

### 2. Recomposition Optimization & Stability
- **Rule**: Prevent unnecessary recompositions to maintain 60fps scrolling and reduce battery consumption.
- **Practice**:
  - Annotate custom UI state data classes with `@Immutable` or `@Stable`.
  - Avoid passing standard `List<T>` or mutable collections as Composable parameters; wrap them in immutable structures or annotate containing classes with `@Immutable`.
  - Use `derivedStateOf` when calculating derived values that change frequently (e.g., scroll offset thresholds, reading progress percentages) to prevent recomposition on every pixel of scroll.
  - In `LazyColumn`, `LazyRow`, and `HorizontalPager`, always provide a stable and unique key:
    ```kotlin
    items(items = chapters, key = { it.id }) { chapter ->
        ChapterListItem(chapter = chapter, onClick = { onChapterClick(chapter.id) })
    }
    ```

---

### 3. Lifecycle-Aware Flow Collection
- **Rule**: Never use standard `collectAsState()` on UI screens.
- **Practice**: Always use `collectAsStateWithLifecycle()` from `androidx.lifecycle.compose`:
  ```kotlin
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  ```
  This automatically pauses coroutine flow collection when the app moves into the background, preventing unnecessary CPU wakefulness and memory retention.

---

### 4. Reader Ergonomics & Accessibility (a11y)
- **Rule**: UI components must support prolonged reading and comply with mobile accessibility baselines.
- **Practice**:
  - All clickable controls (icon buttons, menu actions, chapter list items) must meet the standard **48x48dp minimum touch target**.
  - All icons and cover images must provide meaningful `contentDescription` strings for screen readers (or explicitly pass `null` for purely decorative elements).
  - Support Material 3 dynamic theming, high-contrast dark modes, and pure black AMOLED backgrounds.

---

### 5. Compose Preview Standards
- **Rule**: Every reusable UI component must include isolated Compose previews.
- **Practice**: Provide `@Preview` annotations covering both Light and Dark themes, with synthetic preview data providers. Ensure previews render cleanly without requiring a live Android device or Room database instance.

