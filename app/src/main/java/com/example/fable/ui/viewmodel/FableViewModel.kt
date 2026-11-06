package com.example.fable.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fable.domain.model.GenreCategory
import com.example.fable.domain.model.Story
import com.example.fable.domain.model.UpdateFeed
import com.example.fable.domain.model.Writer
import com.example.fable.domain.repository.StoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FableUiState(
    val stories: List<Story> = emptyList(),
    val shelfStories: List<Story> = emptyList(),
    val genres: List<GenreCategory> = emptyList(),
    val writers: List<Writer> = emptyList(),
    val updateFeed: UpdateFeed? = null,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

class FableViewModel(
    private val repository: StoryRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(FableUiState())
    val uiState: StateFlow<FableUiState> = mutableUiState.asStateFlow()

    init {
        repository.observeCatalog()
            .onEach { stories -> mutableUiState.update { it.copy(stories = stories) } }
            .launchIn(viewModelScope)
        repository.observeShelf()
            .onEach { stories -> mutableUiState.update { it.copy(shelfStories = stories) } }
            .launchIn(viewModelScope)
        refreshCatalog()
    }

    fun refreshCatalog() {
        if (mutableUiState.value.isRefreshing) return
        viewModelScope.launch {
            mutableUiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            var firstError: Throwable? = null

            repository.refreshStories().onFailure { firstError = it }
            runCatching { repository.fetchGenres() }
                .onSuccess { genres -> mutableUiState.update { it.copy(genres = genres) } }
                .onFailure { if (firstError == null) firstError = it }
            runCatching { repository.fetchTopAuthors() }
                .onSuccess { writers -> mutableUiState.update { it.copy(writers = writers) } }
                .onFailure { if (firstError == null) firstError = it }
            repository.refreshUpdateFeed()
                .onSuccess { feed -> mutableUiState.update { it.copy(updateFeed = feed) } }
                .onFailure { if (firstError == null) firstError = it }

            mutableUiState.update {
                it.copy(
                    isRefreshing = false,
                    errorMessage = firstError?.message?.takeIf(String::isNotBlank)
                )
            }
        }
    }

    fun toggleBookmark(story: Story) {
        viewModelScope.launch {
            repository.toggleBookmark(story.id).onFailure { error ->
                mutableUiState.update { it.copy(errorMessage = error.message) }
            }
        }
    }

    class Factory(private val repository: StoryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(FableViewModel::class.java))
            return FableViewModel(repository) as T
        }
    }
}
