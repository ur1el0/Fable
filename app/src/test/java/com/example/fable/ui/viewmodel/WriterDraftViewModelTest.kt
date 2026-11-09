package com.example.fable.ui.viewmodel

import com.example.fable.domain.model.WriterDraft
import com.example.fable.domain.repository.WriterDraftRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WriterDraftViewModelTest {
    @Test
    fun draftRestoresForItsOwnerAfterSwitchingAccounts() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeWriterDraftRepository().apply {
                drafts["account-a"] = draft("Account A")
                drafts["account-b"] = draft("Account B")
            }
            val viewModel = WriterDraftViewModel(repository, "account-a")
            advanceUntilIdle()
            assertEquals("Account A", viewModel.uiState.value.draft.title)

            viewModel.setOwner("account-b")
            advanceUntilIdle()
            assertEquals("Account B", viewModel.uiState.value.draft.title)

            viewModel.setOwner("account-a")
            advanceUntilIdle()
            assertEquals("Account A", viewModel.uiState.value.draft.title)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun ownerChangeFlushesPendingDraftBeforeLoadingNextOwner() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeWriterDraftRepository()
            val viewModel = WriterDraftViewModel(repository, "account-a")
            advanceUntilIdle()
            val unsaved = draft("Just typed")
            viewModel.updateDraft(unsaved)

            viewModel.setOwner("account-b")
            advanceUntilIdle()

            assertEquals(unsaved, repository.drafts["account-a"])
            assertEquals("account-b", viewModel.uiState.value.ownerId)
            assertEquals(WriterDraft(), viewModel.uiState.value.draft)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun autosavePersistsAndSuccessfulPublishClearsOnlyUnchangedDraft() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeWriterDraftRepository()
            val viewModel = WriterDraftViewModel(repository, "account-a")
            advanceUntilIdle()
            val submitted = draft("Submitted")
            viewModel.updateDraft(submitted)
            advanceTimeBy(AUTOSAVE_DELAY_MS)
            runCurrent()
            assertEquals(submitted, repository.drafts["account-a"])

            viewModel.clearAfterPublished("account-a", submitted)
            assertNull(repository.drafts["account-a"])
            assertEquals(WriterDraft(), viewModel.uiState.value.draft)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun publishOfEarlierSnapshotDoesNotEraseEditsMadeWhilePublishing() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeWriterDraftRepository()
            val viewModel = WriterDraftViewModel(repository, "account-a")
            advanceUntilIdle()
            val submitted = draft("Submitted")
            viewModel.updateDraft(submitted)
            advanceTimeBy(AUTOSAVE_DELAY_MS)
            runCurrent()

            val newer = submitted.copy(title = "Newer edit")
            viewModel.updateDraft(newer)
            viewModel.clearAfterPublished("account-a", submitted)

            assertEquals(newer, viewModel.uiState.value.draft)
            advanceTimeBy(AUTOSAVE_DELAY_MS)
            runCurrent()
            assertEquals(newer, repository.drafts["account-a"])
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeWriterDraftRepository : WriterDraftRepository {
        val drafts = mutableMapOf<String, WriterDraft>()

        override suspend fun get(ownerId: String): WriterDraft? = drafts[ownerId]

        override suspend fun save(ownerId: String, draft: WriterDraft) {
            drafts[ownerId] = draft
        }

        override suspend fun clear(ownerId: String) {
            drafts.remove(ownerId)
        }

        override suspend fun clearIfMatches(ownerId: String, draft: WriterDraft): Boolean {
            if (drafts[ownerId] != draft) return false
            drafts.remove(ownerId)
            return true
        }
    }

    private fun draft(title: String) = WriterDraft(
        title = title,
        genre = "Fantasy",
        chapterTitle = "Chapter 1",
        synopsis = "Synopsis",
        content = "Opening chapter"
    )

    private companion object {
        const val AUTOSAVE_DELAY_MS = 500L
    }
}
