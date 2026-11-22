package com.example.fable.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fable.domain.model.GenreCategory
import com.example.fable.domain.model.ReadingStats
import com.example.fable.domain.model.Story
import com.example.fable.domain.model.UpdateFeed
import com.example.fable.domain.model.Writer
import com.example.fable.domain.repository.ReadingStatsRepository
import com.example.fable.domain.repository.StoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CatalogUiState(
    val stories: List<Story> = emptyList(),
    val shelfStories: List<Story> = emptyList(),
    val offlineStoryIds: Set<String> = emptySet(),
    val genres: List<GenreCategory> = emptyList(),
    val writers: List<Writer> = emptyList(),
    val updateFeed: UpdateFeed? = null,
    val readingStats: ReadingStats = ReadingStats(),
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

class CatalogViewModel(
    private val repository: StoryRepository,
    private val readingStatsRepository: ReadingStatsRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = mutableUiState.asStateFlow()

    private var shelfOwnerJob: Job? = null
    private var readingStatsJob: Job? = null
    private var activeShelfOwnerId = GUEST_OWNER_ID

    init {
        repository.observeCatalog()
            .onEach { stories -> mutableUiState.update { it.copy(stories = stories) } }
            .launchIn(viewModelScope)
        repository.observeShelf()
            .onEach { stories -> mutableUiState.update { it.copy(shelfStories = stories) } }
            .launchIn(viewModelScope)
        repository.observeDownloadedStoryIds()
            .onEach { offlineIds -> mutableUiState.update { it.copy(offlineStoryIds = offlineIds) } }
            .launchIn(viewModelScope)
        refreshCatalog()
    }

    fun refreshCatalog() {
        if (mutableUiState.value.isRefreshing) return
        viewModelScope.launch {
            mutableUiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            coroutineScope {
                val storiesDeferred = async { repository.refreshStories() }
                val genresDeferred = async { runCancellableCatching { repository.fetchGenres() } }
                val authorsDeferred = async { runCancellableCatching { repository.fetchTopAuthors() } }
                val updatesDeferred = async { repository.refreshUpdateFeed() }
                val gutenbergDeferred = async { repository.searchGutenberg(topic = "fiction") }
                val standardEbooksDeferred = async { repository.searchStandardEbooks(topic = "fiction") }

                val storiesResult = storiesDeferred.await()
                val genresResult = genresDeferred.await()
                val authorsResult = authorsDeferred.await()
                val updatesResult = updatesDeferred.await()
                // External provider pre-warming is isolated so transient upstream outages
                // never trigger the global Library error banner.
                gutenbergDeferred.await()
                standardEbooksDeferred.await()

                val coreErrors = listOfNotNull(
                    storiesResult.exceptionOrNull(),
                    genresResult.exceptionOrNull(),
                    authorsResult.exceptionOrNull(),
                    updatesResult.exceptionOrNull()
                )
                mutableUiState.update { state ->
                    state.copy(
                        genres = genresResult.getOrNull() ?: state.genres,
                        writers = authorsResult.getOrNull() ?: state.writers,
                        updateFeed = updatesResult.getOrNull() ?: state.updateFeed,
                        isRefreshing = false,
                        errorMessage = coreErrors.firstNotNullOfOrNull { error ->
                            error.message?.takeIf(String::isNotBlank)
                        }
                    )
                }
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

    fun setShelfOwner(ownerId: String?) {
        val nextOwnerId = ownerId?.takeIf(String::isNotBlank) ?: GUEST_OWNER_ID
        shelfOwnerJob?.cancel()
        shelfOwnerJob = viewModelScope.launch {
            repository.setShelfOwner(ownerId)
            activeShelfOwnerId = nextOwnerId
            mutableUiState.update { it.copy(readingStats = ReadingStats()) }
            readingStatsJob?.cancel()
            readingStatsJob = readingStatsRepository.observeStats(nextOwnerId)
                .onEach { stats ->
                    if (activeShelfOwnerId == nextOwnerId) {
                        mutableUiState.update { it.copy(readingStats = stats) }
                    }
                }
                .launchIn(viewModelScope)
            if (nextOwnerId != GUEST_OWNER_ID) {
                repository.syncShelf()
                readingStatsRepository.sync(nextOwnerId)
            }
        }
    }

    fun refreshReadingStats() {
        val ownerId = activeShelfOwnerId
        if (ownerId == GUEST_OWNER_ID) return
        viewModelScope.launch { readingStatsRepository.sync(ownerId) }
    }

    private suspend inline fun <T> runCancellableCatching(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Throwable) {
        Result.failure(error)
    }

    class Factory(
        private val repository: StoryRepository,
        private val readingStatsRepository: ReadingStatsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(CatalogViewModel::class.java))
            return CatalogViewModel(repository, readingStatsRepository) as T
        }
    }

    private companion object {
        const val GUEST_OWNER_ID = "guest"
    }
}
