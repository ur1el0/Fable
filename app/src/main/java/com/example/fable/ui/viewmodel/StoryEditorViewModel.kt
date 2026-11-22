package com.example.fable.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fable.domain.model.Story
import com.example.fable.domain.repository.StoryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StoryEditorUiState(
    val isPublishing: Boolean = false,
    val publishErrorMessage: String? = null,
    val publishedStory: Story? = null,
    val editingStory: Story? = null,
    val isLoadingEditStory: Boolean = false,
    val isSavingEdit: Boolean = false,
    val isDeletingStory: Boolean = false,
    val editErrorMessage: String? = null
)

class StoryEditorViewModel(
    private val repository: StoryRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(StoryEditorUiState())
    val uiState: StateFlow<StoryEditorUiState> = mutableUiState.asStateFlow()

    private var loadEditJob: Job? = null

    fun publishStory(
        title: String,
        genre: String,
        chapterTitle: String,
        synopsis: String,
        content: String,
        onPublished: suspend () -> Unit = {}
    ) {
        if (mutableUiState.value.isPublishing) return
        mutableUiState.update {
            it.copy(isPublishing = true, publishErrorMessage = null, publishedStory = null)
        }
        viewModelScope.launch {
            repository.publishStory(title, genre, chapterTitle, synopsis, content).fold(
                onSuccess = { story ->
                    runCatching { onPublished() }
                    mutableUiState.update {
                        it.copy(isPublishing = false, publishErrorMessage = null, publishedStory = story)
                    }
                },
                onFailure = { error ->
                    mutableUiState.update {
                        it.copy(
                            isPublishing = false,
                            publishErrorMessage = error.message ?: "Story could not be published."
                        )
                    }
                }
            )
        }
    }

    fun dismissPublishedStory() {
        mutableUiState.update { it.copy(publishedStory = null) }
    }

    fun beginEditing(story: Story) {
        startHydratingEditStory(storyId = story.id, initialStory = story)
    }

    fun beginEditingById(storyId: String) {
        val existing = mutableUiState.value.editingStory?.takeIf { it.id == storyId }
        startHydratingEditStory(storyId = storyId, initialStory = existing)
    }

    fun updateStory(
        storyId: String,
        title: String,
        genre: String,
        chapterTitle: String,
        synopsis: String,
        content: String,
        onUpdated: suspend (Story) -> Unit = {}
    ) {
        if (mutableUiState.value.isSavingEdit || mutableUiState.value.isDeletingStory) return
        if (title.isBlank() || genre.isBlank() || synopsis.isBlank() || content.isBlank()) {
            mutableUiState.update {
                it.copy(editErrorMessage = "Title, genre, synopsis, and chapter content are required.")
            }
            return
        }
        mutableUiState.update { it.copy(isSavingEdit = true, editErrorMessage = null) }
        viewModelScope.launch {
            repository.updateStory(storyId, title, genre, chapterTitle, synopsis, content).fold(
                onSuccess = { updated ->
                    mutableUiState.update {
                        it.copy(
                            editingStory = updated,
                            isSavingEdit = false,
                            editErrorMessage = null
                        )
                    }
                    runCatching { onUpdated(updated) }
                },
                onFailure = { error ->
                    mutableUiState.update {
                        it.copy(
                            isSavingEdit = false,
                            editErrorMessage = error.message ?: "Story could not be updated."
                        )
                    }
                }
            )
        }
    }

    fun deleteStory(storyId: String, onDeleted: suspend () -> Unit = {}) {
        if (mutableUiState.value.isDeletingStory || mutableUiState.value.isSavingEdit) return
        mutableUiState.update { it.copy(isDeletingStory = true, editErrorMessage = null) }
        viewModelScope.launch {
            repository.deleteStory(storyId).fold(
                onSuccess = {
                    mutableUiState.update { state ->
                        state.copy(
                            isDeletingStory = false,
                            editingStory = state.editingStory?.takeUnless { it.id == storyId },
                            editErrorMessage = null
                        )
                    }
                    runCatching { onDeleted() }
                },
                onFailure = { error ->
                    mutableUiState.update {
                        it.copy(
                            isDeletingStory = false,
                            editErrorMessage = error.message ?: "Story could not be deleted."
                        )
                    }
                }
            )
        }
    }

    fun clearEditState() {
        loadEditJob?.cancel()
        mutableUiState.update {
            it.copy(
                editingStory = null,
                isLoadingEditStory = false,
                isSavingEdit = false,
                isDeletingStory = false,
                editErrorMessage = null
            )
        }
    }

    private fun startHydratingEditStory(storyId: String, initialStory: Story?) {
        loadEditJob?.cancel()
        mutableUiState.update {
            it.copy(
                editingStory = initialStory,
                isLoadingEditStory = true,
                isSavingEdit = false,
                isDeletingStory = false,
                editErrorMessage = null
            )
        }
        loadEditJob = viewModelScope.launch {
            val refreshResult = repository.refreshStory(storyId)
            val cachedStory = repository.observeStory(storyId).first() ?: initialStory
            var chapters = repository.observeChapters(storyId).first()
            if (cachedStory?.content.isNullOrBlank() && chapters.firstOrNull()?.content.isNullOrBlank()) {
                repository.refreshChapters(storyId)
                chapters = repository.observeChapters(storyId).first()
            }
            val chapterOne = chapters.firstOrNull { it.chapterNumber == 1 } ?: chapters.firstOrNull()
            val resolvedStory = cachedStory?.let { story ->
                story.copy(
                    chapter = story.chapter.ifBlank { chapterOne?.title.orEmpty() },
                    content = story.content.ifBlank { chapterOne?.content.orEmpty() }
                )
            }
            val loadError = when {
                resolvedStory == null ->
                    refreshResult.exceptionOrNull()?.message ?: "Story could not be loaded for editing."
                resolvedStory.content.isBlank() && refreshResult.isFailure ->
                    refreshResult.exceptionOrNull()?.message ?: "Could not load full story content for editing."
                else -> null
            }
            mutableUiState.update {
                it.copy(
                    editingStory = resolvedStory,
                    isLoadingEditStory = false,
                    editErrorMessage = loadError
                )
            }
        }
    }

    class Factory(private val repository: StoryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(StoryEditorViewModel::class.java))
            return StoryEditorViewModel(repository) as T
        }
    }
}
