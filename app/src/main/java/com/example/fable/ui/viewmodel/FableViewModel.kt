package com.example.fable.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fable.domain.model.Chapter
import com.example.fable.domain.model.GenreCategory
import com.example.fable.domain.model.Story
import com.example.fable.domain.model.StoryCatalog
import com.example.fable.domain.model.UpdateFeed
import com.example.fable.domain.model.Writer
import com.example.fable.domain.repository.StoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class FableUiState(
    val stories: List<Story> = emptyList(),
    val shelfStories: List<Story> = emptyList(),
    val providerStories: List<Story> = emptyList(),
    val selectedStory: Story? = null,
    val chapters: List<Chapter> = emptyList(),
    val selectedChapterNumber: Int? = null,
    val isLoadingStory: Boolean = false,
    val isReaderOpen: Boolean = false,
    val genres: List<GenreCategory> = emptyList(),
    val writers: List<Writer> = emptyList(),
    val updateFeed: UpdateFeed? = null,
    val isRefreshing: Boolean = false,
    val isPublishing: Boolean = false,
    val publishErrorMessage: String? = null,
    val publishNoticeMessage: String? = null,
    val isSearchingProvider: Boolean = false,
    val providerErrorMessage: String? = null,
    val errorMessage: String? = null
)

class FableViewModel(
    private val repository: StoryRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(FableUiState())
    val uiState: StateFlow<FableUiState> = mutableUiState.asStateFlow()
    private var selectedStoryJob: Job? = null
    private var chaptersJob: Job? = null
    private var providerSearchJob: Job? = null

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
            val result = repository.toggleBookmark(story.id)
            result.onFailure { error ->
                mutableUiState.update { it.copy(errorMessage = error.message) }
            }
            if (result.isSuccess) repository.syncShelfState(story.id)
        }
    }

    fun searchProvider(catalog: StoryCatalog, query: String) {
        providerSearchJob?.cancel()
        if (catalog == StoryCatalog.FABLE) {
            mutableUiState.update {
                it.copy(providerStories = emptyList(), isSearchingProvider = false, providerErrorMessage = null)
            }
            return
        }
        mutableUiState.update {
            it.copy(providerStories = emptyList(), isSearchingProvider = true, providerErrorMessage = null)
        }
        providerSearchJob = viewModelScope.launch {
            delay(350)
            val normalizedQuery = query.trim().take(200).takeIf(String::isNotEmpty)
            val result = when (catalog) {
                StoryCatalog.FABLE -> Result.success(emptyList())
                StoryCatalog.GUTENBERG -> repository.searchGutenberg(search = normalizedQuery)
                StoryCatalog.MANGA -> repository.searchManga(search = normalizedQuery)
            }
            result.fold(
                onSuccess = { stories ->
                    mutableUiState.update {
                        it.copy(providerStories = stories, isSearchingProvider = false, providerErrorMessage = null)
                    }
                },
                onFailure = { error ->
                    mutableUiState.update {
                        it.copy(providerStories = emptyList(), isSearchingProvider = false, providerErrorMessage = error.message)
                    }
                }
            )
        }
    }

    fun publishStory(title: String, genre: String, synopsis: String, content: String) {
        if (mutableUiState.value.isPublishing) return
        mutableUiState.update { it.copy(isPublishing = true, publishErrorMessage = null, publishNoticeMessage = null) }
        viewModelScope.launch {
            repository.publishStory(title, genre, synopsis, content).fold(
                onSuccess = { story ->
                    mutableUiState.update {
                        it.copy(isPublishing = false, publishErrorMessage = null, publishNoticeMessage = "Story published.")
                    }
                    openStory(story)
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

    fun setShelfOwner(ownerId: String?) {
        viewModelScope.launch {
            repository.setShelfOwner(ownerId)
            if (!ownerId.isNullOrBlank()) repository.syncShelf()
        }
    }

    fun openStory(story: Story) {
        selectedStoryJob?.cancel()
        chaptersJob?.cancel()
        mutableUiState.update {
            it.copy(
                selectedStory = story,
                chapters = emptyList(),
                selectedChapterNumber = story.lastReadChapterNumber,
                isLoadingStory = true,
                isReaderOpen = false,
                errorMessage = null
            )
        }
        selectedStoryJob = repository.observeStory(story.id)
            .onEach { cached -> if (cached != null) mutableUiState.update { it.copy(selectedStory = cached) } }
            .launchIn(viewModelScope)
        chaptersJob = repository.observeChapters(story.id)
            .onEach { chapters -> mutableUiState.update { it.copy(chapters = chapters) } }
            .launchIn(viewModelScope)
        viewModelScope.launch {
            repository.refreshStory(story.id).onFailure(::setError)
            repository.refreshChapters(story.id).onFailure(::setError)
            mutableUiState.update { it.copy(isLoadingStory = false) }
        }
    }

    fun startReading(chapterNumber: Int? = null) {
        val state = mutableUiState.value
        val story = state.selectedStory ?: return
        val number = chapterNumber
            ?: state.selectedChapterNumber
            ?: state.chapters.firstOrNull()?.chapterNumber
            ?: 1
        mutableUiState.update { it.copy(isReaderOpen = true, selectedChapterNumber = number) }
        loadChapter(story.id, number)
    }

    fun selectChapter(chapterNumber: Int) {
        val story = mutableUiState.value.selectedStory ?: return
        mutableUiState.update { it.copy(selectedChapterNumber = chapterNumber) }
        loadChapter(story.id, chapterNumber)
    }

    fun closeStory() {
        selectedStoryJob?.cancel()
        chaptersJob?.cancel()
        mutableUiState.update {
            it.copy(
                selectedStory = null,
                chapters = emptyList(),
                selectedChapterNumber = null,
                isLoadingStory = false,
                isReaderOpen = false
            )
        }
    }

    fun navigateBackFromStory() {
        if (mutableUiState.value.isReaderOpen) {
            mutableUiState.update { it.copy(isReaderOpen = false) }
        } else {
            closeStory()
        }
    }

    fun saveReadingProgress(progressPercent: Int, chapterNumber: Int) {
        val story = mutableUiState.value.selectedStory ?: return
        val page = if (story.totalPages > 0) {
            (story.totalPages * progressPercent.coerceIn(0, 100) / 100).coerceAtLeast(1)
        } else {
            chapterNumber.coerceAtLeast(1)
        }
        viewModelScope.launch {
            val result = repository.saveProgress(
                storyId = story.id,
                progressPercent = progressPercent,
                currentPage = page,
                chapterId = mutableUiState.value.chapters
                    .firstOrNull { it.chapterNumber == chapterNumber }?.id,
                chapterNumber = chapterNumber
            )
            result.onFailure(::setError)
            if (result.isSuccess) repository.syncShelfState(story.id)
        }
    }

    private fun loadChapter(storyId: String, chapterNumber: Int) {
        if (mutableUiState.value.chapters.none { it.chapterNumber == chapterNumber }) return
        viewModelScope.launch {
            repository.refreshChapter(storyId, chapterNumber).onFailure(::setError)
        }
    }

    private fun setError(error: Throwable) {
        mutableUiState.update { it.copy(errorMessage = error.message) }
    }

    class Factory(private val repository: StoryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(FableViewModel::class.java))
            return FableViewModel(repository) as T
        }
    }
}
