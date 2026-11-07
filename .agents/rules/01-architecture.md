---
trigger: always_on
---

# Enterprise Architecture Standards for Fable

These guidelines apply across the entire Fable codebase to enforce clean architecture, offline-first reliability, and strict layer isolation.

---

### 1. Decision & Evidence Pattern (ADRs)
- **Rule**: Document key architectural trade-offs, solutions, and library choices in architecture decision records.
- **Practice**: Maintain an `architecture/decisions/` directory containing structured `.md` files detailing offline-first decisions, Jetpack Compose adoptions, WorkManager pipelines, and source extraction contracts.

---

### 2. Offline-First Single Source of Truth
- **Rule**: The local Room database is the single source of truth for the entire application.
- **Practice**:
  - UI components and ViewModels observe Room database queries via Kotlin `Flow`.
  - When remote content, books, or chapters are fetched from a source extractor, they must be written into the local Room database and internal file storage first.
  - The UI automatically updates via reactive database Flow emissions. Never pass remote network responses directly into UI state without caching them locally.

---

### 3. Clean Architecture Layer Boundaries
- **Rule**: Maintain strict layer separation: Presentation $\rightarrow$ Domain $\leftarrow$ Data.
- **Practice**:
  - **Presentation Layer (`ui/`)**: Contains Jetpack Compose UI, ViewModels, and UI state models. Never touches Room DAOs or network extractors directly.
  - **Domain Layer (`domain/`)**: Pure Kotlin. Contains business models, repository interfaces, and Use Cases. Must have zero dependencies on Android framework classes (`android.*`).
  - **Data Layer (`data/`)**: Implements repository interfaces, manages Room database entities and DAOs, interacts with OkHttp/Jsoup source extractors, and reads/writes internal files.

---

### 4. Unidirectional Data Flow (UDF)
- **Rule**: State flows down from ViewModels to Composables; events flow up from Composables to ViewModels.
- **Practice**:
  - ViewModels expose a single immutable `StateFlow<UiState>` to the UI.
  - ViewModels expose distinct user event functions (e.g., `onChapterSelected(chapterId)`, `onToggleFavorite()`).
  - Composables are stateless consumers receiving state and passing events up via lambda callbacks.

---

### 5. Modularity & Domain-Driven Design (No God Files)
- **Rule**: Prevent overgrown monolithic files ("God Files").
- **Practice**:
  - When a Composable screen exceeds ~250 lines, break it down into dedicated sub-components within a `components/` directory (e.g., `ReaderTopBar.kt`, `ReaderSettingsSheet.kt`, `BookGridItem.kt`).
  - Keep ViewModels focused purely on UI state management and delegate complex business rules to domain Use Cases.
  - Keep Room DAOs focused on specific entities rather than creating a single massive database DAO.

---

### 6. Provider Abstraction & Isolation
- **Rule**: Keep third-party libraries hidden behind domain interfaces.
- **Practice**: Isolate libraries such as Coil, Jsoup, and OkHttp behind repository implementations or dedicated utility classes. Ensure domain models remain independent of external library types.

