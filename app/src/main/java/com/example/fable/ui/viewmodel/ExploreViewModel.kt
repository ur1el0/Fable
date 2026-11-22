package com.example.fable.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fable.domain.model.SourceProvider
import com.example.fable.domain.model.Story
import com.example.fable.domain.model.StoryCatalog
import com.example.fable.domain.repository.StoryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExploreUiState(
    val providerStories: List<Story> = emptyList(),
    val isSearchingProvider: Boolean = false,
    val providerErrorMessage: String? = null
)

class ExploreViewModel(
    private val repository: StoryRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = mutableUiState.asStateFlow()

    private var cachedCatalogStories: List<Story> = emptyList()
    private var providerSearchJob: Job? = null

    init {
        repository.observeCatalog()
            .onEach { stories ->
                cachedCatalogStories = stories
                val byId = stories.associateBy(Story::id)
                mutableUiState.update { state ->
                    state.copy(providerStories = state.providerStories.map { item -> byId[item.id] ?: item })
                }
            }
            .launchIn(viewModelScope)
    }

    fun searchProvider(catalog: StoryCatalog, query: String, sort: String? = null) {
        providerSearchJob?.cancel()
        if (catalog == StoryCatalog.FABLE) {
            mutableUiState.update {
                it.copy(providerStories = emptyList(), isSearchingProvider = false, providerErrorMessage = null)
            }
            return
        }
        val cachedStories = cachedProviderStories(catalog, query).let { stories ->
            when (sort) {
                "popular" -> stories.sortedByDescending { it.providerDownloadCount ?: 0 }
                "recent" -> stories.sortedWith(
                    compareByDescending<Story> { it.isRecentSubmission }
                        .thenByDescending { it.updatedAtUtc.ifBlank { it.createdAtUtc } }
                )
                else -> stories
            }
        }
        mutableUiState.update {
            it.copy(providerStories = cachedStories, isSearchingProvider = true, providerErrorMessage = null)
        }
        providerSearchJob = viewModelScope.launch {
            delay(350)
            val normalizedQuery = query.trim().take(200).takeIf(String::isNotEmpty)
            val result = when (catalog) {
                StoryCatalog.FABLE -> Result.success(emptyList())
                StoryCatalog.GUTENBERG -> repository.searchGutenberg(
                    search = normalizedQuery,
                    sort = sort?.takeIf { it in GUTENBERG_SORT_OPTIONS } ?: "catalog"
                )
                StoryCatalog.STANDARD_EBOOKS -> repository.searchStandardEbooks(search = normalizedQuery)
                StoryCatalog.MANGA -> repository.searchManga(search = normalizedQuery)
            }
            result.fold(
                onSuccess = { stories ->
                    val byId = cachedCatalogStories.associateBy(Story::id)
                    val merged = stories.map { item -> byId[item.id] ?: item }
                    mutableUiState.update {
                        it.copy(providerStories = merged, isSearchingProvider = false, providerErrorMessage = null)
                    }
                },
                onFailure = { error ->
                    mutableUiState.update {
                        it.copy(
                            isSearchingProvider = false,
                            providerErrorMessage = error.message?.takeIf(String::isNotBlank)
                                ?: "Could not refresh the provider catalog."
                        )
                    }
                }
            )
        }
    }

    private fun cachedProviderStories(catalog: StoryCatalog, query: String): List<Story> {
        val provider = when (catalog) {
            StoryCatalog.FABLE -> return emptyList()
            StoryCatalog.GUTENBERG -> SourceProvider.GUTENBERG
            StoryCatalog.STANDARD_EBOOKS -> SourceProvider.STANDARD_EBOOKS
            StoryCatalog.MANGA -> SourceProvider.MANGADEX
        }
        val normalizedQuery = query.trim().take(200)
        return cachedCatalogStories.filter { story ->
            story.sourceProvider == provider &&
                (normalizedQuery.isEmpty() || listOf(story.title, story.author, story.genre, story.synopsis)
                    .any { field -> field.contains(normalizedQuery, ignoreCase = true) })
        }
    }

    class Factory(private val repository: StoryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ExploreViewModel::class.java))
            return ExploreViewModel(repository) as T
        }
    }

    private companion object {
        val GUTENBERG_SORT_OPTIONS = setOf("catalog", "popular", "recent")
    }
}
