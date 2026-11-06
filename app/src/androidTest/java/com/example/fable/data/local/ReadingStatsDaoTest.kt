package com.example.fable.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReadingStatsDaoTest {
    private lateinit var database: FableDatabase
    private lateinit var dao: ReadingStatsDao

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            FableDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.readingStatsDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun pendingSessionsAndCachedStatsAreScopedToOwner() = runBlocking {
        dao.insertSession(session(ownerId = "account-a", id = "same-event"))
        dao.insertSession(session(ownerId = "account-b", id = "same-event"))
        dao.upsertStats(stats(ownerId = "account-a", minutes = 12))
        dao.upsertStats(stats(ownerId = "account-b", minutes = 31))

        assertEquals(1, dao.getPendingSessions("account-a").size)
        assertEquals(12, dao.observeStats("account-a").first()?.totalMinutesRead)
        assertEquals(31, dao.observeStats("account-b").first()?.totalMinutesRead)

        dao.deleteSession("account-a", "same-event")

        assertTrue(dao.getPendingSessions("account-a").isEmpty())
        assertEquals(1, dao.getPendingSessions("account-b").size)
    }

    private fun session(ownerId: String, id: String) = ReadingSessionEntity(
        ownerId = ownerId,
        id = id,
        storyId = "story-1",
        secondsRead = 60,
        readAtUtc = TIMESTAMP,
        isCompleted = false
    )

    private fun stats(ownerId: String, minutes: Int) = ReadingStatsEntity(
        ownerId = ownerId,
        storiesReadCount = 1,
        totalMinutesRead = minutes,
        streakDays = 1,
        updatedAtUtc = TIMESTAMP
    )

    private companion object {
        const val TIMESTAMP = "2026-11-06T12:00:00Z"
    }
}
