package com.example.fable.ui.viewmodel

import com.example.fable.domain.model.Chapter
import com.example.fable.domain.model.GenreCategory
import com.example.fable.domain.model.SourceProvider
import com.example.fable.domain.model.Story
import com.example.fable.domain.model.StoryCatalog
import com.example.fable.domain.model.UpdateFeed
import com.example.fable.domain.model.Writer
import com.example.fable.domain.repository.StoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModelTest {
    @Test
    fun keepsMatchingCachedProviderStoriesWhenSearchFails() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val cached = story("gutenberg-11", "Alice's Adventures", SourceProvider.GUTENBERG)
            val repository = FakeStoryRepository(
                catalog = listOf(
                    cached,
                    story("manga-1", "Alice Manga", SourceProvider.MANGADEX),
                    story("fable-1", "Alice in Fable", SourceProvider.FABLE_ORIGINAL)
                ),
                gutenbergResult = Result.failure(IllegalStateException("Provider unavailable"))
            )
            val viewModel = ExploreViewModel(repository)
            advanceUntilIdle()

            viewModel.searchProvider(StoryCatalog.GUTENBERG, " alice ")
            assertEquals(listOf(cached), viewModel.uiState.value.providerStories)

            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(listOf(cached), state.providerStories)
            assertFalse(state.isSearchingProvider)
            assertNotNull(state.providerErrorMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun replacesCachedProviderStoriesAfterSuccessfulSearch() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val cached = story("gutenberg-old", "Alice Old", SourceProvider.GUTENBERG)
            val result = story("gutenberg-new", "Alice New", SourceProvider.GUTENBERG)
            val repository = FakeStoryRepository(
                catalog = listOf(cached),
                gutenbergResult = Result.success(listOf(result))
            )
            val viewModel = ExploreViewModel(repository)
            advanceUntilIdle()

            viewModel.searchProvider(StoryCatalog.GUTENBERG, "Alice")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(listOf(result), state.providerStories)
            assertFalse(state.isSearchingProvider)
            assertEquals(null, state.providerErrorMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun searchesStandardEbooksAndPreservesCachedEditionOnFailure() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val cachedSe = story("se-dracula", "Dracula", SourceProvider.STANDARD_EBOOKS)
            val cachedGutenberg = story("gutenberg-345", "Dracula", SourceProvider.GUTENBERG)
            val repository = FakeStoryRepository(
                catalog = listOf(cachedSe, cachedGutenberg),
                gutenbergResult = Result.success(listOf(cachedGutenberg)),
                standardEbooksResult = Result.failure(IllegalStateException("Standard Ebooks unavailable"))
            )
            val viewModel = ExploreViewModel(repository)
            advanceUntilIdle()

            viewModel.searchProvider(StoryCatalog.STANDARD_EBOOKS, "Dracula")
            assertEquals(listOf(cachedSe), viewModel.uiState.value.providerStories)

            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(listOf(cachedSe), state.providerStories)
            assertFalse(state.isSearchingProvider)
            assertEquals("Standard Ebooks unavailable", state.providerErrorMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun searchProviderForwardsValidGutenbergSortAndDefaultsInvalidSortToCatalog() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeStoryRepository(
                catalog = emptyList(),
                gutenbergResult = Result.success(emptyList())
            )
            val viewModel = ExploreViewModel(repository)
            advanceUntilIdle()
            repository.gutenbergSorts.clear()

            viewModel.searchProvider(StoryCatalog.GUTENBERG, "Alice", "popular")
            advanceUntilIdle()
            viewModel.searchProvider(StoryCatalog.GUTENBERG, "Alice", "unsupported")
            advanceUntilIdle()

            assertEquals(listOf<String?>("popular", "catalog"), repository.gutenbergSorts)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeStoryRepository(
        private val catalog: List<Story>,
        private val gutenbergResult: Result<List<Story>>,
        private val standardEbooksResult: Result<List<Story>> = Result.success(emptyList())
    ) : StoryRepository {
        val gutenbergSorts = mutableListOf<String?>()

        override fun observeCatalog(): Flow<List<Story>> = flowOf(catalog)
        override fun observeShelf(): Flow<List<Story>> = flowOf(emptyList())
        override fun observeDownloadedStoryIds(): Flow<Set<String>> = flowOf(emptySet())
        override fun observeStory(storyId: String): Flow<Story?> = flowOf(null)
        override fun observeChapters(storyId: String): Flow<List<Chapter>> = flowOf(emptyList())
        override suspend fun refreshStories(genre: String?, search: String?) = Result.success(Unit)
        override suspend fun refreshStory(storyId: String) = Result.success(Unit)
        override suspend fun refreshChapters(storyId: String) = Result.success(Unit)
        override suspend fun refreshChapter(storyId: String, chapterNumber: Int) = Result.success(Unit)
        override suspend fun downloadStoryForOffline(storyId: String) = Result.success(1)
        override suspend fun removeOfflineChapters(storyId: String) = Result.success(Unit)
        override suspend fun searchGutenberg(topic: String?, search: String?) = gutenbergResult
        override suspend fun searchGutenberg(topic: String?, search: String?, sort: String?): Result<List<Story>> {
            gutenbergSorts += sort
            return gutenbergResult
        }
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
        override suspend fun syncShelfState(storyId: String) = Result.success(Unit)
        override suspend fun toggleBookmark(storyId: String) = Result.success(false)
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

    private companion object {
        fun story(id: String, title: String, provider: SourceProvider) = Story(
            id = id,
            title = title,
            author = "Test author",
            genre = "Fantasy",
            synopsis = "Test synopsis",
            content = "",
            sourceProvider = provider
        )
    }
}
