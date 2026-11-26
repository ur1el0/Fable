---
trigger: always_on
---

# Enterprise Architecture Standards for Fable

These guidelines apply across the entire Fable codebase to enforce clean architecture, offline-first reliability, and strict layer isolation.

---

### 1. Decision & Evidence Pattern (ADRs)
- **Rule**: Document key architectural trade-offs, solutions, and library choices in architecture decision records.
- **Practice**: Maintain an `architecture/decisions/` directory containing structured `.md` files detailing offline-first Room decisions, Jetpack Compose adoption, shared `/api/v1/` backend contracts (ADR 009), and format-aware discovery/reading (ADR 010).

---

### 2. Offline-First Single Source of Truth
- **Rule**: The local Room database (`FableDatabase`, schema v6) is the single source of truth for cached stories, chapters, shelf states, reading stats, writer drafts, and passage highlights.
- **Practice**:
  - UI components and ViewModels observe Room database queries via Kotlin `Flow`.
  - When remote stories or chapters are fetched from `FableApiService` or `MangaApiService`, they are persisted and reconciled into Room (`coalesceChapters` preserves non-empty cached chapters when summary responses return blank content).
  - The UI automatically updates via reactive database Flow emissions.

---

### 3. Clean Architecture Layer Boundaries
- **Rule**: Maintain strict layer separation: Presentation $\rightarrow$ Domain $\leftarrow$ Data.
- **Practice**:
  - **Presentation Layer (`ui/`)**: Contains Jetpack Compose UI, typed navigation (`FableNavigation.kt`), focused ViewModels (`CatalogViewModel`, `ExploreViewModel`, `ReaderViewModel`, `StoryEditorViewModel`, `WriterDraftViewModel`, `AccountViewModel`, `ServerConfigViewModel`), and UI state models. Never touches Room DAOs or network services directly.
  - **Domain Layer (`domain/`)**: Pure Kotlin. Contains business models, repository interfaces, and deterministic reading/search/zoom calculation engines (`ReaderTextPaginator`, `ReaderProgressMapper`, `InBookSearchEngine`, `HighlightExporter`, `ComicZoomMath`).
  - **Data Layer (`data/`)**: Implements repository interfaces, manages Room database entities/DAOs (`v1`–`v6` migrations), and communicates with the shared Fable API via `FableHttpClient`.

---

### 4. Unidirectional Data Flow (UDF)
- **Rule**: State flows down from ViewModels to Composables; events flow up from Composables to ViewModels.
- **Practice**:
  - ViewModels expose immutable `StateFlow` state streams to the UI.
  - ViewModels expose distinct user event functions.
  - Composables are stateless consumers receiving state and passing events up via lambda callbacks.

---

### 5. Modularity & Domain-Driven Design (No God Files)
- **Rule**: Prevent overgrown monolithic files ("God Files").
- **Practice**:
  - Decompose screens and reader sheets into focused components under `ui/screens/components/`.
  - Keep ViewModels scoped to a single domain area rather than reintroducing a monolithic app-wide ViewModel.
  - Keep Room DAOs focused on specific entity domains (`StoryDao`, `ReadingStatsDao`, `WriterDraftDao`, `HighlightDao`).
