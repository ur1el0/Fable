package com.example.fable.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ShelfSyncItemDto(
    val storyId: String,
    val readingProgress: Double,
    val isBookmarked: Boolean,
    val isCompleted: Boolean,
    val updatedAtUtc: String
)

@Serializable
data class ShelfSyncRequestDto(
    val deviceId: String,
    val items: List<ShelfSyncItemDto>
)

@Serializable
data class ShelfSyncResponseDto(
    val status: String = "ok",
    val reconciledItems: List<ShelfSyncItemDto> = emptyList(),
    val serverTimeUtc: String = ""
)
