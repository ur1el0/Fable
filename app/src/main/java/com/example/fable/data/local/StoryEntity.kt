package com.example.fable.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stories",
    indices = [Index("genre"), Index("shelfUpdatedAtUtc")]
)
data class StoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val genre: String,
    val chapter: String,
    val synopsis: String,
    val content: String,
    val readTimeMinutes: Int,
    val createdAtUtc: String,
    val updatedAtUtc: String,
    val coverImageName: String?,
    val heroImageName: String?,
    val coverImageUrl: String?,
    val totalPages: Int,
    val currentPage: Int,
    val progressPercent: Int,
    val rating: Double?,
    val savesCount: String,
    val readsCount: String,
    val isTaleOfTheDay: Boolean,
    val isRecentSubmission: Boolean,
    val isCuratorSpotlight: Boolean,
    val badgeText: String?,
    val totalChapters: Int,
    val contentFormat: String,
    val sourceProvider: String,
    val providerId: String?,
    val providerDownloadCount: Int?,
    val isBookmarked: Boolean,
    val isCompleted: Boolean,
    val lastReadChapterId: String?,
    val lastReadChapterNumber: Int?,
    val shelfUpdatedAtUtc: String
)
