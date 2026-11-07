package com.example.fable.domain.repository

import com.example.fable.domain.model.ReadingStats
import kotlinx.coroutines.flow.Flow

interface ReadingStatsRepository {
    fun observeStats(ownerId: String): Flow<ReadingStats>
    suspend fun recordSession(
        ownerId: String,
        storyId: String,
        secondsRead: Int,
        isCompleted: Boolean
    ): Result<Unit>

    suspend fun sync(ownerId: String): Result<Unit>
}
