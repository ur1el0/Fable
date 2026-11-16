# 04 Database Schema & Persistence (Room SQLite)

## Current Android Schema (Version 3)

Fable Android uses **AndroidX Room** over an embedded **SQLite** database as the single source of truth for the client. The database resides at Schema Version 3 across 5 normalized tables:
- `stories`: Caches catalog and discovered metadata across all source providers (`FABLE_ORIGINAL`, `GUTENBERG`, `STANDARD_EBOOKS`, `MANGADEX`).
- `chapters`: Stores individual chapter text, word counts, and manga image page URLs, with a foreign-key cascade link to `stories.id`.
- `shelf_states`: Stores account-scoped shelf state (bookmarks, completion flags, reading progress percentage, current page, and last-read chapter pointers) partitioned by `(ownerId, storyId)`.
- `reading_sessions`: Outbox of recorded reading durations (seconds read, timestamp, completion) pending synchronization with the backend.
- `reading_stats`: Caches account-scoped aggregate reading analytics (stories read count, total minutes read, consecutive day streak).

The schema is versioned, exported via KSP to `app/schemas/`, and backed by verified automated migrations (`MIGRATION_1_2` and `MIGRATION_2_3`).

---

## 1. Table Schemas & Room Entities

### 1.1 Table: `stories` (`StoryEntity`)
Stores catalog metadata, cover URLs, editorial badges, and source provider identity.

| Column | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `TEXT` | PRIMARY KEY, NOT NULL | Unique composite or UUIDv5 identifier (e.g. `gutenberg:123`, `standard-ebooks:slug`) |
| `title` | `TEXT` | NOT NULL | Title of the literary work |
| `author` | `TEXT` | NOT NULL | Author or creator attribution |
| `genre` | `TEXT` | NOT NULL | Genre category string |
| `chapter` | `TEXT` | NOT NULL | Formatted chapter subtitle / tag |
| `synopsis` | `TEXT` | NOT NULL | Narrative summary or blurb |
| `content` | `TEXT` | NOT NULL | Embedded text or introductory body |
| `readTimeMinutes` | `INTEGER`| NOT NULL | Calculated reading duration |
| `createdAtUtc` | `TEXT` | NOT NULL | ISO-8601 creation timestamp |
| `updatedAtUtc` | `TEXT` | NOT NULL | ISO-8601 update timestamp |
| `coverImageName` | `TEXT` | NULL | Local bundled cover asset name |
| `heroImageName` | `TEXT` | NULL | Local bundled hero banner name |
| `coverImageUrl` | `TEXT` | NULL | Remote HTTP cover image URL |
| `totalPages` | `INTEGER`| NOT NULL | Calculated pagination page count |
| `currentPage` | `INTEGER`| NOT NULL | Local device fallback page index |
| `progressPercent` | `INTEGER`| NOT NULL | Local device fallback progress percentage (0-100) |
| `rating` | `REAL` | NULL | Aggregate editorial rating |
| `savesCount` | `TEXT` | NOT NULL | Human-readable saves count |
| `readsCount` | `TEXT` | NOT NULL | Human-readable reads count |
| `isTaleOfTheDay` | `INTEGER`| NOT NULL | Featured daily story flag |
| `isRecentSubmission`|`INTEGER`| NOT NULL | Community submission badge flag |
| `isCuratorSpotlight`|`INTEGER`| NOT NULL | Editorial curator spotlight flag |
| `badgeText` | `TEXT` | NULL | Custom promotional chip badge |
| `totalChapters` | `INTEGER`| NOT NULL | Discovered chapter count |
| `contentFormat` | `TEXT` | NOT NULL | Format discriminator (`prose`, `manga`) |
| `sourceProvider` | `TEXT` | NOT NULL | Provenance provider (`FABLE_ORIGINAL`, `GUTENBERG`, `STANDARD_EBOOKS`, `MANGADEX`) |
| `providerId` | `TEXT` | NULL | Upstream provider identifier (e.g. Gutenberg ID, Standard Ebooks slug) |
| `providerDownloadCount`|`INTEGER`| NULL | Upstream download popularity count |
| `isBookmarked` | `INTEGER`| NOT NULL | Device fallback bookmark flag |
| `isCompleted` | `INTEGER`| NOT NULL | Device fallback completion flag |
| `lastReadChapterId`|`TEXT` | NULL | Pointer to active reading chapter |
| `lastReadChapterNumber`|`INTEGER`| NULL | Ordinal of active reading chapter |
| `shelfUpdatedAtUtc`|`TEXT` | NOT NULL | ISO-8601 timestamp of shelf modification |

**Indices:**
- `CREATE INDEX index_stories_genre ON stories(genre);`
- `CREATE INDEX index_stories_shelfUpdatedAtUtc ON stories(shelfUpdatedAtUtc);`

---

### 1.2 Table: `chapters` (`ChapterEntity`)
Stores chapter content, word metrics, and panel image lists.

| Column | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `TEXT` | PRIMARY KEY, NOT NULL | Unique chapter ID (e.g. `gutenberg:123:ch:1`) |
| `storyId` | `TEXT` | NOT NULL, FK (`stories.id`), CASCADE | Parent story reference |
| `chapterNumber` | `INTEGER`| NOT NULL | 1-based sequential chapter ordinal |
| `title` | `TEXT` | NOT NULL | Chapter heading title |
| `content` | `TEXT` | NOT NULL | Sanitized chapter body text |
| `wordCount` | `INTEGER`| NOT NULL | Pre-calculated word count |
| `createdAtUtc` | `TEXT` | NOT NULL | Ingestion timestamp |
| `pageUrls` | `TEXT` | NOT NULL | JSON-encoded array of panel image URLs |

**Indices & Foreign Keys:**
- `FOREIGN KEY (storyId) REFERENCES stories(id) ON DELETE CASCADE`
- `CREATE INDEX index_chapters_storyId ON chapters(storyId);`
- `CREATE UNIQUE INDEX index_chapters_storyId_chapterNumber ON chapters(storyId, chapterNumber);`

---

### 1.3 Table: `shelf_states` (`ShelfStateEntity`)
Enforces multi-tenant account isolation. Shelves for `guest` and authenticated users are partitioned by `ownerId`.

| Column | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `ownerId` | `TEXT` | PRIMARY KEY (1/2), NOT NULL | Account user ID or `"guest"` |
| `storyId` | `TEXT` | PRIMARY KEY (2/2), NOT NULL | Story identifier reference |
| `isBookmarked` | `INTEGER`| NOT NULL | Bookmark flag (1 = saved) |
| `isCompleted` | `INTEGER`| NOT NULL | Finished flag (1 = completed) |
| `progressPercent`| `INTEGER`| NOT NULL | Reading progress percentage (0-100) |
| `currentPage` | `INTEGER`| NOT NULL | Last active page index |
| `lastReadChapterId`|`TEXT` | NULL | Last active chapter ID |
| `lastReadChapterNumber`|`INTEGER`| NULL | Last active chapter ordinal |
| `updatedAtUtc` | `TEXT` | NOT NULL | ISO-8601 timestamp of shelf change |

**Indices:**
- `CREATE INDEX index_shelf_states_storyId ON shelf_states(storyId);`
- `CREATE INDEX index_shelf_states_ownerId_updatedAtUtc ON shelf_states(ownerId, updatedAtUtc);`

---

### 1.4 Table: `reading_sessions` (`ReadingSessionEntity`)
Locally records completed reading sessions and serves as an offline sync outbox.

| Column | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `ownerId` | `TEXT` | PRIMARY KEY (1/2), NOT NULL | Account user ID or `"guest"` |
| `id` | `TEXT` | PRIMARY KEY (2/2), NOT NULL | Unique session UUID |
| `storyId` | `TEXT` | NOT NULL | Target story ID |
| `secondsRead` | `INTEGER`| NOT NULL | Duration spent reading in seconds |
| `readAtUtc` | `TEXT` | NOT NULL | ISO-8601 session timestamp |
| `isCompleted` | `INTEGER`| NOT NULL | Whether session marked story complete |

**Indices:**
- `CREATE INDEX index_reading_sessions_ownerId_readAtUtc ON reading_sessions(ownerId, readAtUtc);`

---

### 1.5 Table: `reading_stats` (`ReadingStatsEntity`)
Stores cached aggregate reading metrics per user account.

| Column | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `ownerId` | `TEXT` | PRIMARY KEY, NOT NULL | Account user ID or `"guest"` |
| `storiesReadCount`|`INTEGER`| NOT NULL | Total count of completed stories |
| `totalMinutesRead`|`INTEGER`| NOT NULL | Cumulative minutes spent reading |
| `streakDays` | `INTEGER`| NOT NULL | Current daily reading streak |
| `updatedAtUtc` | `TEXT` | NOT NULL | Timestamp of last sync |

---

## 2. SQLite Migration History

### Migration 1 → 2 (`MIGRATION_1_2`)
- Creates table `shelf_states` with composite primary key `(ownerId, storyId)`.
- Creates indices on `storyId` and `(ownerId, updatedAtUtc)`.
- Safely migrates existing shelf data from `stories` into `shelf_states` under default `ownerId = 'guest'`.

### Migration 2 → 3 (`MIGRATION_2_3`)
- Creates table `reading_sessions` with composite primary key `(ownerId, id)`.
- Creates index on `(ownerId, readAtUtc)`.
- Creates table `reading_stats` with primary key `ownerId`.

---

## 3. Room Schema Export & Verification

Room exports JSON schema contracts to `app/schemas/com.example.fable.data.local.FableDatabase/`:
```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```
All DAO Flow queries execute asynchronously on Room's background dispatcher, ensuring zero Main thread disk stalls during active reader scrolling.

