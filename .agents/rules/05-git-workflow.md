---
trigger: always_on
---

# Git Workflow & Version Control

All contributions to the Fable repository must adhere to the following Git workflow to ensure a clean, trackable, and safe history.

---

### 1. Feature Branching Protocol
- **Rule**: No pushing commits directly to `main`.
- **Practice**: All development must occur on dedicated feature branches (e.g., `feature/room-database`, `feature/reader-ui`, `fix/chapter-parser`).
- **Declaration**: The active branch must be declared and confirmed before beginning work on a feature.

---

### 2. Pull Request (PR) Integration Workflow
- **Rule**: Changes are merged into `main` via PRs after CI validation.
- **Practice**: Push the feature branch to the remote repository and open a Pull Request. Rely on the CI pipeline (`./gradlew testDebugUnitTest lintDebug`) to validate the code. Provide a clear, markdown-formatted, emoji-free PR Title and Body.

---

### 3. Concise Conventional Commit Formatting
- **Rule**: Commit messages must be concise, crisp, and follow conventional commit standards.
- **Practice**: Use standard prefixes:
  - `feat:` New user-facing or architectural capability.
  - `fix:` Bug fix or regression resolution.
  - `refactor:` Code restructuring without behavioral changes.
  - `test:` Adding or updating unit tests.
  - `docs:` Documentation or ADR additions.
  - `chore:` Dependency version updates or build script tweaks.

---

### 4. Atomic, Separated Commits (STRICT)
- **Rule**: Never bundle unrelated changes across architectural layers into a single large commit.
- **Practice**: Provide separate `git add` and `git commit` commands for each logical boundary:
  1. Commit for Database schema and entity models.
  2. Commit for Repository and Domain Use Cases.
  3. Commit for UI Composables and ViewModels.
  4. Commit for Unit and UI tests.

---

### 5. Secret & Build Artifact Safety
- **Rule**: Never commit private files or generated Android build artifacts.
- **Practice**: Strictly ignore `local.properties`, keystore files (`*.jks`, `*.keystore`), `.hprof` memory dumps, and the entire `build/` directory.

