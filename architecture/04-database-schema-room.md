# 04 Database Schema & Persistence (Room v6)

## 1. Relational Schema Architecture

Fable Android uses **AndroidX Room (`FableDatabase`, version 6)** with schema export enabled (`app/schemas/com.example.fable.data.local.FableDatabase/1.json` through `6.json`). Every schema upgrade is backed by an explicit SQL `Migration` (`MIGRATION_1_2` through `MIGRATION_5_6`) and verified by `ShelfStateMigrationTest`.

```
┌──────────────────────────────────────────┐
│                 stories                  │
├──────────────────────────────────────────┤
│ PK  id: TEXT                             │
│     ownerUserId: TEXT?                   │
│     title: TEXT                          │
│     author: TEXT                         │
│     AuthorBio: TEXT                      │
│     genre: TEXT                          │
│     readTimeMinutes: INTEGER             │
│     rating: REAL                         │
│     synopsis: TEXT                       │
│     accentHex: TEXT                      │
│     featuredRank: INTEGER?               │
│     isBookmarked: INTEGER (legacy col)   │
│     isCompleted: INTEGER (legacy col)    │
│     readingProgress: REAL (legacy col)   │
│     lastReadChapterIndex: INTEGER        │
│     provider: TEXT                       │
│     contentFormat: TEXT                  │
│     coverImageUrl: TEXT?                 │
│     externalId: TEXT?                    │
│     sourceUrl: TEXT?                     │
└──────────┬─────────────────────┬─────────┘
           │ 1:N (CASCADE)       │ 1:N (CASCADE)
           ▼                     ▼
┌─────────────────────────┐  ┌─────────────────────────────┐
│        chapters         │  │      story_highlights       │
├─────────────────────────┤  ├─────────────────────────────┤
│ PK  storyId: TEXT (FK)  │  │ PK  id: TEXT                │
│ PK  index: INTEGER      │  │ FK  storyId: TEXT           │
│     chapterId: TEXT     │  │     chapterIndex: INTEGER   │
│     title: TEXT         │  │     chapterTitle: TEXT      │
│     readTimeMinutes: INT│  │     paragraphIndex: INTEGER │
│     content: TEXT       │  │     startWordIndex: INTEGER │
│     pagesJson: TEXT     │  │     endWordIndex: INTEGER   │
│     pageCount: INTEGER  │  │     quoteText: TEXT         │
└─────────────────────────┘  │     note: TEXT              │
                             │     colorName: TEXT         │
                             │     createdAtEpochMillis    │
                             └─────────────────────────────┘

┌─────────────────────────┐  ┌─────────────────────────────┐
│      shelf_states       │  │      reading_sessions       │
├─────────────────────────┤  ├─────────────────────────────┤
│ PK  ownerId: TEXT       │  │ PK  sessionId: TEXT         │
│ PK  storyId: TEXT       │  │     ownerId: TEXT           │
│     isBookmarked: INT   │  │     storyId: TEXT           │
│     isCompleted: INT    │  │     chapterIndex: INTEGER   │
│     readingProgress:REAL│  │     durationSeconds: INT    │
│     lastReadChapterIdx  │  │     wordsRead: INTEGER      │
│     updatedAtEpochMs    │  │     completedStory: INTEGER │
│     pendingSync: INT    │  │     endedAtIso: TEXT        │
└─────────────────────────┘  │     pendingSync: INTEGER    │
                             └─────────────────────────────┘

┌─────────────────────────┐  ┌─────────────────────────────┐
│      reading_stats      │  │        writer_drafts        │
├─────────────────────────┤  ├─────────────────────────────┤
│ PK  ownerId: TEXT       │  │ PK  draftKey: TEXT          │
│     totalReadingMinutes │  │     title: TEXT             │
│     totalWordsRead: INT │  │     genre: TEXT             │
│     completedStoriesCount│ │     synopsis: TEXT          │
│     activeDaysLast7: INT│  │     chapterTitle: TEXT      │
│     currentStreakDays   │  │     body: TEXT              │
│     sessionsCount: INT  │  │     updatedAtEpochMillis    │
│     lastReadAtIso: TEXT?│  └─────────────────────────────┘
└─────────────────────────┘
```

---

## 2. Schema Version History & Migrations

| Migration | Target Version | Changes Introduced |
|---|---|---|
| Base (`1.json`) | `v1` | Initial `stories` and `chapters` tables (`ForeignKey.CASCADE` on `chapters.storyId`). |
| `MIGRATION_1_2` | `v2` | Added `shelf_states` (account-scoped shelf progress with `ownerId`), `reading_sessions`, and `reading_stats`; backfilled existing shelf flags from `stories` into the `__guest__` owner scope. |
| `MIGRATION_2_3` | `v3` | Added provider and comic metadata columns to `stories` (`provider`, `contentFormat`, `coverImageUrl`, `externalId`, `sourceUrl`) and comic page columns to `chapters` (`chapterId`, `pagesJson`, `pageCount`). |
| `MIGRATION_3_4` | `v4` | Added `writer_drafts` table for local auto-saved author manuscripts (`WriterDraftEntity`). |
| `MIGRATION_4_5` | `v5` | Added nullable `ownerUserId` column to `stories` so published Fable Originals can be matched to the signed-in author for Edit/Delete permissions and account deletion cleanup. |
| `MIGRATION_5_6` | `v6` | Added `story_highlights` table with foreign key `CASCADE` to `stories(id)` and indices on `storyId` and `(storyId, chapterIndex)` for persistent passage highlights and annotations. |

---

## 3. Data Integrity & Offline Cache Rules

- **Non-Destructive Chapter Merging (`coalesceChapters`)**: Catalog and search endpoints return lightweight story summaries whose chapters have empty `content` and empty `pages`. When `RoomStoryRepository` upserts catalog results, it preserves existing cached chapter bodies and comic page lists in `chapters`.
- **Offline Chapter Detection (`observeDownloadedStoryIds`)**: `StoryDao.observeDownloadedStoryIds()` emits story IDs that have at least one chapter with non-blank prose `content` or a non-empty `pagesJson` payload (`!= '[]'`).
- **Explicit Offline Cache Removal (`clearOfflineCache`)**: Clears `content = ''` and `pagesJson = '[]'` for a story's chapters inside a Room transaction without deleting the `StoryEntity` or user `ShelfStateEntity`.
