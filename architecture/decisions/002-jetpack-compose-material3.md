# ADR 002: Jetpack Compose & Material 3 Presentation Baseline

## Status
Accepted

## Context
Traditional Android XML layout architectures (View, Fragment, RecyclerView) require extensive boilerplate, complex ViewBinding/DataBinding setups, and separate theme declarations that increase maintenance overhead and slow down UI iteration. Fable requires:
- Dynamic, ergonomic reader theming (Light, Dark, AMOLED Pure Black, Sepia).
- Smooth interactive overlays (center-tap reader chrome, settings bottom sheets).
- High frame-rate scrolling and pagination (<16ms per frame).

## Decision
We chose **100% Jetpack Compose** paired with **Material Design 3** as the exclusive UI toolkit for Fable.
- All layouts, navigation destinations, and custom reader canvases are built using Composable functions.
- XML layout files and Fragments are completely eliminated from the codebase.
- Unidirectional Data Flow (UDF) is strictly enforced with ViewModels exposing immutable `StateFlow<UiState>` collected via `collectAsStateWithLifecycle()`.

## Consequences
- **Positive:** Dramatically reduced boilerplate; direct Kotlin expressiveness for complex custom gestures and animations.
- **Positive:** Simplified dynamic theming allowing immediate switching between Light, Dark, AMOLED, and Sepia palettes.
- **Negative:** Developers must rigorously enforce recomposition stability (annotating models with `@Immutable`, using `derivedStateOf`, and avoiding unstable collection parameters) to prevent UI frame drops.
