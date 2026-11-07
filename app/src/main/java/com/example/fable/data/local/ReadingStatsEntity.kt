package com.example.fable.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_stats")
data class ReadingStatsEntity(
    @PrimaryKey val ownerId: String,
    val storiesReadCount: Int,
    val totalMinutesRead: Int,
    val streakDays: Int,
    val updatedAtUtc: String
)
