package com.example.fable.ui.viewmodel

import com.example.fable.domain.model.Chapter
import com.example.fable.domain.model.GenreCategory
import com.example.fable.domain.model.ReaderPreferences
import com.example.fable.domain.model.ReadingStats
import com.example.fable.domain.model.Story
import com.example.fable.domain.model.UpdateFeed
import com.example.fable.domain.model.Writer
import com.example.fable.domain.repository.ReaderPreferencesRepository
import com.example.fable.domain.repository.ReadingStatsRepository
import com.example.fable.domain.repository.StoryRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FableViewModelChapterLoadingTest {
    @Test
    fun publishCallbackRunsOnlyAfterRepositorySuccess() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeStoryRepository(chapters = emptyList())
            val viewModel = viewModel(repository)
            advanceUntilIdle()
            var callbackCalled = false

            viewModel.publishStory("Title", "Fantasy", "Chapter 1", "Synopsis", "Text") {
                callbackCalled = true
            }
            advanceUntilIdle()
            assertTrue(callbackCalled)
            assertEquals("story-1", viewModel.uiState.value.publishedStory?.id)
            assertNull(viewModel.uiState.value.selectedStory)

            viewModel.dismissPublishedStory()
            assertNull(viewModel.uiState.value.publishedStory)

            repository.publishError = IllegalStateException("offline")
            callbackCalled = false
            viewModel.publishStory("Title", "Fantasy", "Chapter 1", "Synopsis", "Text") {
                callbackCalled = true
            }
            advanceUntilIdle()

            assertFalse(callbackCalled)
            assertNull(viewModel.uiState.value.publishedStory)
            assertEquals("offline", viewModel.uiState.value.publishErrorMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun coalescesConcurrentRequestsForTheSameChapter() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Result<Unit>>()
            val repository = FakeStoryRepository(chapters = listOf(chapter())) {
                started.complete(Unit)
                release.await()
            }
            val viewModel = viewModel(repository)
            advanceUntilIdle()
            viewModel.openStory(story())
            advanceUntilIdle()

            viewModel.startReading(1)
            viewModel.ensureChapterLoaded(1)
            runCurrent()

            started.await()
            assertEquals(1, repository.chapterRefreshCount)
            release.complete(Result.success(Unit))
            advanceUntilIdle()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun skipsAChapterAlreadyCachedLocally() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeStoryRepository(chapters = listOf(chapter(content = "Cached text")))
            val viewModel = viewModel(repository)
            advanceUntilIdle()
            viewModel.openStory(story())
            advanceUntilIdle()

            viewModel.startReading(1)
            advanceUntilIdle()

            assertEquals(0, repository.chapterRefreshCount)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun skipsChapterOneWhenItsBodyIsCachedOnTheStory() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val story = story(content = "Cached story body")
            val repository = FakeStoryRepository(
                chapters = listOf(chapter()),
                initialStory = story
            )
            val viewModel = viewModel(repository)
            advanceUntilIdle()
            viewModel.openStory(story)
            advanceUntilIdle()

            viewModel.startReading(1)
            advanceUntilIdle()

            assertEquals(0, repository.chapterRefreshCount)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun loadsChapterOnDemandWhenCatalogStoryHasNoCachedChaptersYet() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val metadataOnlyStory = story(content = "").copy(totalChapters = 0)
            val repository = FakeStoryRepository(
                chapters = emptyList(),
                initialStory = metadataOnlyStory
            )
            val viewModel = viewModel(repository)
            advanceUntilIdle()
            viewModel.openStory(metadataOnlyStory)
            advanceUntilIdle()

            viewModel.startReading(1)
            advanceUntilIdle()

            assertEquals(1, repository.chapterRefreshCount)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun viewModel(repository: StoryRepository) = FableViewModel(
        repository = repository,
        readingStatsRepository = FakeReadingStatsRepository,
        readerPreferencesRepository = FakeReaderPreferencesRepository
    )

    private class FakeStoryRepository(
        chapters: List<Chapter>,
        initialStory: Story = story(),
        private val refreshChapter: suspend () -> Result<Unit> = { Result.success(Unit) }
    ) : StoryRepository {
        private val observedStory = MutableStateFlow<Story?>(initialStory)
        private val observedChapters = MutableStateFlow(chapters)
        var chapterRefreshCount = 0
            private set
        var publishError: Throwable? = null

        override fun observeCatalog(): Flow<List<Story>> = flowOf(emptyList())
        override fun observeShelf(): Flow<List<Story>> = flowOf(emptyList())
        override fun observeStory(storyId: String): Flow<Story?> = observedStory
        override fun observeChapters(storyId: String): Flow<List<Chapter>> = observedChapters
        override suspend fun refreshStories(genre: String?, search: String?) = Result.success(Unit)
        override suspend fun refreshStory(storyId: String) = Result.success(Unit)
        override suspend fun refreshChapters(storyId: String) = Result.success(Unit)
        override suspend fun refreshChapter(storyId: String, chapterNumber: Int): Result<Unit> {
            chapterRefreshCount += 1
            return refreshChapter()
        }
        override suspend fun searchGutenberg(topic: String?, search: String?) =
            Result.success(emptyList<Story>())
        override suspend fun searchManga(search: String?, limit: Int, offset: Int) =
            Result.success(emptyList<Story>())
        override suspend fun publishStory(
            title: String,
            genre: String,
            chapterTitle: String,
            synopsis: String,
            content: String
        ): Result<Story> = publishError?.let { Result.failure(it) } ?: Result.success(story())
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

    private object FakeReadingStatsRepository : ReadingStatsRepository {
        override fun observeStats(ownerId: String): Flow<ReadingStats> = flowOf(ReadingStats())
        override suspend fun recordSession(
            ownerId: String,
            storyId: String,
            secondsRead: Int,
            isCompleted: Boolean
        ) = Result.success(Unit)
        override suspend fun sync(ownerId: String) = Result.success(Unit)
    }

    private object FakeReaderPreferencesRepository : ReaderPreferencesRepository {
        override fun read() = ReaderPreferences()
        override fun save(preferences: ReaderPreferences) = Unit
    }

    private companion object {
        fun story(content: String = "") = Story(
            id = "story-1",
            title = "Test story",
            author = "Reader",
            genre = "Fantasy",
            synopsis = "A test story",
            content = content,
            totalChapters = 1,
            lastReadChapterNumber = 1
        )

        fun chapter(content: String = "") = Chapter(
            id = "chapter-1",
            storyId = "story-1",
            chapterNumber = 1,
            title = "Chapter one",
            content = content
        )
    }
}
