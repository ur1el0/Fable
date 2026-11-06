package com.example.fable.data.local

import com.example.fable.data.remote.dto.ShelfSyncItemDto

fun ShelfSyncItemDto.toEntity(
    ownerId: String,
    previous: ShelfStateEntity? = null
) = ShelfStateEntity(
    ownerId = ownerId,
    storyId = storyId,
    isBookmarked = isBookmarked,
    isCompleted = isCompleted,
    progressPercent = (readingProgress.coerceIn(0.0, 1.0) * 100).toInt(),
    currentPage = previous?.currentPage ?: 1,
    lastReadChapterId = previous?.lastReadChapterId,
    lastReadChapterNumber = previous?.lastReadChapterNumber,
    updatedAtUtc = updatedAtUtc
)

fun ShelfStateEntity.toSyncDto() = ShelfSyncItemDto(
    storyId = storyId,
    readingProgress = progressPercent.coerceIn(0, 100) / 100.0,
    isBookmarked = isBookmarked,
    isCompleted = isCompleted,
    updatedAtUtc = updatedAtUtc
)
