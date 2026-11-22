package com.example.fable.ui.viewmodel

import com.example.fable.domain.model.Chapter
import com.example.fable.domain.model.GenreCategory
import com.example.fable.domain.model.Story
import com.example.fable.domain.model.UpdateFeed
import com.example.fable.domain.model.Writer
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
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StoryEditorViewModelTest {
    @Test
    fun publishCallbackRunsOnlyAfterRepositorySuccess() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeStoryRepository()
            val viewModel = StoryEditorViewModel(repository)
            var callbackCalled = false

            viewModel.publishStory("Title", "Fantasy", "Chapter 1", "Synopsis", "Text") {
                callbackCalled = true
            }
            advanceUntilIdle()

            assertTrue(callbackCalled)
            assertEquals("story-1", viewModel.uiState.value.publishedStory?.id)

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
    fun beginEditingHydratesBlankSummaryContentBeforeAllowingEdit() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val summaryStub = story(content = "")
            val repository = FakeStoryRepository(
                initialStory = summaryStub,
                hydratedOnRefresh = summaryStub.copy(
                    chapter = "Chapter I",
                    content = "Full chapter body hydrated from detail endpoint"
                )
            )
            val viewModel = StoryEditorViewModel(repository)

            viewModel.beginEditing(summaryStub)
            advanceUntilIdle()

            val editing = viewModel.uiState.value.editingStory!!
            assertFalse(viewModel.uiState.value.isLoadingEditStory)
            assertEquals("Full chapter body hydrated from detail endpoint", editing.content)
            assertEquals("Chapter I", editing.chapter)
            assertNull(viewModel.uiState.value.editErrorMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun updateStoryAndDeleteStoryInvokeCallbacksOnSuccess() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeStoryRepository()
            val viewModel = StoryEditorViewModel(repository)
            var updatedTitle: String? = null
            var deleted = false

            viewModel.updateStory(
                storyId = "story-1",
                title = "Edited Title",
                genre = "Sci-Fi",
                chapterTitle = "Opening",
                synopsis = "Updated synopsis",
                content = "Updated content"
            ) { updated ->
                updatedTitle = updated.title
            }
            advanceUntilIdle()

            assertEquals("Edited Title", updatedTitle)
            assertEquals("Edited Title", viewModel.uiState.value.editingStory?.title)

            viewModel.deleteStory("story-1") {
                deleted = true
            }
            advanceUntilIdle()

            assertTrue(deleted)
            assertNull(viewModel.uiState.value.editingStory)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeStoryRepository(
        initialStory: Story = story(),
        private val hydratedOnRefresh: Story = initialStory
    ) : StoryRepository {
        private val storyFlow = MutableStateFlow<Story?>(initialStory)
        var publishError: Throwable? = null

        override fun observeCatalog(): Flow<List<Story>> = flowOf(emptyList())
        override fun observeShelf(): Flow<List<Story>> = flowOf(emptyList())
        override fun observeDownloadedStoryIds(): Flow<Set<String>> = flowOf(emptySet())
        override fun observeStory(storyId: String): Flow<Story?> = storyFlow
        override fun observeChapters(storyId: String): Flow<List<Chapter>> = flowOf(emptyList())
        override suspend fun refreshStories(genre: String?, search: String?) = Result.success(Unit)
        override suspend fun refreshStory(storyId: String): Result<Unit> {
            storyFlow.value = hydratedOnRefresh
            return Result.success(Unit)
        }
        override suspend fun refreshChapters(storyId: String) = Result.success(Unit)
        override suspend fun refreshChapter(storyId: String, chapterNumber: Int) = Result.success(Unit)
        override suspend fun downloadStoryForOffline(storyId: String) = Result.success(1)
        override suspend fun removeOfflineChapters(storyId: String) = Result.success(Unit)
        override suspend fun searchGutenberg(topic: String?, search: String?) = Result.success(emptyList<Story>())
        override suspend fun searchStandardEbooks(topic: String?, search: String?) = Result.success(emptyList<Story>())
        override suspend fun searchManga(search: String?, limit: Int, offset: Int) = Result.success(emptyList<Story>())
        override suspend fun publishStory(
            title: String,
            genre: String,
            chapterTitle: String,
            synopsis: String,
            content: String
        ): Result<Story> = publishError?.let { Result.failure(it) } ?: Result.success(story())
        override suspend fun updateStory(
            storyId: String,
            title: String,
            genre: String,
            chapterTitle: String,
            synopsis: String,
            content: String
        ): Result<Story> = Result.success(
            story().copy(
                id = storyId,
                title = title,
                genre = genre,
                chapter = chapterTitle,
                synopsis = synopsis,
                content = content
            )
        )
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
        fun story(content: String = "Body") = Story(
            id = "story-1",
            title = "Test story",
            author = "Writer",
            genre = "Fantasy",
            synopsis = "Synopsis",
            content = content,
            ownerUserId = "owner-1"
        )
    }
}
