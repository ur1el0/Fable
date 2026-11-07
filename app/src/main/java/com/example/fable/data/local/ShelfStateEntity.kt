package com.example.fable.data.local

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "shelf_states",
    primaryKeys = ["ownerId", "storyId"],
    indices = [Index("storyId"), Index(value = ["ownerId", "updatedAtUtc"])]
)
data class ShelfStateEntity(
    val ownerId: String,
    val storyId: String,
    val isBookmarked: Boolean,
    val isCompleted: Boolean,
    val progressPercent: Int,
    val currentPage: Int,
    val lastReadChapterId: String?,
    val lastReadChapterNumber: Int?,
    val updatedAtUtc: String
)
