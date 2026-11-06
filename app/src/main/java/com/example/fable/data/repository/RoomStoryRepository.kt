package com.example.fable.data.repository

import com.example.fable.data.local.StoryDao
import com.example.fable.data.local.StoryEntity
import com.example.fable.data.local.toDomain
import com.example.fable.data.local.toEntity
import com.example.fable.data.remote.FableApiService
import com.example.fable.data.remote.dto.StoryDto
import com.example.fable.data.remote.dto.toDomain
import com.example.fable.domain.model.Chapter
import com.example.fable.domain.model.GenreCategory
import com.example.fable.domain.model.Story
import com.example.fable.domain.model.SourceProvider
import com.example.fable.domain.model.UpdateFeed
import com.example.fable.domain.model.Writer
import com.example.fable.domain.repository.StoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class RoomStoryRepository(
    private val api: FableApiService,
    private val dao: StoryDao
) : StoryRepository {
    override fun observeCatalog(): Flow<List<Story>> =
        dao.observeCatalog().map { entities -> entities.map(StoryEntity::toDomain) }

    override fun observeShelf(): Flow<List<Story>> =
        dao.observeShelf().map { entities -> entities.map(StoryEntity::toDomain) }

    override fun observeStory(storyId: String): Flow<Story?> =
        dao.observeStory(storyId).map { it?.toDomain() }

    override fun observeChapters(storyId: String): Flow<List<Chapter>> =
        dao.observeChapters(storyId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun refreshStories(genre: String?, search: String?): Result<Unit> = runCatching {
        val remote = api.fetchStories(genre = genre, search = search)
        if (remote.isNotEmpty()) {
            val existing = dao.getStories(remote.map { it.id }).associateBy(StoryEntity::id)
            dao.upsertStories(remote.map { it.toEntity(existing[it.id]) })
        }
    }

    override suspend fun refreshStory(storyId: String): Result<Unit> = runCatching {
        val existing = dao.getStory(storyId)
        val remote = when (SourceProvider.fromWireValue(existing?.sourceProvider.orEmpty())) {
            SourceProvider.GUTENBERG -> existing?.providerId?.toIntOrNull()
                ?.let { api.fetchGutenbergStory(it) } ?: api.fetchStory(storyId)
            else -> api.fetchStory(storyId)
        }
        dao.upsertStories(listOf(remote.toEntity(existing)))
    }

    override suspend fun refreshChapters(storyId: String): Result<Unit> = runCatching {
        val story = dao.getStory(storyId)
        val remoteChapters = when (SourceProvider.fromWireValue(story?.sourceProvider.orEmpty())) {
            SourceProvider.GUTENBERG -> story?.providerId?.toIntOrNull()
                ?.let { api.fetchGutenbergChapters(it) } ?: api.fetchChapters(storyId)
            else -> api.fetchChapters(storyId)
        }
        val chapters = remoteChapters.map { it.toEntity() }
        dao.replaceChapters(storyId, chapters)
    }

    override suspend fun refreshChapter(storyId: String, chapterNumber: Int): Result<Unit> = runCatching {
        val story = dao.getStory(storyId)
        val chapter = when (SourceProvider.fromWireValue(story?.sourceProvider.orEmpty())) {
            SourceProvider.GUTENBERG -> story?.providerId?.toIntOrNull()
                ?.let { providerId -> api.fetchGutenbergChapters(providerId).firstOrNull { it.chapterNumber == chapterNumber } }
                ?: api.fetchChapter(storyId, chapterNumber)
            else -> api.fetchChapter(storyId, chapterNumber)
        } ?: error("Chapter $chapterNumber is not available.")
        dao.upsertChapters(listOf(chapter.toEntity()))
    }

    override suspend fun searchGutenberg(topic: String?, search: String?): Result<List<Story>> = runCatching {
        val remote = api.fetchGutenbergStories(topic = topic, search = search)
        persistStories(remote)
        remote.map { it.toDomain() }
    }

    private suspend fun persistStories(remote: List<StoryDto>) {
        if (remote.isEmpty()) return
        val existing = dao.getStories(remote.map { it.id }).associateBy(StoryEntity::id)
        dao.upsertStories(remote.map { it.toEntity(existing[it.id]) })
    }

    override suspend fun toggleBookmark(storyId: String): Result<Boolean> = runCatching {
        val story = requireNotNull(dao.getStory(storyId)) { "Story is not cached." }
        val bookmarked = !story.isBookmarked
        dao.updateShelfState(
            storyId = storyId,
            isBookmarked = bookmarked,
            isCompleted = story.isCompleted,
            progressPercent = story.progressPercent,
            currentPage = story.currentPage,
            chapterId = story.lastReadChapterId,
            chapterNumber = story.lastReadChapterNumber,
            updatedAtUtc = Instant.now().toString()
        )
        bookmarked
    }

    override suspend fun saveProgress(
        storyId: String,
        progressPercent: Int,
        currentPage: Int,
        chapterId: String?,
        chapterNumber: Int?
    ): Result<Unit> = runCatching {
        val story = requireNotNull(dao.getStory(storyId)) { "Story is not cached." }
        val progress = progressPercent.coerceIn(0, 100)
        dao.updateShelfState(
            storyId = storyId,
            isBookmarked = story.isBookmarked,
            isCompleted = story.isCompleted || progress == 100,
            progressPercent = progress,
            currentPage = currentPage.coerceAtLeast(1),
            chapterId = chapterId,
            chapterNumber = chapterNumber,
            updatedAtUtc = Instant.now().toString()
        )
    }

    override suspend fun fetchGenres(): List<GenreCategory> = api.fetchGenres().map { it.toDomain() }

    override suspend fun fetchTopAuthors(): List<Writer> = api.fetchTopAuthors().map { it.toDomain() }

    override suspend fun refreshUpdateFeed(): Result<UpdateFeed> = runCatching {
        val feed = api.fetchUpdateFeed()
        val updates = feed.toDomain()
        val remoteStories = listOfNotNull(feed.taleOfTheDay, feed.curatorSpotlight) + feed.recentSubmissions
        if (remoteStories.isNotEmpty()) {
            val existing = dao.getStories(remoteStories.map { it.id }).associateBy(StoryEntity::id)
            dao.upsertStories(remoteStories.map { it.toEntity(existing[it.id]) })
        }
        updates
    }
}
