package com.example.fable.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [StoryEntity::class, ChapterEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(FableTypeConverters::class)
abstract class FableDatabase : RoomDatabase() {
    abstract fun storyDao(): StoryDao
}
