package com.example.fable.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShelfStateMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FableDatabase::class.java,
        emptyList()
    )

    @Test
    fun migratesExistingShelfStateToGuestOwner() {
        helper.createDatabase(TEST_DATABASE, 1).apply {
            insertStory(this, "read-story", bookmarked = true, progress = 42, currentPage = 4)
            insertStory(this, "empty-story", bookmarked = false, progress = 0, currentPage = 1)
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DATABASE,
            2,
            true,
            FableDatabase.MIGRATION_1_2
        )
        migrated.query(
            "SELECT ownerId, storyId, isBookmarked, progressPercent, currentPage, updatedAtUtc " +
                "FROM shelf_states ORDER BY storyId"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("guest", cursor.getString(0))
            assertEquals("read-story", cursor.getString(1))
            assertEquals(1, cursor.getInt(2))
            assertEquals(42, cursor.getInt(3))
            assertEquals(4, cursor.getInt(4))
            assertEquals(TEST_TIMESTAMP, cursor.getString(5))
            assertFalse(cursor.moveToNext())
        }
        migrated.close()
    }

    private fun insertStory(
        database: androidx.sqlite.db.SupportSQLiteDatabase,
        storyId: String,
        bookmarked: Boolean,
        progress: Int,
        currentPage: Int
    ) {
        val values = ContentValues()
        database.query("PRAGMA table_info(`stories`)").use { columns ->
            while (columns.moveToNext()) {
                val name = columns.getString(1)
                val type = columns.getString(2).uppercase()
                val notNull = columns.getInt(3) != 0
                when (name) {
                    "id" -> values.put(name, storyId)
                    "title" -> values.put(name, storyId)
                    "isBookmarked" -> values.put(name, bookmarked)
                    "isCompleted" -> values.put(name, false)
                    "progressPercent" -> values.put(name, progress)
                    "currentPage" -> values.put(name, currentPage)
                    "lastReadChapterId" -> if (progress > 0) values.put(name, "chapter-4")
                    "lastReadChapterNumber" -> if (progress > 0) values.put(name, 4)
                    "shelfUpdatedAtUtc" -> values.put(name, TEST_TIMESTAMP)
                    else -> if (notNull) when {
                        type.contains("INT") -> values.put(name, 0)
                        type.contains("REAL") -> values.put(name, 0.0)
                        else -> values.put(name, "")
                    }
                }
            }
        }
        database.insert("stories", SQLiteDatabase.CONFLICT_REPLACE, values)
    }

    private companion object {
        const val TEST_DATABASE = "shelf-state-migration-test"
        const val TEST_TIMESTAMP = "2026-11-06T12:00:00Z"
    }
}
