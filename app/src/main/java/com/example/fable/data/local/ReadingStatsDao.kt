package com.example.fable.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingStatsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSession(session: ReadingSessionEntity)

    @Query("SELECT * FROM reading_sessions WHERE ownerId = :ownerId ORDER BY readAtUtc, id")
    suspend fun getPendingSessions(ownerId: String): List<ReadingSessionEntity>

    @Query("DELETE FROM reading_sessions WHERE ownerId = :ownerId AND id = :sessionId")
    suspend fun deleteSession(ownerId: String, sessionId: String)

    @Query("SELECT * FROM reading_stats WHERE ownerId = :ownerId LIMIT 1")
    fun observeStats(ownerId: String): Flow<ReadingStatsEntity?>

    @Upsert
    suspend fun upsertStats(stats: ReadingStatsEntity)
}
