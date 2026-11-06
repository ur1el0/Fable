package com.example.fable.data.repository

import com.example.fable.data.local.ReadingSessionEntity
import com.example.fable.data.local.ReadingStatsDao
import com.example.fable.data.local.ReadingStatsEntity
import com.example.fable.data.remote.ReadingStatsRemoteDataSource
import com.example.fable.data.remote.dto.ReadingSessionRequestDto
import com.example.fable.data.remote.dto.ReadingStatsDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomReadingStatsRepositoryTest {
    @Test
    fun uploadsPendingSessionsBeforeRefreshingCachedStats() = runTest {
        val dao = FakeReadingStatsDao()
        val remote = FakeReadingStatsRemote()
        val repository = repository(dao, remote)

        repository.recordSession(OWNER_A, STORY_ID, secondsRead = 45, isCompleted = false).getOrThrow()
        repository.sync(OWNER_A).getOrThrow()

        assertEquals(listOf(TOKEN_A), remote.recordedSessions.map { it.accessToken })
        assertEquals(0, dao.pending(OWNER_A).size)
        val cachedStats = requireNotNull(dao.stats[OWNER_A])
        assertEquals(3, cachedStats.storiesReadCount)
        assertEquals(42, cachedStats.totalMinutesRead)
        assertEquals(5, cachedStats.streakDays)
        assertTrue(cachedStats.updatedAtUtc.isNotBlank())
    }

    @Test
    fun failedUploadRetainsTheSameEventForAnIdempotentRetry() = runTest {
        val dao = FakeReadingStatsDao()
        val remote = FakeReadingStatsRemote().apply { failAfterRecording = true }
        val repository = repository(dao, remote)
        repository.recordSession(OWNER_A, STORY_ID, secondsRead = 60, isCompleted = true).getOrThrow()

        assertTrue(repository.sync(OWNER_A).isFailure)
        val pendingId = dao.pending(OWNER_A).single().id
        remote.failAfterRecording = false
        repository.sync(OWNER_A).getOrThrow()

        assertEquals(2, remote.recordedSessions.size)
        assertEquals(pendingId, remote.recordedSessions.first().request.id)
        assertEquals(pendingId, remote.recordedSessions.last().request.id)
        assertTrue(dao.pending(OWNER_A).isEmpty())
    }

    @Test
    fun tokenChangeStopsSyncAndLeavesTheSessionQueued() = runTest {
        val dao = FakeReadingStatsDao()
        val remote = FakeReadingStatsRemote()
        var token: String? = TOKEN_A
        val repository = RoomReadingStatsRepository(remote, dao) { token }
        repository.recordSession(OWNER_A, STORY_ID, secondsRead = 12, isCompleted = false).getOrThrow()
        remote.afterRecord = { token = TOKEN_B }

        repository.sync(OWNER_A).getOrThrow()

        assertEquals(1, remote.recordedSessions.size)
        assertEquals(0, remote.statsFetchCount)
        assertEquals(1, dao.pending(OWNER_A).size)
    }

    @Test
    fun ignoresShortSessionsAndCapsLongSessionPayloads() = runTest {
        val dao = FakeReadingStatsDao()
        val repository = repository(dao, FakeReadingStatsRemote())

        repository.recordSession(OWNER_A, STORY_ID, secondsRead = 2, isCompleted = false).getOrThrow()
        repository.recordSession(OWNER_A, STORY_ID, secondsRead = 90_000, isCompleted = false).getOrThrow()

        assertEquals(86_400, dao.pending(OWNER_A).single().secondsRead)
    }

    private fun repository(dao: FakeReadingStatsDao, remote: FakeReadingStatsRemote) =
        RoomReadingStatsRepository(remote, dao) { TOKEN_A }

    private class FakeReadingStatsRemote : ReadingStatsRemoteDataSource {
        val recordedSessions = mutableListOf<RecordedSession>()
        var failAfterRecording = false
        var afterRecord: (() -> Unit)? = null
        var statsFetchCount = 0

        override suspend fun recordReadingSession(request: ReadingSessionRequestDto, accessToken: String) {
            recordedSessions += RecordedSession(request, accessToken)
            afterRecord?.invoke()
            if (failAfterRecording) error("Simulated connection drop after request delivery")
        }

        override suspend fun fetchReadingStats(accessToken: String): ReadingStatsDto {
            statsFetchCount += 1
            return ReadingStatsDto(storiesReadCount = 3, totalMinutesRead = 42, streakDays = 5)
        }
    }

    private class FakeReadingStatsDao : ReadingStatsDao {
        private val sessions = linkedMapOf<Pair<String, String>, ReadingSessionEntity>()
        val stats = mutableMapOf<String, ReadingStatsEntity>()
        private val statsFlows = mutableMapOf<String, MutableStateFlow<ReadingStatsEntity?>>()

        override suspend fun insertSession(session: ReadingSessionEntity) {
            sessions.putIfAbsent(session.ownerId to session.id, session)
        }

        override suspend fun getPendingSessions(ownerId: String): List<ReadingSessionEntity> = pending(ownerId)

        fun pending(ownerId: String) = sessions.values
            .filter { it.ownerId == ownerId }
            .sortedWith(compareBy(ReadingSessionEntity::readAtUtc, ReadingSessionEntity::id))

        override suspend fun deleteSession(ownerId: String, sessionId: String) {
            sessions.remove(ownerId to sessionId)
        }

        override fun observeStats(ownerId: String): Flow<ReadingStatsEntity?> =
            statsFlows.getOrPut(ownerId) { MutableStateFlow(stats[ownerId]) }

        override suspend fun upsertStats(stats: ReadingStatsEntity) {
            this.stats[stats.ownerId] = stats
            statsFlows.getOrPut(stats.ownerId) { MutableStateFlow(null) }.value = stats
        }
    }

    private data class RecordedSession(
        val request: ReadingSessionRequestDto,
        val accessToken: String
    )

    private companion object {
        const val OWNER_A = "account-a"
        const val STORY_ID = "00000000-0000-0000-0000-000000000001"
        const val TOKEN_A = "token-a"
        const val TOKEN_B = "token-b"
    }
}
