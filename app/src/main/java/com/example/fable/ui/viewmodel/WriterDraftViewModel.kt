package com.example.fable.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fable.domain.model.WriterDraft
import com.example.fable.domain.repository.WriterDraftRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val GUEST_OWNER_ID = "guest"

data class WriterDraftUiState(
    val ownerId: String = GUEST_OWNER_ID,
    val draft: WriterDraft = WriterDraft(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isDiscarding: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val errorMessage: String? = null
)

class WriterDraftViewModel(
    private val repository: WriterDraftRepository,
    initialOwnerId: String?
) : ViewModel() {
    private val persistenceMutex = Mutex()
    private val activeOwnerId = MutableStateFlow(normalizeOwner(initialOwnerId))
    private val mutableUiState = MutableStateFlow(
        WriterDraftUiState(ownerId = activeOwnerId.value)
    )
    val uiState: StateFlow<WriterDraftUiState> = mutableUiState.asStateFlow()
    private var saveJob: Job? = null
    private var saveJobOwnerId: String? = null

    init {
        loadOwner(activeOwnerId.value)
    }

    fun setOwner(ownerId: String?) {
        val nextOwnerId = normalizeOwner(ownerId)
        val previousOwnerId = activeOwnerId.value
        if (nextOwnerId == previousOwnerId) return

        val previousState = mutableUiState.value
        val previousDraft = previousState.draft.takeIf {
            previousState.ownerId == previousOwnerId &&
                !previousState.isLoading &&
                previousState.hasUnsavedChanges
        }
        if (saveJobOwnerId == previousOwnerId) saveJob?.cancel()
        activeOwnerId.value = nextOwnerId
        mutableUiState.value = WriterDraftUiState(ownerId = nextOwnerId)
        loadOwner(nextOwnerId, previousOwnerId, previousDraft)
    }

    fun updateDraft(draft: WriterDraft) {
        val current = mutableUiState.value
        if (current.isLoading || current.isDiscarding || current.ownerId != activeOwnerId.value || draft == current.draft) return

        mutableUiState.value = current.copy(
            draft = draft,
            isSaving = false,
            hasUnsavedChanges = true,
            errorMessage = null
        )
        scheduleSave(activeOwnerId.value, draft)
    }

    fun discardDraft() {
        val current = mutableUiState.value
        val ownerId = activeOwnerId.value
        if (current.isLoading || current.ownerId != ownerId || !current.draft.hasContent()) return

        if (saveJobOwnerId == ownerId) saveJob?.cancel()
        mutableUiState.value = current.copy(
            isSaving = true,
            isDiscarding = true,
            hasUnsavedChanges = false,
            errorMessage = null
        )
        viewModelScope.launch {
            runCatching {
                persistenceMutex.withLock {
                    if (current.hasUnsavedChanges) repository.save(ownerId, current.draft)
                    repository.clear(ownerId)
                }
            }.fold(
                onSuccess = {
                    mutableUiState.update { state ->
                        if (activeOwnerId.value == ownerId && state.ownerId == ownerId && state.draft == current.draft) {
                            state.copy(
                                draft = WriterDraft(),
                                isSaving = false,
                                isDiscarding = false,
                                hasUnsavedChanges = false,
                                errorMessage = null
                            )
                        } else {
                            state
                        }
                    }
                },
                onFailure = { error ->
                    val stillCurrent = activeOwnerId.value == ownerId &&
                        mutableUiState.value.ownerId == ownerId &&
                        mutableUiState.value.draft == current.draft
                    mutableUiState.update { state ->
                        if (stillCurrent && state.ownerId == ownerId && state.draft == current.draft) {
                            state.copy(
                                isSaving = false,
                                isDiscarding = false,
                                hasUnsavedChanges = current.hasUnsavedChanges,
                                errorMessage = error.message ?: "Draft could not be discarded."
                            )
                        } else {
                            state
                        }
                    }
                    if (stillCurrent && current.hasUnsavedChanges) scheduleSave(ownerId, current.draft)
                }
            )
        }
    }

    private fun scheduleSave(ownerId: String, draft: WriterDraft) {
        saveJob?.cancel()
        saveJobOwnerId = ownerId
        saveJob = viewModelScope.launch {
            delay(AUTOSAVE_DELAY_MS)
            val snapshot = mutableUiState.value.takeIf {
                it.ownerId == ownerId && it.draft == draft && it.hasUnsavedChanges
            } ?: return@launch
            mutableUiState.update { state ->
                if (state.ownerId == ownerId && state.draft == draft) state.copy(isSaving = true) else state
            }
            runCatching {
                persistenceMutex.withLock { repository.save(ownerId, draft) }
            }.fold(
                onSuccess = {
                    mutableUiState.update { state ->
                        if (state.ownerId == ownerId && state.draft == snapshot.draft && state.hasUnsavedChanges) {
                            state.copy(isSaving = false, hasUnsavedChanges = false, errorMessage = null)
                        } else {
                            state
                        }
                    }
                },
                onFailure = { error ->
                    mutableUiState.update { state ->
                        if (state.ownerId == ownerId && state.draft == draft && state.hasUnsavedChanges) {
                            state.copy(isSaving = false, errorMessage = error.message ?: "Draft could not be saved.")
                        } else {
                            state
                        }
                    }
                }
            )
        }
    }

    private fun WriterDraft.hasContent(): Boolean =
        title.isNotBlank() || genre.isNotBlank() || chapterTitle.isNotBlank() && chapterTitle != "Chapter 1" ||
            synopsis.isNotBlank() || content.isNotBlank()

    suspend fun clearAfterPublished(ownerId: String, submittedDraft: WriterDraft) {
        val isCurrentSnapshot = activeOwnerId.value == ownerId &&
            mutableUiState.value.ownerId == ownerId &&
            mutableUiState.value.draft == submittedDraft
        if (isCurrentSnapshot && saveJobOwnerId == ownerId) saveJob?.cancel()
        runCatching {
            persistenceMutex.withLock {
                val isCurrentSnapshot = activeOwnerId.value == ownerId &&
                    mutableUiState.value.ownerId == ownerId &&
                    mutableUiState.value.draft == submittedDraft
                if (isCurrentSnapshot) {
                    repository.clear(ownerId)
                    mutableUiState.update { state ->
                        if (activeOwnerId.value == ownerId && state.draft == submittedDraft) {
                            state.copy(
                                draft = WriterDraft(),
                                isSaving = false,
                                hasUnsavedChanges = false,
                                errorMessage = null
                            )
                        } else {
                            state
                        }
                    }
                } else {
                    repository.clearIfMatches(ownerId, submittedDraft)
                }
            }
        }.onFailure { error ->
            mutableUiState.update { state ->
                if (state.ownerId == ownerId) {
                    state.copy(errorMessage = error.message ?: "Published draft could not be cleared.")
                } else {
                    state
                }
            }
        }
    }

    private fun loadOwner(
        ownerId: String,
        previousOwnerId: String? = null,
        previousDraft: WriterDraft? = null
    ) {
        viewModelScope.launch {
            persistenceMutex.withLock {
                if (previousOwnerId != null && previousDraft != null) {
                    runCatching { repository.save(previousOwnerId, previousDraft) }
                }
                val result = runCatching { repository.get(ownerId) }
                if (activeOwnerId.value != ownerId) return@withLock
                result.fold(
                    onSuccess = { draft ->
                        mutableUiState.value = WriterDraftUiState(
                            ownerId = ownerId,
                            draft = draft ?: WriterDraft(),
                            isLoading = false
                        )
                    },
                    onFailure = { error ->
                        mutableUiState.value = WriterDraftUiState(
                            ownerId = ownerId,
                            isLoading = false,
                            errorMessage = error.message ?: "Draft could not be loaded."
                        )
                    }
                )
            }
        }
    }

    private fun normalizeOwner(ownerId: String?): String =
        ownerId?.takeIf(String::isNotBlank) ?: GUEST_OWNER_ID

    class Factory(
        private val repository: WriterDraftRepository,
        private val ownerId: String?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(WriterDraftViewModel::class.java))
            return WriterDraftViewModel(repository, ownerId) as T
        }
    }

    private companion object {
        const val AUTOSAVE_DELAY_MS = 500L
    }
}
