package com.example.fable.data.repository

import com.example.fable.data.local.ReadingSessionEntity
import com.example.fable.data.local.ReadingStatsDao
import com.example.fable.data.local.toDomain
import com.example.fable.data.local.toEntity
import com.example.fable.data.local.toRequestDto
import com.example.fable.data.remote.ReadingStatsRemoteDataSource
import com.example.fable.domain.model.ReadingStats
import com.example.fable.domain.repository.ReadingStatsRepository
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RoomReadingStatsRepository(
    private val api: ReadingStatsRemoteDataSource,
    private val dao: ReadingStatsDao,
    private val accessTokenProvider: () -> String?
) : ReadingStatsRepository {
    private val syncMutex = Mutex()

    override fun observeStats(ownerId: String): Flow<ReadingStats> =
        dao.observeStats(ownerId).map { it?.toDomain() ?: ReadingStats() }

    override suspend fun recordSession(
        ownerId: String,
        storyId: String,
        secondsRead: Int,
        isCompleted: Boolean
    ): Result<Unit> = runCatching {
        require(ownerId.isNotBlank()) { "A shelf owner is required to record a reading session." }
        if (secondsRead < MIN_SESSION_SECONDS) return@runCatching
        val normalizedStoryId = normalizeStoryIdForBackend(storyId)
        dao.insertSession(
            ReadingSessionEntity(
                ownerId = ownerId,
                id = UUID.randomUUID().toString(),
                storyId = normalizedStoryId,
                secondsRead = secondsRead.coerceAtMost(MAX_SESSION_SECONDS),
                readAtUtc = Instant.now().toString(),
                isCompleted = isCompleted
            )
        )
    }

    override suspend fun sync(ownerId: String): Result<Unit> = runCatching {
        if (ownerId.isBlank() || ownerId == GUEST_OWNER_ID) return@runCatching
        val token = accessTokenProvider()?.takeIf(String::isNotBlank) ?: return@runCatching

        syncMutex.withLock sync@{
            for (session in dao.getPendingSessions(ownerId)) {
                if (accessTokenProvider() != token) return@sync
                try {
                    api.recordReadingSession(session.toRequestDto(), token)
                    if (accessTokenProvider() != token) return@sync
                    dao.deleteSession(ownerId, session.id)
                } catch (exception: Exception) {
                    if (isUnrecoverablePayloadError(exception)) {
                        dao.deleteSession(ownerId, session.id)
                    } else {
                        throw exception
                    }
                }
            }

            if (accessTokenProvider() != token) return@sync
            val stats = api.fetchReadingStats(token)
            if (accessTokenProvider() != token) return@sync
            dao.upsertStats(stats.toEntity(ownerId, Instant.now().toString()))
        }
    }

    private fun normalizeStoryIdForBackend(storyId: String): String {
        if (runCatching { UUID.fromString(storyId) }.isSuccess) return storyId
        val gutenbergNumber = storyId.removePrefix("gutenberg-").toLongOrNull()
            ?.takeIf { it in 1..Int.MAX_VALUE.toLong() }
        if (gutenbergNumber != null) {
            return UUID(0L, gutenbergNumber).toString()
        }
        return storyId
    }

    private fun isUnrecoverablePayloadError(exception: Throwable): Boolean {
        if (exception is com.example.fable.data.remote.FableApiException) {
            return exception.statusCode in 400..499 &&
                exception.statusCode != 401 &&
                exception.statusCode != 429
        }
        return false
    }

    private companion object {
        const val GUEST_OWNER_ID = "guest"
        const val MIN_SESSION_SECONDS = 3
        const val MAX_SESSION_SECONDS = 86_400
    }
}
