package com.example.fable.data.local

import com.example.fable.data.remote.dto.StoryDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryShelfStateMapperTest {
    @Test
    fun emptyOwnerStateDoesNotExposeLegacyShelfValues() {
        val story = legacyStory().toDomain(shelfState = null)

        assertFalse(story.isBookmarked)
        assertFalse(story.isCompleted)
        assertEquals(0, story.progressPercent)
        assertEquals(1, story.currentPage)
        assertEquals(null, story.lastReadChapterId)
        assertEquals(null, story.lastReadChapterNumber)
    }

    @Test
    fun ownerStateOverridesSharedStoryValues() {
        val state = ShelfStateEntity(
            ownerId = "account-1",
            storyId = STORY_ID,
            isBookmarked = false,
            isCompleted = true,
            progressPercent = 100,
            currentPage = 60,
            lastReadChapterId = "chapter-60",
            lastReadChapterNumber = 60,
            updatedAtUtc = UPDATED_AT
        )

        val story = legacyStory().toDomain(state)

        assertFalse(story.isBookmarked)
        assertTrue(story.isCompleted)
        assertEquals(100, story.progressPercent)
        assertEquals(60, story.currentPage)
        assertEquals("chapter-60", story.lastReadChapterId)
        assertEquals(60, story.lastReadChapterNumber)
    }

    @Test
    fun metadataOnlySummaryDtoPreservesHydratedLocalStoryFields() {
        val hydrated = legacyStory().copy(
            chapter = "CHAPTER I. Down the Rabbit-Hole",
            synopsis = "Alice was beginning to get very tired of sitting by her sister on the bank.",
            content = "Full chapter one text...",
            readTimeMinutes = 132,
            totalChapters = 12,
            sourceProvider = "GUTENBERG",
            providerId = "11",
            providerDownloadCount = 64000
        )
        val summaryDto = StoryDto(
            id = STORY_ID,
            title = "Alice's Adventures in Wonderland",
            author = "Lewis Carroll",
            genre = "Fantasy",
            chapter = "",
            synopsis = "",
            content = "",
            readTimeMinutes = 0,
            totalChapters = 0,
            sourceProvider = "GUTENBERG",
            providerId = "11",
            providerDownloadCount = null
        )

        val merged = summaryDto.toEntity(previous = hydrated)

        assertEquals("CHAPTER I. Down the Rabbit-Hole", merged.chapter)
        assertEquals("Alice was beginning to get very tired of sitting by her sister on the bank.", merged.synopsis)
        assertEquals("Full chapter one text...", merged.content)
        assertEquals(132, merged.readTimeMinutes)
        assertEquals(12, merged.totalChapters)
        assertEquals("11", merged.providerId)
        assertEquals(64000, merged.providerDownloadCount)
    }

    private fun legacyStory() = StoryEntity(
        id = STORY_ID,
        title = "Legacy story",
        author = "Writer",
        genre = "Fantasy",
        chapter = "Chapter 1",
        synopsis = "Synopsis",
        content = "Content",
        readTimeMinutes = 1,
        createdAtUtc = UPDATED_AT,
        updatedAtUtc = UPDATED_AT,
        coverImageName = null,
        heroImageName = null,
        coverImageUrl = null,
        totalPages = 60,
        currentPage = 7,
        progressPercent = 35,
        rating = null,
        savesCount = "0",
        readsCount = "0",
        isTaleOfTheDay = false,
        isRecentSubmission = false,
        isCuratorSpotlight = false,
        badgeText = null,
        totalChapters = 60,
        contentFormat = "PROSE",
        sourceProvider = "FABLE_ORIGINAL",
        providerId = null,
        providerDownloadCount = null,
        isBookmarked = true,
        isCompleted = false,
        lastReadChapterId = "chapter-7",
        lastReadChapterNumber = 7,
        shelfUpdatedAtUtc = UPDATED_AT
    )

    private companion object {
        const val STORY_ID = "story-1"
        const val UPDATED_AT = "2026-11-06T12:00:00.000Z"
    }
}
