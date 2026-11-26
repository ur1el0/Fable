package com.example.fable.ui.viewmodel

import com.example.fable.domain.model.Chapter
import com.example.fable.domain.model.GenreCategory
import com.example.fable.domain.model.ReadingStats
import com.example.fable.domain.model.SourceProvider
import com.example.fable.domain.model.Story
import com.example.fable.domain.model.UpdateFeed
import com.example.fable.domain.model.Writer
import com.example.fable.domain.repository.ReadingStatsRepository
import com.example.fable.domain.repository.StoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {
    @Test
    fun refreshCatalogIsolatesExternalProviderFailuresFromLibraryErrorBanner() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeStoryRepository(
                gutenbergResult = Result.failure(IllegalStateException("Gutenberg down")),
                standardEbooksResult = Result.failure(IllegalStateException("Standard Ebooks down"))
            )
            val viewModel = CatalogViewModel(repository, FakeReadingStatsRepository())
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isRefreshing)
            assertNull(viewModel.uiState.value.errorMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun observesDownloadedStoryIdsAndSyncsBookmarkToggle() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val offlineIds = MutableStateFlow(setOf("story-1"))
            val repository = FakeStoryRepository(offlineIds = offlineIds)
            val viewModel = CatalogViewModel(repository, FakeReadingStatsRepository())
            advanceUntilIdle()

            assertEquals(setOf("story-1"), viewModel.uiState.value.offlineStoryIds)

            val target = Story(
                id = "story-1",
                title = "Story",
                author = "Author",
                genre = "Fantasy",
                synopsis = "Synopsis",
                content = "Text",
                sourceProvider = SourceProvider.FABLE_ORIGINAL
            )
            viewModel.toggleBookmark(target)
            advanceUntilIdle()

            assertEquals(listOf("story-1"), repository.toggledStories)
            assertEquals(listOf("story-1"), repository.syncedStories)

            offlineIds.value = emptySet()
            advanceUntilIdle()
            assertEquals(emptySet<String>(), viewModel.uiState.value.offlineStoryIds)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeStoryRepository(
        private val offlineIds: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet()),
        private val gutenbergResult: Result<List<Story>> = Result.success(emptyList()),
        private val standardEbooksResult: Result<List<Story>> = Result.success(emptyList())
    ) : StoryRepository {
        val toggledStories = mutableListOf<String>()
        val syncedStories = mutableListOf<String>()

        override fun observeCatalog(): Flow<List<Story>> = flowOf(emptyList())
        override fun observeShelf(): Flow<List<Story>> = flowOf(emptyList())
        override fun observeDownloadedStoryIds(): Flow<Set<String>> = offlineIds
        override fun observeStory(storyId: String): Flow<Story?> = flowOf(null)
        override fun observeChapters(storyId: String): Flow<List<Chapter>> = flowOf(emptyList())
        override suspend fun refreshStories(genre: String?, search: String?) = Result.success(Unit)
        override suspend fun refreshStory(storyId: String) = Result.success(Unit)
        override suspend fun refreshChapters(storyId: String) = Result.success(Unit)
        override suspend fun refreshChapter(storyId: String, chapterNumber: Int) = Result.success(Unit)
        override suspend fun downloadStoryForOffline(storyId: String) = Result.success(1)
        override suspend fun removeOfflineChapters(storyId: String) = Result.success(Unit)
        override suspend fun searchGutenberg(topic: String?, search: String?) = gutenbergResult
        override suspend fun searchStandardEbooks(topic: String?, search: String?) = standardEbooksResult
        override suspend fun searchManga(search: String?, limit: Int, offset: Int) = Result.success(emptyList<Story>())
        override suspend fun publishStory(
            title: String,
            genre: String,
            chapterTitle: String,
            synopsis: String,
            content: String
        ) = Result.failure<Story>(UnsupportedOperationException())
        override suspend fun updateStory(
            storyId: String,
            title: String,
            genre: String,
            chapterTitle: String,
            synopsis: String,
            content: String
        ) = Result.failure<Story>(UnsupportedOperationException())
        override suspend fun deleteStory(storyId: String) = Result.success(Unit)
        override suspend fun purgeUserData(ownerId: String) = Result.success(Unit)
        override suspend fun setShelfOwner(ownerId: String?) = Unit
        override suspend fun syncShelf() = Result.success(Unit)
        override suspend fun syncShelfState(storyId: String): Result<Unit> {
            syncedStories += storyId
            return Result.success(Unit)
        }
        override suspend fun toggleBookmark(storyId: String): Result<Boolean> {
            toggledStories += storyId
            return Result.success(true)
        }
        override suspend fun saveProgress(
            storyId: String,
            progressPercent: Int,
            currentPage: Int,
            chapterId: String?,
            chapterNumber: Int?
        ) = Result.success(Unit)
        override suspend fun fetchGenres(): List<GenreCategory> = emptyList()
        override suspend fun fetchTopAuthors(): List<Writer> = emptyList()
        override suspend fun refreshUpdateFeed(): Result<UpdateFeed> = Result.success(UpdateFeed())
    }

    private class FakeReadingStatsRepository : ReadingStatsRepository {
        override fun observeStats(ownerId: String): Flow<ReadingStats> = flowOf(ReadingStats())
        override suspend fun recordSession(
            ownerId: String,
            storyId: String,
            secondsRead: Int,
            isCompleted: Boolean
        ) = Result.success(Unit)
        override suspend fun sync(ownerId: String) = Result.success(Unit)
        override suspend fun clearOwnerData(ownerId: String) = Result.success(Unit)
    }
}
