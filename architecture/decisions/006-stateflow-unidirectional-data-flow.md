# ADR 006: StateFlow & Unidirectional Data Flow (UDF)

## Status
Accepted

## Context
Asynchronous state management across asynchronous database flows, background download progress updates, and rapid user scrolling can lead to race conditions, multiple conflicting state sources, and memory leaks if not strictly standardized.

## Decision
We chose **Kotlin Coroutines `StateFlow`** as the standardized state container across all ViewModels, coupled with Unidirectional Data Flow (UDF):
- Each ViewModel maintains a private `_uiState = MutableStateFlow<UiState>(...)` and exposes a public read-only `uiState: StateFlow<UiState> = _uiState.asStateFlow()`.
- UI Composables observe state strictly via `androidx.lifecycle.compose.collectAsStateWithLifecycle()`.
- User interactions are passed upwards as discrete event callbacks (e.g., `onChapterSelected(id)`, `onFontSizeChanged(scale)`).
- One-off events (e.g., displaying a snackbar or triggering haptic feedback) are modeled via `SharedFlow` or single-event channels.

## Consequences
- **Positive:** Guaranteed state consistency; the UI always renders the exact current state emitted by the ViewModel.
- **Positive:** Lifecycle safety; `collectAsStateWithLifecycle()` automatically suspends flow collection when the app moves into the background, preventing CPU battery drain.
- **Negative:** Requires disciplined modeling of immutable UI state data classes for every screen.
