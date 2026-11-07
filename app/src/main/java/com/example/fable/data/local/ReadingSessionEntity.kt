package com.example.fable.data.local

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "reading_sessions",
    primaryKeys = ["ownerId", "id"],
    indices = [Index(value = ["ownerId", "readAtUtc"])]
)
data class ReadingSessionEntity(
    val ownerId: String,
    val id: String,
    val storyId: String,
    val secondsRead: Int,
    val readAtUtc: String,
    val isCompleted: Boolean
)
