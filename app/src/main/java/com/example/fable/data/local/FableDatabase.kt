package com.example.fable.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        StoryEntity::class,
        ChapterEntity::class,
        ShelfStateEntity::class,
        ReadingSessionEntity::class,
        ReadingStatsEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(FableTypeConverters::class)
abstract class FableDatabase : RoomDatabase() {
    abstract fun storyDao(): StoryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `shelf_states` (
                        `ownerId` TEXT NOT NULL,
                        `storyId` TEXT NOT NULL,
                        `isBookmarked` INTEGER NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `progressPercent` INTEGER NOT NULL,
                        `currentPage` INTEGER NOT NULL,
                        `lastReadChapterId` TEXT,
                        `lastReadChapterNumber` INTEGER,
                        `updatedAtUtc` TEXT NOT NULL,
                        PRIMARY KEY(`ownerId`, `storyId`)
                    )""".trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shelf_states_storyId` ON `shelf_states` (`storyId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shelf_states_ownerId_updatedAtUtc` ON `shelf_states` (`ownerId`, `updatedAtUtc`)")
                db.execSQL(
                    """INSERT INTO `shelf_states` (
                        `ownerId`, `storyId`, `isBookmarked`, `isCompleted`, `progressPercent`,
                        `currentPage`, `lastReadChapterId`, `lastReadChapterNumber`, `updatedAtUtc`
                    )
                    SELECT 'guest', `id`, `isBookmarked`, `isCompleted`, `progressPercent`,
                        `currentPage`, `lastReadChapterId`, `lastReadChapterNumber`, `shelfUpdatedAtUtc`
                    FROM `stories`
                    WHERE `isBookmarked` = 1 OR `isCompleted` = 1 OR `progressPercent` > 0
                        OR `currentPage` != 1 OR `lastReadChapterId` IS NOT NULL
                        OR `lastReadChapterNumber` IS NOT NULL""".trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `reading_sessions` (
                        `ownerId` TEXT NOT NULL,
                        `id` TEXT NOT NULL,
                        `storyId` TEXT NOT NULL,
                        `secondsRead` INTEGER NOT NULL,
                        `readAtUtc` TEXT NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        PRIMARY KEY(`ownerId`, `id`)
                    )""".trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reading_sessions_ownerId_readAtUtc` ON `reading_sessions` (`ownerId`, `readAtUtc`)")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `reading_stats` (
                        `ownerId` TEXT NOT NULL,
                        `storiesReadCount` INTEGER NOT NULL,
                        `totalMinutesRead` INTEGER NOT NULL,
                        `streakDays` INTEGER NOT NULL,
                        `updatedAtUtc` TEXT NOT NULL,
                        PRIMARY KEY(`ownerId`)
                    )""".trimIndent()
                )
            }
        }
    }
}
