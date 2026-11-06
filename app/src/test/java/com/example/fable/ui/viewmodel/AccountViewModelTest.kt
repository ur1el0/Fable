package com.example.fable.ui.viewmodel

import com.example.fable.domain.model.Story
import com.example.fable.domain.model.UserProfile
import com.example.fable.domain.repository.AccountRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {
    @Test
    fun signInLoadsPublishedStories() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val story = story("owned-story")
            val viewModel = AccountViewModel(FakeAccountRepository { Result.success(listOf(story)) })

            viewModel.signIn("reader@example.com", "password")
            advanceUntilIdle()

            assertEquals(listOf(story), viewModel.uiState.value.publishedStories)
            assertEquals(false, viewModel.uiState.value.isLoadingPublishedStories)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun latePublishedStoriesResponseIsDiscardedAfterSignOut() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val pendingStories = CompletableDeferred<Result<List<Story>>>()
            val viewModel = AccountViewModel(FakeAccountRepository { pendingStories.await() })

            viewModel.signIn("reader@example.com", "password")
            runCurrent()
            viewModel.signOut()
            runCurrent()
            pendingStories.complete(Result.success(listOf(story("private-story"))))
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.user)
            assertEquals(emptyList<Story>(), viewModel.uiState.value.publishedStories)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeAccountRepository(
        private val loadStories: suspend () -> Result<List<Story>>
    ) : AccountRepository {
        override fun hasSession() = false

        override suspend fun currentUser() = Result.success(USER)

        override suspend fun myStories() = loadStories()

        override suspend fun signIn(email: String, password: String) = Result.success(USER)

        override suspend fun register(email: String, password: String, name: String, handle: String) =
            Result.success(USER)

        override suspend fun updateProfile(name: String, handle: String, bio: String) = Result.success(USER)

        override suspend fun requestPasswordReset(email: String) = Result.success(Unit)

        override suspend fun confirmPasswordReset(token: String, newPassword: String) = Result.success(Unit)

        override suspend fun signOut() = Result.success(Unit)
    }

    private fun story(id: String) = Story(
        id = id,
        title = "Owned Story",
        author = "Reader",
        genre = "Fantasy",
        synopsis = "Synopsis",
        content = "Content"
    )

    private companion object {
        val USER = UserProfile(
            id = "account-1",
            email = "reader@example.com",
            name = "Reader",
            handle = "reader",
            bio = "",
            avatarImageUrl = null,
            createdAtUtc = "2026-11-06T12:00:00Z"
        )
    }
}
