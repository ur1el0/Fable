package com.example.fable.data.repository

import com.example.fable.data.local.ChapterEntity
import com.example.fable.data.local.ShelfStateEntity
import com.example.fable.data.local.StoryDao
import com.example.fable.data.local.StoryEntity
import com.example.fable.data.local.toEntity
import com.example.fable.data.remote.FableApiService
import com.example.fable.data.remote.dto.StoryDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomStoryRepositoryShelfScopeTest {
    @Test
    fun shelfStateRemainsIsolatedWhenSwitchingAccountsAndGuestScope() = runTest {
        val dao = InMemoryStoryDao()
        dao.upsertStories(
            listOf(
                StoryDto(
                    id = STORY_ID,
                    title = "Shared story",
                    author = "Writer",
                    genre = "Fantasy",
                    synopsis = "A story shared by all readers.",
                    content = "Chapter text"
                ).toEntity()
            )
        )
        val repository = RoomStoryRepository(
            api = FableApiService(baseUrl = "https://fable.test/api/v1/"),
            dao = dao,
            deviceId = "test-device",
            accessTokenProvider = { null }
        )

        repository.setShelfOwner(ACCOUNT_A)
        assertTrue(repository.toggleBookmark(STORY_ID).getOrThrow())
        repository.saveProgress(
            storyId = STORY_ID,
            progressPercent = 35,
            currentPage = 8,
            chapterId = "chapter-8",
            chapterNumber = 8
        ).getOrThrow()

        val accountAState = repository.observeShelf().first().single()
        assertTrue(accountAState.isBookmarked)
        assertEquals(35, accountAState.progressPercent)
        assertEquals(8, accountAState.currentPage)

        repository.setShelfOwner(ACCOUNT_B)
        assertTrue(repository.observeShelf().first().isEmpty())
        assertTrue(repository.toggleBookmark(STORY_ID).getOrThrow())
        repository.saveProgress(
            storyId = STORY_ID,
            progressPercent = 10,
            currentPage = 3,
            chapterId = "chapter-3",
            chapterNumber = 3
        ).getOrThrow()

        val accountBState = repository.observeShelf().first().single()
        assertTrue(accountBState.isBookmarked)
        assertEquals(10, accountBState.progressPercent)
        assertEquals(3, accountBState.currentPage)

        repository.setShelfOwner(ACCOUNT_A)
        val restoredAccountAState = repository.observeShelf().first().single()
        assertTrue(restoredAccountAState.isBookmarked)
        assertEquals(35, restoredAccountAState.progressPercent)
        assertEquals(8, restoredAccountAState.currentPage)

        repository.setShelfOwner(null)
        assertTrue(repository.observeShelf().first().isEmpty())
        assertFalse(repository.observeCatalog().first().single().isBookmarked)
    }

    private class InMemoryStoryDao : StoryDao {
        private val stories = MutableStateFlow<Map<String, StoryEntity>>(emptyMap())
        private val shelves = MutableStateFlow<Map<Pair<String, String>, ShelfStateEntity>>(emptyMap())
        private val chapters = MutableStateFlow<Map<String, List<ChapterEntity>>>(emptyMap())

        override fun observeCatalog(): Flow<List<StoryEntity>> =
            stories.map { it.values.toList() }

        override fun observeStory(storyId: String): Flow<StoryEntity?> =
            stories.map { it[storyId] }

        override suspend fun getStory(storyId: String): StoryEntity? = stories.value[storyId]

        override suspend fun getStories(storyIds: List<String>): List<StoryEntity> =
            storyIds.mapNotNull(stories.value::get)

        override suspend fun upsertStories(stories: List<StoryEntity>) {
            this.stories.value = this.stories.value.toMutableMap().apply {
                stories.forEach { put(it.id, it) }
            }
        }

        override fun observeShelfStates(ownerId: String): Flow<List<ShelfStateEntity>> =
            shelves.map { states -> states.values.filter { it.ownerId == ownerId } }

        override suspend fun getShelfStates(ownerId: String): List<ShelfStateEntity> =
            shelves.value.values.filter { it.ownerId == ownerId }

        override suspend fun getShelfState(ownerId: String, storyId: String): ShelfStateEntity? =
            shelves.value[ownerId to storyId]

        override suspend fun upsertShelfState(state: ShelfStateEntity) {
            shelves.value = shelves.value.toMutableMap().apply {
                put(state.ownerId to state.storyId, state)
            }
        }

        override suspend fun upsertShelfStates(states: List<ShelfStateEntity>) {
            shelves.value = shelves.value.toMutableMap().apply {
                states.forEach { put(it.ownerId to it.storyId, it) }
            }
        }

        override fun observeChapters(storyId: String): Flow<List<ChapterEntity>> =
            chapters.map { values -> values[storyId].orEmpty().sortedBy(ChapterEntity::chapterNumber) }

        override suspend fun getChapter(storyId: String, chapterNumber: Int): ChapterEntity? =
            chapters.value[storyId]?.firstOrNull { it.chapterNumber == chapterNumber }

        override suspend fun deleteChapters(storyId: String) {
            chapters.value = chapters.value.toMutableMap().apply { remove(storyId) }
        }

        override suspend fun upsertChapters(chapters: List<ChapterEntity>) {
            if (chapters.isEmpty()) return
            val updated = this.chapters.value.toMutableMap()
            chapters.groupBy(ChapterEntity::storyId).forEach { (storyId, incoming) ->
                val byNumber = (updated[storyId].orEmpty())
                    .associateBy(ChapterEntity::chapterNumber)
                    .toMutableMap()
                incoming.forEach { byNumber[it.chapterNumber] = it }
                updated[storyId] = byNumber.values.sortedBy(ChapterEntity::chapterNumber)
            }
            this.chapters.value = updated
        }
    }

    private companion object {
        const val STORY_ID = "shared-story"
        const val ACCOUNT_A = "account-a"
        const val ACCOUNT_B = "account-b"
    }
}
