package com.example.fable.data.repository

import com.example.fable.data.local.ShelfStateEntity
import com.example.fable.data.local.toEntity
import com.example.fable.data.remote.dto.ShelfSyncItemDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShelfSyncPlannerTest {
    @Test
    fun uploadsOnlyLocalStateNewerThanServer() {
        val plan = planShelfSync(
            ownerId = OWNER,
            localStates = listOf(local("newer", "2026-11-06T12:00:00.002Z")),
            remoteItems = listOf(remote("newer", "2026-11-06T12:00:00.001Z"))
        )

        assertTrue(plan.serverWins.isEmpty())
        assertEquals(listOf("newer"), plan.upload.map(ShelfSyncItemDto::storyId))
    }

    @Test
    fun serverWinsOnEqualTimestampAndPreservesLocalReaderPosition() {
        val plan = planShelfSync(
            ownerId = OWNER,
            localStates = listOf(local("same", TIMESTAMP, page = 18, chapterNumber = 3)),
            remoteItems = listOf(remote("same", TIMESTAMP, progress = 0.5))
        )

        assertTrue(plan.upload.isEmpty())
        val reconciled = plan.serverWins.single()
        assertEquals(50, reconciled.progressPercent)
        assertEquals(18, reconciled.currentPage)
        assertEquals("chapter-3", reconciled.lastReadChapterId)
        assertEquals(3, reconciled.lastReadChapterNumber)
    }

    @Test
    fun uploadsLocalOnlyItemsAndDownloadsRemoteOnlyItems() {
        val plan = planShelfSync(
            ownerId = OWNER,
            localStates = listOf(local("local-only", TIMESTAMP)),
            remoteItems = listOf(remote("remote-only", TIMESTAMP))
        )

        assertEquals(listOf("remote-only"), plan.serverWins.map(ShelfStateEntity::storyId))
        assertEquals(listOf("local-only"), plan.upload.map(ShelfSyncItemDto::storyId))
    }

    @Test
    fun ignoresOutOfRangeRemoteProgressAndClampsItForLocalUse() {
        val state = remote("clamped", TIMESTAMP, progress = 2.0).toEntity(OWNER)

        assertEquals(100, state.progressPercent)
        assertFalse(state.isBookmarked)
    }

    @Test
    fun handlesMixedTimezoneFormatsGracefully() {
        val plan = planShelfSync(
            ownerId = OWNER,
            localStates = listOf(local("story-tz", "2026-11-06T12:00:00+00:00")),
            remoteItems = listOf(remote("story-tz", "2026-11-06T12:00:00Z"))
        )

        // Equal timestamps: server wins, no upload
        assertTrue(plan.upload.isEmpty())
        assertEquals(1, plan.serverWins.size)
        assertEquals("story-tz", plan.serverWins.single().storyId)
    }

    private fun local(
        storyId: String,
        timestamp: String,
        page: Int = 1,
        chapterNumber: Int? = null
    ) = ShelfStateEntity(
        ownerId = OWNER,
        storyId = storyId,
        isBookmarked = true,
        isCompleted = false,
        progressPercent = 25,
        currentPage = page,
        lastReadChapterId = chapterNumber?.let { "chapter-$it" },
        lastReadChapterNumber = chapterNumber,
        updatedAtUtc = timestamp
    )

    private fun remote(
        storyId: String,
        timestamp: String,
        progress: Double = 0.25
    ) = ShelfSyncItemDto(
        storyId = storyId,
        readingProgress = progress,
        isBookmarked = false,
        isCompleted = false,
        updatedAtUtc = timestamp
    )

    private companion object {
        const val OWNER = "account-1"
        const val TIMESTAMP = "2026-11-06T12:00:00.000Z"
    }
}
