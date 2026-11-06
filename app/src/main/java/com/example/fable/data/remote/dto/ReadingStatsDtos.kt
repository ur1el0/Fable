package com.example.fable.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReadingSessionRequestDto(
    val id: String,
    val storyId: String,
    val secondsRead: Int,
    val readAtUtc: String,
    val isCompleted: Boolean
)

@Serializable
data class ReadingStatsDto(
    val storiesReadCount: Int = 0,
    val totalMinutesRead: Int = 0,
    val streakDays: Int = 0
)
