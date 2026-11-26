# Fable Android UI/UX & Visual Design Specification

## 1. Visual Direction: Warm Editorial Minimalism + High-Contrast Dark Mode

Fable Android pairs a tactile warm-paper aesthetic in light mode with a WCAG AA high-contrast dark mode (`AppAppearance`: `SYSTEM`, `LIGHT`, `DARK`) and dedicated reading surfaces (`ReaderTheme`: `PAPER`, `SEPIA`, `NIGHT`).

### Global App Palettes (`ui/theme/Color.kt` & `Theme.kt`)

| Role | Light Mode (Warm Paper) | Dark Mode (WCAG AA Obsidian) |
|---|---|---|
| **Background** | `#FBF8F3` (`FableBackground`) | `#0B0F17` (`FableDarkBackground`) |
| **Elevated Surface** | `#F4EFE6` (`FableSurface`) | `#111824` (`FableDarkSurface`) |
| **Primary Ink / Text** | `#18181B` (`FableText`) | `#F1F5F9` (`FableDarkText`) |
| **Muted / Secondary Text** | `#52525B` (`FableMutedText`) | `#A5B4C8` (`FableDarkMutedText`) |
| **Primary Brand Accent** | `#3949AB` (`FablePrimary`) | `#7890FF` (`FableDarkPrimary`) |
| **Outline / Hairline** | `#E4DDD0` (`FableOutline`) | `#232F42` (`FableDarkOutline`) |

### Reader Surface Palettes (`ReaderTheme`)
- **Paper (`PAPER`)**: `#FBF8F3` canvas, `#1C1B18` ink.
- **Sepia (`SEPIA`)**: `#F3E9D2` canvas, `#2B2118` ink.
- **Night (`NIGHT`)**: `#0D1017` canvas, `#E5E7EB` ink.

### Passage Highlight Swatches (`HighlightColor`)
- **Terracotta (`TERRACOTTA`)**: `#D96B43`
- **Amber (`AMBER`)**: `#D49B27`
- **Sage (`SAGE`)**: `#4E8A6E`

---

## 2. Typography & Reader Ergonomics

- **Reader Font Families (`ReaderFont`)**:
  - `SERIF` (Default literary body)
  - `SANS` (Clean modern sans-serif)
  - `MONO` (Technical / typewriter monospace)
- **Adjustable Reader Metrics**:
  - Font size: `15sp` to `26sp` (preserved across paginated reflows via word-offset mapping).
  - Line height multiplier: `1.25x` to `1.9x`.
  - Per-session screen brightness: `Auto` (system default) or manual slider (`5%`–`100%`).
- **Touch Targets & Accessibility**:
  - Interactive controls maintain a minimum `48dp` touch target.
  - Comic zoom includes both multi-touch pinch/double-tap gestures and an accessible `"Zoom"` / `"250% · Reset"` HUD pill.
  - Password fields include explicit show/hide visibility toggles.
