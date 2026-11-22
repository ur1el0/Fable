package com.example.fable.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fable.domain.model.Chapter
import com.example.fable.domain.model.ReaderPreferences
import com.example.fable.domain.model.Story
import com.example.fable.domain.repository.ReaderPreferencesRepository
import com.example.fable.domain.repository.ReadingStatsRepository
import com.example.fable.domain.repository.StoryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReaderUiState(
    val selectedStory: Story? = null,
    val chapters: List<Chapter> = emptyList(),
    val selectedChapterNumber: Int? = null,
    val isLoadingStory: Boolean = false,
    val isDownloadingOffline: Boolean = false,
    val offlineMessage: String? = null,
    val readerPreferences: ReaderPreferences = ReaderPreferences(),
    val errorMessage: String? = null
)

class ReaderViewModel(
    private val repository: StoryRepository,
    private val readingStatsRepository: ReadingStatsRepository,
    private val readerPreferencesRepository: ReaderPreferencesRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(
        ReaderUiState(readerPreferences = readerPreferencesRepository.read())
    )
    val uiState: StateFlow<ReaderUiState> = mutableUiState.asStateFlow()

    private var selectedStoryJob: Job? = null
    private var chaptersJob: Job? = null
    private var refreshJob: Job? = null
    private val loadingChapterRequests = mutableSetOf<Pair<String, Int>>()
    private var activeOwnerId = GUEST_OWNER_ID
    private var readerSessionStoryId: String? = null
    private var readerSessionOwnerId = GUEST_OWNER_ID
    private var readerSessionCompleted = false

    fun setActiveOwner(ownerId: String?) {
        activeOwnerId = ownerId?.takeIf(String::isNotBlank) ?: GUEST_OWNER_ID
    }

    fun openStory(story: Story) {
        if (mutableUiState.value.selectedStory?.id == story.id && selectedStoryJob?.isActive == true) {
            return
        }
        startStoryObservation(
            storyId = story.id,
            initialStory = story,
            initialChapterNumber = story.lastReadChapterNumber,
            startReadingChapter = null
        )
    }

    fun openStoryById(storyId: String, startReadingChapter: Int? = null) {
        val currentState = mutableUiState.value
        if (currentState.selectedStory?.id == storyId && selectedStoryJob?.isActive == true) {
            if (startReadingChapter != null && currentState.selectedChapterNumber != startReadingChapter) {
                startReading(startReadingChapter)
            } else if (startReadingChapter != null && readerSessionStoryId != storyId) {
                startReading(startReadingChapter)
            }
            return
        }
        startStoryObservation(
            storyId = storyId,
            initialStory = null,
            initialChapterNumber = startReadingChapter,
            startReadingChapter = startReadingChapter
        )
    }

    fun startReading(chapterNumber: Int? = null): Int {
        val state = mutableUiState.value
        val story = state.selectedStory ?: return chapterNumber ?: 1
        val number = chapterNumber
            ?: state.selectedChapterNumber
            ?: state.chapters.firstOrNull()?.chapterNumber
            ?: 1
        readerSessionStoryId = story.id
        readerSessionOwnerId = activeOwnerId
        readerSessionCompleted = story.isCompleted
        mutableUiState.update { it.copy(selectedChapterNumber = number) }
        loadChapter(story.id, number)
        return number
    }

    fun selectChapter(chapterNumber: Int) {
        val story = mutableUiState.value.selectedStory ?: return
        mutableUiState.update { it.copy(selectedChapterNumber = chapterNumber) }
        loadChapter(story.id, chapterNumber)
    }

    fun ensureChapterLoaded(chapterNumber: Int) {
        val story = mutableUiState.value.selectedStory ?: return
        loadChapter(story.id, chapterNumber)
    }

    fun closeStory() {
        selectedStoryJob?.cancel()
        chaptersJob?.cancel()
        refreshJob?.cancel()
        readerSessionStoryId = null
        mutableUiState.update {
            it.copy(
                selectedStory = null,
                chapters = emptyList(),
                selectedChapterNumber = null,
                isLoadingStory = false,
                isDownloadingOffline = false,
                offlineMessage = null,
                errorMessage = null
            )
        }
    }

    fun saveReadingProgress(progressPercent: Int, chapterNumber: Int) {
        val story = mutableUiState.value.selectedStory ?: return
        if (progressPercent >= 100) readerSessionCompleted = true
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

    fun recordReadingSession(storyId: String, secondsRead: Int, isCompleted: Boolean) {
        if (secondsRead < MIN_READING_SESSION_SECONDS) return
        if (readerSessionStoryId != storyId) return
        val ownerId = readerSessionOwnerId
        if (ownerId == GUEST_OWNER_ID) return
        viewModelScope.launch {
            val result = readingStatsRepository.recordSession(
                ownerId = ownerId,
                storyId = storyId,
                secondsRead = secondsRead,
                isCompleted = readerSessionCompleted || isCompleted
            )
            result.onFailure(::setError)
            if (result.isSuccess && ownerId == activeOwnerId) {
                readingStatsRepository.sync(ownerId)
            }
        }
    }

    fun updateReaderPreferences(preferences: ReaderPreferences) {
        if (preferences == mutableUiState.value.readerPreferences) return
        readerPreferencesRepository.save(preferences)
        mutableUiState.update { it.copy(readerPreferences = preferences) }
    }

    fun resetReaderPreferences() {
        updateReaderPreferences(ReaderPreferences())
    }

    fun downloadStoryForOffline() {
        val state = mutableUiState.value
        val story = state.selectedStory ?: return
        if (state.isDownloadingOffline) return
        mutableUiState.update { it.copy(isDownloadingOffline = true, offlineMessage = null) }
        viewModelScope.launch {
            repository.downloadStoryForOffline(story.id).fold(
                onSuccess = { count ->
                    if (mutableUiState.value.selectedStory?.id == story.id) {
                        val unitLabel = if (count == 1) "chapter" else "chapters"
                        mutableUiState.update {
                            it.copy(
                                isDownloadingOffline = false,
                                offlineMessage = "Saved $count $unitLabel for offline reading."
                            )
                        }
                    }
                },
                onFailure = { error ->
                    if (mutableUiState.value.selectedStory?.id == story.id) {
                        mutableUiState.update {
                            it.copy(
                                isDownloadingOffline = false,
                                offlineMessage = error.message?.takeIf(String::isNotBlank)
                                    ?: "Could not save chapters offline."
                            )
                        }
                    }
                }
            )
        }
    }

    fun removeOfflineDownload() {
        val story = mutableUiState.value.selectedStory ?: return
        if (mutableUiState.value.isDownloadingOffline) return
        viewModelScope.launch {
            repository.removeOfflineChapters(story.id).fold(
                onSuccess = {
                    if (mutableUiState.value.selectedStory?.id == story.id) {
                        mutableUiState.update { it.copy(offlineMessage = "Removed offline chapters.") }
                    }
                },
                onFailure = { error ->
                    if (mutableUiState.value.selectedStory?.id == story.id) {
                        mutableUiState.update {
                            it.copy(
                                offlineMessage = error.message?.takeIf(String::isNotBlank)
                                    ?: "Could not remove offline cache."
                            )
                        }
                    }
                }
            )
        }
    }

    fun clearOfflineMessage() {
        mutableUiState.update { it.copy(offlineMessage = null) }
    }

    private fun startStoryObservation(
        storyId: String,
        initialStory: Story?,
        initialChapterNumber: Int?,
        startReadingChapter: Int?
    ) {
        selectedStoryJob?.cancel()
        chaptersJob?.cancel()
        refreshJob?.cancel()
        mutableUiState.update {
            it.copy(
                selectedStory = initialStory,
                chapters = emptyList(),
                selectedChapterNumber = initialChapterNumber,
                isLoadingStory = true,
                isDownloadingOffline = false,
                offlineMessage = null,
                errorMessage = null
            )
        }
        selectedStoryJob = repository.observeStory(storyId)
            .onEach { cached ->
                if (cached != null) {
                    mutableUiState.update { state -> state.copy(selectedStory = cached) }
                }
            }
            .launchIn(viewModelScope)
        chaptersJob = repository.observeChapters(storyId)
            .onEach { chapters -> mutableUiState.update { it.copy(chapters = chapters) } }
            .launchIn(viewModelScope)
        refreshJob = viewModelScope.launch {
            repository.refreshStory(storyId).onFailure(::setError)
            repository.refreshChapters(storyId).onFailure(::setError)
            mutableUiState.update { it.copy(isLoadingStory = false) }
            if (startReadingChapter != null && mutableUiState.value.selectedStory?.id == storyId) {
                startReading(startReadingChapter)
            }
        }
    }

    private fun loadChapter(storyId: String, chapterNumber: Int) {
        val state = mutableUiState.value
        val chapter = state.chapters.firstOrNull { it.chapterNumber == chapterNumber }
        if (state.chapters.isNotEmpty() && chapter == null) return
        if (
            chapter != null && (
                chapter.content.isNotBlank() || chapter.pageUrls.isNotEmpty() ||
                    (chapterNumber == 1 && state.selectedStory?.content?.isNotBlank() == true)
                )
        ) return
        if (chapter == null && chapterNumber == 1 && state.selectedStory?.content?.isNotBlank() == true) return
        val requestKey = storyId to chapterNumber
        if (!loadingChapterRequests.add(requestKey)) return
        viewModelScope.launch {
            try {
                repository.refreshChapter(storyId, chapterNumber).onFailure { error ->
                    if (mutableUiState.value.selectedStory?.id == storyId) setError(error)
                }
            } finally {
                loadingChapterRequests.remove(requestKey)
            }
        }
    }

    private fun setError(error: Throwable) {
        mutableUiState.update { it.copy(errorMessage = error.message) }
    }

    class Factory(
        private val repository: StoryRepository,
        private val readingStatsRepository: ReadingStatsRepository,
        private val readerPreferencesRepository: ReaderPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ReaderViewModel::class.java))
            return ReaderViewModel(repository, readingStatsRepository, readerPreferencesRepository) as T
        }
    }

    private companion object {
        const val GUEST_OWNER_ID = "guest"
        const val MIN_READING_SESSION_SECONDS = 3
    }
}
