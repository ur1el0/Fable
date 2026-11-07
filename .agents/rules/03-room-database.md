---
trigger: always_on
---

# Room Database & Persistence Guidelines

These guidelines ensure transactional safety, query optimization, and schema migration integrity across Fable's local SQLite database.

---

### 1. Reactive DAO Query Returns
- **Rule**: All observable queries must return Kotlin Coroutines `Flow`.
- **Practice**:
  - DAO queries that supply data to the UI layer must return `Flow<T>` or `Flow<List<T>>`:
    ```kotlin
    @Query("SELECT * FROM books WHERE isFavorite = 1 ORDER BY lastReadTime DESC")
    fun getFavoriteBooks(): Flow<List<BookEntity>>
    ```
  - Room automatically offloads Flow query execution and table change observation to its internal background dispatcher.

---

### 2. Suspend Functions for Database Mutations
- **Rule**: All database write, insert, update, and delete operations must be declared as `suspend` functions.
- **Practice**:
  - Never call synchronous DAO methods on the Main (UI) thread:
    ```kotlin
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)
    
    @Update
    suspend fun updateProgress(progress: HistoryEntity)
    ```

---

### 3. Transaction Safety & Batch Operations
- **Rule**: Never execute multi-table mutations or interdependent database updates outside a transaction.
- **Practice**:
  - Annotate multi-row operations with `@Transaction`:
    ```kotlin
    @Transaction
    suspend fun insertBookWithChapters(book: BookEntity, chapters: List<ChapterEntity>) {
        insertBook(book)
        insertChapters(chapters)
    }
    ```

---

### 4. Strict Schema Migrations & Integrity
- **Rule**: No unversioned schema alterations.
- **Practice**:
  - Increment `@Database(version = N)` whenever modifying entity columns, tables, or indices.
  - Provide an explicit `Migration(from, to)` implementation verifying integrity before shipping updates.
  - Enable schema export in Gradle:
    ```kotlin
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
    ```
  - Commit exported schema JSON files into version control to track schema evolution over time.

---

### 5. Foreign Key Constraints & Indexing
- **Rule**: Always define indices on foreign keys and frequently queried columns.
- **Practice**:
  - In `ChapterEntity`, index `bookId` and configure cascade deletion:
    ```kotlin
    @Entity(
        tableName = "chapters",
        foreignKeys = [
            ForeignKey(
                entity = BookEntity::class,
                parentColumns = ["id"],
                childColumns = ["bookId"],
                onDelete = ForeignKey.CASCADE
            )
        ],
        indices = [Index(value = ["bookId"])]
    )
    ```

