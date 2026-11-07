package com.example.fable.data.local

import com.example.fable.data.remote.dto.ReadingSessionRequestDto
import com.example.fable.data.remote.dto.ReadingStatsDto
import com.example.fable.domain.model.ReadingStats

fun ReadingStatsEntity.toDomain() = ReadingStats(
    storiesReadCount = storiesReadCount,
    totalMinutesRead = totalMinutesRead,
    streakDays = streakDays
)

fun ReadingStatsDto.toEntity(ownerId: String, updatedAtUtc: String) = ReadingStatsEntity(
    ownerId = ownerId,
    storiesReadCount = storiesReadCount,
    totalMinutesRead = totalMinutesRead,
    streakDays = streakDays,
    updatedAtUtc = updatedAtUtc
)

fun ReadingSessionEntity.toRequestDto() = ReadingSessionRequestDto(
    id = id,
    storyId = storyId,
    secondsRead = secondsRead,
    readAtUtc = readAtUtc,
    isCompleted = isCompleted
)
