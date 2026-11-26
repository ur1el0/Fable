package com.example.fable.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WriterDraftDaoTest {
    private lateinit var database: FableDatabase
    private lateinit var dao: WriterDraftDao

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            FableDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.writerDraftDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun draftsAreIsolatedAndClearedOnlyWhenSubmittedSnapshotMatches() = runBlocking {
        val accountADraft = draft(ownerId = "account-a", title = "A")
        val accountBDraft = draft(ownerId = "account-b", title = "B")
        dao.upsert(accountADraft)
        dao.upsert(accountBDraft)

        assertEquals(accountADraft, dao.get("account-a"))
        assertEquals(accountBDraft, dao.get("account-b"))
        assertEquals(0, dao.clearIfMatches("account-a", "Edited", "Fantasy", "Chapter 1", "Synopsis", "Text"))
        assertEquals(accountADraft, dao.get("account-a"))

        assertEquals(
            1,
            dao.clearIfMatches("account-a", "A", "Fantasy", "Chapter 1", "Synopsis", "Text")
        )
        assertNull(dao.get("account-a"))
        assertTrue(dao.get("account-b") != null)

        dao.clear("account-b")
        assertNull(dao.get("account-b"))
    }

    private fun draft(ownerId: String, title: String) = WriterDraftEntity(
        ownerId = ownerId,
        title = title,
        genre = "Fantasy",
        chapterTitle = "Chapter 1",
        synopsis = "Synopsis",
        content = "Text"
    )
}
