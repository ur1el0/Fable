# 04 Database Schema & Persistence (Room SQLite)

## 1. RDBMS Strategy: Room Persistence Library

Fable uses **AndroidX Room** over an embedded **SQLite** engine as the single source of truth for the entire application. Room provides compile-time query verification via KSP, robust reactive `Flow` observation, and transactional integrity.

---

## 2. Table Schemas & Room Entities

### 2.1 Table: `books` (`BookEntity`)
Stores metadata and user library flags for all tracked novels and books.

| Column | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `TEXT` | PRIMARY KEY, NOT NULL | Unique composite ID (e.g., `"${sourceId}_${slug}"`) |
| `title` | `TEXT` | NOT NULL | Title of the work |
| `author` | `TEXT` | NOT NULL | Author or creator name |
| `cover_url` | `TEXT` | NULL | Remote or cached URL for the cover illustration |
| `description` | `TEXT` | NULL | Synopsis or blurb |
| `source_id` | `TEXT` | NOT NULL | Identifier of the source extractor origin |
| `is_favorite` | `INTEGER` | NOT NULL, DEFAULT 0 | Library bookmark flag (1 = in library, 0 = browsing cache) |
| `total_chapters`| `INTEGER` | NOT NULL, DEFAULT 0 | Count of discovered chapters |
| `created_at` | `INTEGER` | NOT NULL | Timestamp when book was first saved |
| `last_read_at` | `INTEGER` | NULL | Timestamp when book was last opened |

**Indices:**
- `CREATE INDEX index_books_source_id ON books(source_id);`
- `CREATE INDEX index_books_is_favorite ON books(is_favorite);`
- `CREATE INDEX index_books_last_read_at ON books(last_read_at);`

---

### 2.2 Table: `chapters` (`ChapterEntity`)
Stores individual chapter metadata and read/download states.

| Column | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `TEXT` | PRIMARY KEY, NOT NULL | Unique chapter ID (e.g., `"${bookId}_ch_${num}"`) |
| `book_id` | `TEXT` | NOT NULL, FK (`books.id`), ON DELETE CASCADE | Associated book reference |
| `title` | `TEXT` | NOT NULL | Chapter display title |
| `chapter_number`| `REAL` | NOT NULL | Numeric sequence ordering (supports floats like 10.5) |
| `url` | `TEXT` | NOT NULL | Source URL for web content extraction |
| `is_read` | `INTEGER` | NOT NULL, DEFAULT 0 | Completion flag (1 = completed) |
| `download_state`| `TEXT` | NOT NULL, DEFAULT 'NOT_DOWNLOADED' | `'NOT_DOWNLOADED'`, `'QUEUED'`, `'DOWNLOADING'`, `'DOWNLOADED'` |
| `release_date` | `INTEGER` | NULL | Publication timestamp |

**Indices & Foreign Keys:**
```kotlin
@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["book_id"]),
        Index(value = ["chapter_number"])
    ]
)
```

---

### 2.3 Table: `categories` (`CategoryEntity`) & `book_categories` (`BookCategoryCrossRef`)
Supports user-defined library categories (e.g., "Currently Reading", "Plan to Read").

- **`categories` Table:**
  - `id`: `INTEGER PRIMARY KEY AUTOINCREMENT`
  - `name`: `TEXT NOT NULL UNIQUE`
  - `sort_order`: `INTEGER NOT NULL DEFAULT 0`
- **`book_categories` (Cross-Reference Table):**
  - `book_id`: `TEXT NOT NULL, FK(books.id) ON DELETE CASCADE`
  - `category_id`: `INTEGER NOT NULL, FK(categories.id) ON DELETE CASCADE`
  - `PRIMARY KEY (book_id, category_id)`

---

### 2.4 Table: `reading_history` (`HistoryEntity`)
Tracks exact checkpoint progress within a book.

| Column | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `book_id` | `TEXT` | PRIMARY KEY, FK (`books.id`), ON DELETE CASCADE | Associated book |
| `chapter_id` | `TEXT` | NOT NULL | Active chapter ID |
| `page_index` | `INTEGER` | NOT NULL, DEFAULT 0 | Page index in Paging Mode |
| `scroll_offset` | `INTEGER` | NOT NULL, DEFAULT 0 | Exact pixel scroll offset in Continuous Scroll Mode |
| `last_read_time`| `INTEGER` | NOT NULL | Checkpoint update timestamp |

---

### 2.5 Table: `downloads` (`DownloadEntity`)
Manages the active chapter download queue.

| Column | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `chapter_id` | `TEXT` | PRIMARY KEY, FK (`chapters.id`), ON DELETE CASCADE | Target chapter |
| `book_id` | `TEXT` | NOT NULL | Associated book ID |
| `status` | `TEXT` | NOT NULL | `'QUEUED'`, `'DOWNLOADING'`, `'COMPLETED'`, `'FAILED'` |
| `progress` | `INTEGER` | NOT NULL, DEFAULT 0 | Percentage progress (0-100) |
| `file_path` | `TEXT` | NULL | Relative path within internal storage sandbox |
| `queued_at` | `INTEGER` | NOT NULL | Queue entry timestamp |

---

## 3. Schema Migrations & KSP Export

Room is configured to export schema definitions to `app/schemas/`:
```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```
All entity modifications require an explicit `Migration(from, to)` implementation verifying SQLite table alterations before shipping production updates.

