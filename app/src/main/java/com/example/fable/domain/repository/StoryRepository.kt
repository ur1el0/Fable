package com.example.fable.domain.repository

import com.example.fable.domain.model.Chapter
import com.example.fable.domain.model.GenreCategory
import com.example.fable.domain.model.Story
import com.example.fable.domain.model.UpdateFeed
import com.example.fable.domain.model.Writer
import kotlinx.coroutines.flow.Flow

interface StoryRepository {
    fun observeCatalog(): Flow<List<Story>>
    fun observeShelf(): Flow<List<Story>>
    fun observeStory(storyId: String): Flow<Story?>
    fun observeChapters(storyId: String): Flow<List<Chapter>>
    suspend fun refreshStories(genre: String? = null, search: String? = null): Result<Unit>
    suspend fun refreshStory(storyId: String): Result<Unit>
    suspend fun refreshChapters(storyId: String): Result<Unit>
    suspend fun refreshChapter(storyId: String, chapterNumber: Int): Result<Unit>
    suspend fun searchGutenberg(topic: String? = null, search: String? = null): Result<List<Story>>
    suspend fun searchManga(search: String? = null, limit: Int = 20, offset: Int = 0): Result<List<Story>>
    suspend fun publishStory(title: String, genre: String, synopsis: String, content: String): Result<Story>
    suspend fun setShelfOwner(ownerId: String?)
    suspend fun syncShelf(): Result<Unit>
    suspend fun toggleBookmark(storyId: String): Result<Boolean>
    suspend fun saveProgress(
        storyId: String,
        progressPercent: Int,
        currentPage: Int,
        chapterId: String?,
        chapterNumber: Int?
    ): Result<Unit>
    suspend fun fetchGenres(): List<GenreCategory>
    suspend fun fetchTopAuthors(): List<Writer>
    suspend fun refreshUpdateFeed(): Result<UpdateFeed>
}
