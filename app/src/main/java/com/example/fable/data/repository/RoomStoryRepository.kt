package com.example.fable.data.repository

import com.example.fable.data.local.StoryDao
import com.example.fable.data.local.StoryEntity
import com.example.fable.data.local.ShelfStateEntity
import com.example.fable.data.local.toDomain
import com.example.fable.data.local.toEntity
import com.example.fable.data.local.toShelfState
import com.example.fable.data.remote.FableApiService
import com.example.fable.data.remote.dto.CreateStoryRequestDto
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class RoomStoryRepository(
    private val api: FableApiService,
    private val dao: StoryDao
) : StoryRepository {
    private val activeShelfOwnerId = MutableStateFlow(GUEST_OWNER_ID)
    private val shelfMutationMutex = Mutex()
    @OptIn(ExperimentalCoroutinesApi::class)
    private val currentShelfStates = activeShelfOwnerId
        .flatMapLatest(dao::observeShelfStates)
        .map { states -> states.associateBy(ShelfStateEntity::storyId) }
        .distinctUntilChanged()

    override fun observeCatalog(): Flow<List<Story>> =
        combine(dao.observeCatalog(), currentShelfStates) { entities, states ->
            entities.map { entity -> entity.toDomain(states[entity.id]) }
        }

    override fun observeShelf(): Flow<List<Story>> =
        combine(dao.observeCatalog(), currentShelfStates) { entities, states ->
            entities.mapNotNull { entity ->
                val shelfState = states[entity.id] ?: return@mapNotNull null
                entity.toDomain(shelfState).takeIf {
                    it.isBookmarked || it.progressPercent > 0 || it.isCompleted
                }
            }.sortedByDescending { states[it.id]?.updatedAtUtc.orEmpty() }
        }

    override fun observeStory(storyId: String): Flow<Story?> =
        combine(dao.observeStory(storyId), currentShelfStates) { entity, states ->
            entity?.toDomain(states[storyId])
        }

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
            SourceProvider.MANGADEX -> existing?.providerId
                ?.let { api.fetchMangaStory(it) } ?: api.fetchStory(storyId)
            else -> api.fetchStory(storyId)
        }
        dao.upsertStories(listOf(remote.toEntity(existing)))
    }

    override suspend fun refreshChapters(storyId: String): Result<Unit> = runCatching {
        val story = dao.getStory(storyId)
        val remoteChapters = when (SourceProvider.fromWireValue(story?.sourceProvider.orEmpty())) {
            SourceProvider.GUTENBERG -> story?.providerId?.toIntOrNull()
                ?.let { api.fetchGutenbergChapters(it) } ?: api.fetchChapters(storyId)
            SourceProvider.MANGADEX -> story?.providerId
                ?.let { api.fetchMangaChapters(it, limit = 50) } ?: api.fetchChapters(storyId)
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
            SourceProvider.MANGADEX -> story?.providerId
                ?.let { providerId -> api.fetchMangaChapters(providerId, limit = 50).firstOrNull { it.chapterNumber == chapterNumber } }
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

    override suspend fun searchManga(
        search: String?,
        limit: Int,
        offset: Int
    ): Result<List<Story>> = runCatching {
        val remote = api.fetchMangaStories(search = search, limit = limit.coerceIn(1, 50), offset = offset.coerceAtLeast(0))
        persistStories(remote)
        remote.map { it.toDomain() }
    }

    override suspend fun publishStory(
        title: String,
        genre: String,
        synopsis: String,
        content: String
    ): Result<Story> = runCatching {
        val readTimeMinutes = (content.trim().split(Regex("\\s+")).size / 200).coerceAtLeast(1)
        val remote = api.createStory(
            CreateStoryRequestDto(
                title = title.trim(),
                genre = genre.trim(),
                chapter = "Chapter 1",
                synopsis = synopsis.trim(),
                content = content.trim(),
                readTimeMinutes = readTimeMinutes
            )
        )
        val existing = dao.getStory(remote.id)
        dao.upsertStories(listOf(remote.toEntity(existing)))
        remote.toDomain()
    }

    override suspend fun setShelfOwner(ownerId: String?) {
        val nextOwnerId = ownerId?.takeIf(String::isNotBlank) ?: GUEST_OWNER_ID
        shelfMutationMutex.withLock { activeShelfOwnerId.value = nextOwnerId }
    }

    private suspend fun persistStories(remote: List<StoryDto>) {
        if (remote.isEmpty()) return
        val existing = dao.getStories(remote.map { it.id }).associateBy(StoryEntity::id)
        dao.upsertStories(remote.map { it.toEntity(existing[it.id]) })
    }

    override suspend fun toggleBookmark(storyId: String): Result<Boolean> = runCatching {
        shelfMutationMutex.withLock {
            val ownerId = activeShelfOwnerId.value
            val story = requireNotNull(dao.getStory(storyId)) { "Story is not cached." }
            val previous = dao.getShelfState(ownerId, storyId)
                ?: if (ownerId == GUEST_OWNER_ID) story.toShelfState(ownerId) else emptyShelfState(ownerId, storyId)
            val updated = previous.copy(
                isBookmarked = !previous.isBookmarked,
                updatedAtUtc = currentUtcTimestamp()
            )
            dao.upsertShelfState(updated)
            updated.isBookmarked
        }
    }

    override suspend fun saveProgress(
        storyId: String,
        progressPercent: Int,
        currentPage: Int,
        chapterId: String?,
        chapterNumber: Int?
    ): Result<Unit> = runCatching {
        shelfMutationMutex.withLock {
            val ownerId = activeShelfOwnerId.value
            val story = requireNotNull(dao.getStory(storyId)) { "Story is not cached." }
            val previous = dao.getShelfState(ownerId, storyId)
                ?: if (ownerId == GUEST_OWNER_ID) story.toShelfState(ownerId) else emptyShelfState(ownerId, storyId)
            val progress = progressPercent.coerceIn(0, 100)
            dao.upsertShelfState(
                previous.copy(
                    isCompleted = previous.isCompleted || progress == 100,
                    progressPercent = progress,
                    currentPage = currentPage.coerceAtLeast(1),
                    lastReadChapterId = chapterId,
                    lastReadChapterNumber = chapterNumber,
                    updatedAtUtc = currentUtcTimestamp()
                )
            )
        }
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

    private fun currentUtcTimestamp(): String = SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        Locale.US
    ).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date())

    private fun emptyShelfState(ownerId: String, storyId: String) = ShelfStateEntity(
        ownerId = ownerId,
        storyId = storyId,
        isBookmarked = false,
        isCompleted = false,
        progressPercent = 0,
        currentPage = 1,
        lastReadChapterId = null,
        lastReadChapterNumber = null,
        updatedAtUtc = currentUtcTimestamp()
    )

    private companion object {
        const val GUEST_OWNER_ID = "guest"
    }
}
