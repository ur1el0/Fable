---
trigger: always_on
---

# Testing Protocol & Quality Assurance

Fable enforces a strict testing discipline across the domain, repository, database, and UI layers to ensure offline reliability and prevent regressions.

---

### 1. Domain & Use Case Testing
- **Rule**: Every domain Use Case must be covered by reproducible JVM unit tests.
- **Practice**:
  - Use `JUnit 4` or `JUnit 5` running on the JVM without requiring an Android emulator.
  - Leverage test fakes (`FakeBookRepository`) over complex mocking libraries to keep tests fast, readable, and refactor-safe.
  - Verify both successful execution and domain error paths.

---

### 2. Coroutines & Reactive Flow Testing
- **Rule**: Never use `Thread.sleep()` or unpredictable delays in asynchronous tests.
- **Practice**:
  - Use `kotlinx.coroutines.test.runTest` and `StandardTestDispatcher` for deterministic coroutine testing.
  - Use the **Turbine** library (`app.cash.turbine:turbine`) to assert Flow emissions:
    ```kotlin
    @Test
    fun `reading progress updates emit in chronological order`() = runTest {
        repository.observeProgress(bookId = "book_1").test {
            assertEquals(initialProgress, awaitItem())
            repository.saveProgress(newProgress)
            assertEquals(newProgress, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
    ```

---

### 3. In-Memory Room Database Testing
- **Rule**: Verify DAO operations and SQLite transactions using in-memory databases.
- **Practice**:
  - Instantiate Room using `Room.inMemoryDatabaseBuilder(context, FableDatabase::class.java).allowMainThreadQueries().build()`.
  - Validate foreign key cascading, index performance, and transaction rollbacks on error.

---

### 4. Source Extractor DOM Fixture Testing
- **Rule**: Never run extractor unit tests against live external websites during CI builds.
- **Practice**:
  - Save static HTML response snapshots in `src/test/resources/fixtures/` (e.g., `sample_novel_page.html`, `sample_chapter_list.html`).
  - Feed these static HTML fixtures into Jsoup extractors to assert selector parsing correctness without network dependency.

---

### 5. Compose UI Component Testing
- **Rule**: Complex interactive components must have UI tests verifying state rendering.
- **Practice**:
  - Use `createComposeRule()` to test stateless Composables.
  - Assert element presence, semantics tags, and click callback execution.

