package com.example.fable.data.remote

import com.example.fable.data.remote.dto.ReadingSessionRequestDto
import com.example.fable.data.remote.dto.ReadingStatsDto

interface ReadingStatsRemoteDataSource {
    suspend fun recordReadingSession(request: ReadingSessionRequestDto, accessToken: String)
    suspend fun fetchReadingStats(accessToken: String): ReadingStatsDto
}
