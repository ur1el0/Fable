# Fable - Design & Aesthetics

## 1. Visual Aesthetics
- **Native Material 3 Modernity**: Fable follows modern Android Material Design 3 guidelines with expressive color tones, tonal elevation, and smooth dynamic shapes (`CornerSize(16.dp)`).
- **Dedicated Reading Palettes**:
  - **Light Mode**: High-contrast, clean off-white background (`#FAFAFA`) with deep charcoal text (`#1C1B1F`) to reduce glare.
  - **Dark Mode**: Material 3 dark surface (`#121212`) with softened white text (`#E6E1E5`).
  - **AMOLED Mode**: Pure pitch black (`#000000`) designed for OLED power savings and deep contrast.
  - **Sepia / Paper Mode**: Warm parchment background (`#F4ECD8`) with warm espresso text (`#433422`) for eye comfort during prolonged reading sessions.

## 2. Reader Ergonomics & Interaction Design
- **Immersive Edge-to-Edge**: The reading canvas utilizes 100% of the screen. System status bar and navigation bar insets are respected during active controls, but smoothly fade away when reading.
- **Center-Tap Control Overlay**: Tapping the center 33% of the reading area toggles reader chrome (top app bar, bottom progress scrubber, settings trigger). Tapping the left/right 33% advances or reverses pages in Paging mode.
- **Micro-Animations & Transitions**:
  - Chapter transitions use subtle horizontal slide or cross-fade animations (`tween( durationMillis = 200)`).
  - Bottom sheets for reader settings animate smoothly without layout jank or dropped frames.
- **Loading & Empty States**:
  - Chapter content loading displays subtle shimmering skeleton text lines rather than harsh spinning wheels.
  - Library empty states provide pleasant vector illustrations encouraging the user to explore or import books.

## 3. Component Standards
- **Book Cover Cards**: Standard 2:3 book aspect ratio with subtle rounded corners (`12.dp`), clipped cover art via Coil, and an optional gradient scrim for title overlays.
- **Chapter List Items**: Clean 56dp minimum height list items with explicit status indicators:
  - Unread: Bold font weight, high-contrast text.
  - Read: Dimmed text color (`onSurfaceVariant`).
  - Downloaded: Subtle download checkmark badge.
- **Reader Settings Bottom Sheet**: Compact, accessible bottom sheet containing quick toggles for font family, text scale slider, line spacing, margins, and color palette selectors.
- **Touch Target Integrity**: All interactive buttons, icon buttons, and list items must satisfy the minimum 48x48dp touch target standard.

## 4. Typography Standards
- **Font Options**:
  - **Sans-Serif**: Android Roboto / Inter for clean modern UI.
  - **Serif**: Literata / Merriweather for comfortable long-form novel reading.
  - **Monospace**: For system dialogues, metadata, or code excerpts.
- **Ergonomic Tuning**:
  - Font size adjustable from 12sp to 32sp.
  - Line height multiplier adjustable between 1.2x and 2.2x.
  - Paragraph spacing adjustable between 0dp and 24dp.
  - Horizontal margin padding adjustable between 12dp and 36dp.

