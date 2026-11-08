package com.example.fable.data.repository

import com.example.fable.data.local.ChapterEntity
import com.example.fable.data.local.StoryDao
import com.example.fable.data.local.StoryEntity
import com.example.fable.data.local.ShelfStateEntity
import com.example.fable.data.local.toDomain
import com.example.fable.data.local.toEntity
import com.example.fable.data.local.toShelfState
import com.example.fable.data.local.toSyncDto
import com.example.fable.data.remote.FableApiService
import com.example.fable.data.remote.dto.CreateStoryRequestDto
import com.example.fable.data.remote.dto.ShelfSyncItemDto
import com.example.fable.data.remote.dto.ShelfSyncRequestDto
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
import java.time.Instant

class RoomStoryRepository(
    private val api: FableApiService,
    private val dao: StoryDao,
    private val deviceId: String,
    private val accessTokenProvider: () -> String?
) : StoryRepository {
    private val activeShelfOwnerId = MutableStateFlow(GUEST_OWNER_ID)
    private val shelfMutationMutex = Mutex()
    private val shelfSyncMutex = Mutex()
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
        val provider = SourceProvider.fromWireValue(existing?.sourceProvider.orEmpty())
        val gutenbergId = resolveGutenbergId(existing, storyId)
        if (existing != null && (provider == SourceProvider.GUTENBERG || gutenbergId != null)) {
            return@runCatching
        }
        val remote = when {
            provider == SourceProvider.GUTENBERG || gutenbergId != null ->
                gutenbergId?.let { api.fetchGutenbergStory(it) } ?: api.fetchStory(storyId)
            provider == SourceProvider.MANGADEX ->
                existing?.providerId?.let { api.fetchMangaStory(it) } ?: api.fetchStory(storyId)
            else -> api.fetchStory(storyId)
        }
        val targetStoryId = remote.id.ifBlank { storyId }
        dao.upsertStories(listOf(remote.copy(id = targetStoryId).toEntity(existing)))
        val embeddedChapters = remote.chapters
            ?.map { it.toEntity().copy(storyId = targetStoryId) }
            .orEmpty()
        if (embeddedChapters.isNotEmpty()) {
            dao.replaceChapters(targetStoryId, embeddedChapters)
            hydrateStoryFromChapters(targetStoryId, embeddedChapters)
        }
    }

    override suspend fun refreshChapters(storyId: String): Result<Unit> = runCatching {
        var story = dao.getStory(storyId)
        if (story == null) {
            refreshStory(storyId).getOrThrow()
            story = dao.getStory(storyId)
        }
        val provider = SourceProvider.fromWireValue(story?.sourceProvider.orEmpty())
        val gutenbergId = resolveGutenbergId(story, storyId)
        val remoteChapters = when {
            provider == SourceProvider.GUTENBERG || gutenbergId != null ->
                gutenbergId?.let { api.fetchGutenbergChapters(it) } ?: api.fetchChapters(storyId)
            provider == SourceProvider.MANGADEX ->
                story?.providerId?.let { api.fetchMangaChapters(it, limit = 50) } ?: api.fetchChapters(storyId)
            else -> api.fetchChapters(storyId)
        }
        val chapters = remoteChapters.map { it.toEntity().copy(storyId = storyId) }
        dao.replaceChapters(storyId, chapters)
        hydrateStoryFromChapters(storyId, chapters)
    }

    override suspend fun refreshChapter(storyId: String, chapterNumber: Int): Result<Unit> = runCatching {
        var story = dao.getStory(storyId)
        if (story == null) {
            refreshStory(storyId).getOrThrow()
            story = dao.getStory(storyId)
        }
        val provider = SourceProvider.fromWireValue(story?.sourceProvider.orEmpty())
        val gutenbergId = resolveGutenbergId(story, storyId)
        when {
            (provider == SourceProvider.GUTENBERG || gutenbergId != null) && gutenbergId != null -> {
                val chapters = api.fetchGutenbergChapters(gutenbergId)
                    .map { it.toEntity().copy(storyId = storyId) }
                if (chapters.none { it.chapterNumber == chapterNumber }) {
                    error("Chapter $chapterNumber is not available.")
                }
                dao.replaceChapters(storyId, chapters)
                hydrateStoryFromChapters(storyId, chapters)
            }
            provider == SourceProvider.MANGADEX && !story?.providerId.isNullOrBlank() -> {
                val chapters = api.fetchMangaChapters(requireNotNull(story?.providerId), limit = 50)
                    .map { it.toEntity().copy(storyId = storyId) }
                if (chapters.none { it.chapterNumber == chapterNumber }) {
                    error("Chapter $chapterNumber is not available.")
                }
                dao.replaceChapters(storyId, chapters)
                hydrateStoryFromChapters(storyId, chapters)
            }
            else -> {
                val chapter = api.fetchChapter(storyId, chapterNumber)
                    .toEntity()
                    .copy(storyId = storyId)
                dao.upsertChapters(listOf(chapter))
                hydrateStoryFromChapters(storyId, listOf(chapter))
            }
        }
    }

    override suspend fun searchGutenberg(topic: String?, search: String?): Result<List<Story>> = runCatching {
        val remote = api.fetchGutenbergStories(topic = topic, search = search)
        val persisted = persistStories(remote)
        persisted.map { it.toDomain() }
    }

    override suspend fun searchManga(
        search: String?,
        limit: Int,
        offset: Int
    ): Result<List<Story>> = runCatching {
        val remote = api.fetchMangaStories(search = search, limit = limit.coerceIn(1, 50), offset = offset.coerceAtLeast(0))
        val persisted = persistStories(remote)
        persisted.map { it.toDomain() }
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

    override suspend fun syncShelf(): Result<Unit> = runCatching {
        val ownerId = activeShelfOwnerId.value
        if (ownerId == GUEST_OWNER_ID) return@runCatching
        val token = accessTokenProvider()?.takeIf(String::isNotBlank) ?: return@runCatching

        shelfSyncMutex.withLock sync@{
            val remoteItems = api.fetchShelf(deviceId, token)
            if (!isCurrentShelfSession(ownerId, token)) return@sync

            cacheMissingShelfStories(remoteItems, ownerId)
            if (!isCurrentShelfSession(ownerId, token)) return@sync

            val plan = shelfMutationMutex.withLock state@{
                if (!isCurrentShelfSession(ownerId, token)) return@state null
                val localStates = dao.getShelfStates(ownerId)
                planShelfSync(ownerId, localStates, remoteItems).also { merge ->
                    dao.upsertShelfStates(merge.serverWins)
                }
            } ?: return@sync

            if (plan.upload.isEmpty()) return@sync
            val response = api.syncShelf(ShelfSyncRequestDto(deviceId, plan.upload), token)
            if (!isCurrentShelfSession(ownerId, token)) return@sync

            shelfMutationMutex.withLock state@{
                if (!isCurrentShelfSession(ownerId, token)) return@state
                val currentStates = dao.getShelfStates(ownerId).associateBy(ShelfStateEntity::storyId)
                val reconciledStates = response.reconciledItems.mapNotNull { remote ->
                    val current = currentStates[remote.storyId]
                    if (current == null || !isNewer(current.updatedAtUtc, remote.updatedAtUtc)) {
                        remote.toEntity(ownerId, previous = current)
                    } else {
                        null
                    }
                }
                dao.upsertShelfStates(reconciledStates)
            }
        }
    }

    override suspend fun syncShelfState(storyId: String): Result<Unit> = runCatching {
        val ownerId = activeShelfOwnerId.value
        if (ownerId == GUEST_OWNER_ID) return@runCatching
        val token = accessTokenProvider()?.takeIf(String::isNotBlank) ?: return@runCatching

        shelfSyncMutex.withLock sync@{
            if (!isCurrentShelfSession(ownerId, token)) return@sync
            val localState = shelfMutationMutex.withLock state@{
                if (!isCurrentShelfSession(ownerId, token)) return@state null
                dao.getShelfState(ownerId, storyId)
            } ?: return@sync

            val response = api.syncShelf(
                ShelfSyncRequestDto(deviceId, listOf(localState.toSyncDto())),
                token
            )
            if (!isCurrentShelfSession(ownerId, token)) return@sync

            shelfMutationMutex.withLock state@{
                if (!isCurrentShelfSession(ownerId, token)) return@state
                val current = dao.getShelfState(ownerId, storyId)
                val reconciled = response.reconciledItems
                    .asSequence()
                    .filter { it.storyId == storyId }
                    .mapNotNull { remote ->
                        if (current == null || !isNewer(current.updatedAtUtc, remote.updatedAtUtc)) {
                            remote.toEntity(ownerId, previous = current)
                        } else {
                            null
                        }
                    }
                    .toList()
                dao.upsertShelfStates(reconciled)
            }
        }
    }

    private suspend fun persistStories(remote: List<StoryDto>): List<StoryEntity> {
        if (remote.isEmpty()) return emptyList()
        val existing = dao.getStories(remote.map { it.id }).associateBy(StoryEntity::id)
        val entities = remote.map { it.toEntity(existing[it.id]) }
        dao.upsertStories(entities)
        return entities
    }

    private suspend fun hydrateStoryFromChapters(storyId: String, chapters: List<ChapterEntity>) {
        if (chapters.isEmpty()) return
        val story = dao.getStory(storyId) ?: return
        val sortedChapters = chapters.sortedBy(ChapterEntity::chapterNumber)
        val chapterOne = sortedChapters.firstOrNull { it.chapterNumber == 1 }
        val maxChapterNumber = sortedChapters.maxOf { it.chapterNumber }
        val totalWords = sortedChapters.sumOf { chapter ->
            if (chapter.wordCount > 0) {
                chapter.wordCount
            } else {
                chapter.content.trim().split(Regex("\\s+")).count(String::isNotBlank)
            }
        }
        val computedReadTime = if (totalWords > 0) (totalWords / 200).coerceAtLeast(1) else 0
        val updatedStory = story.copy(
            chapter = if (chapterOne != null) story.chapter.ifBlank { chapterOne.title } else story.chapter,
            synopsis = if (chapterOne != null) story.synopsis.ifBlank { extractOpeningExcerpt(listOf(chapterOne)) } else story.synopsis,
            content = if (chapterOne != null) story.content.ifBlank { chapterOne.content } else story.content,
            readTimeMinutes = if (story.readTimeMinutes > 0) story.readTimeMinutes else computedReadTime,
            totalChapters = maxOf(story.totalChapters, maxChapterNumber, sortedChapters.size)
        )
        if (updatedStory != story) {
            dao.upsertStories(listOf(updatedStory))
        }
    }

    private fun extractOpeningExcerpt(chapters: List<ChapterEntity>): String {
        for (chapter in chapters) {
            for (paragraph in chapter.content.split(Regex("\\n\\s*\\n"))) {
                val cleaned = paragraph.trim().replace(Regex("\\s+"), " ")
                if (cleaned.length >= 20) {
                    return if (cleaned.length <= 240) cleaned else "${cleaned.take(240).trimEnd()}..."
                }
            }
        }
        return ""
    }

    private fun resolveGutenbergId(story: StoryEntity?, storyId: String): Int? =
        story?.providerId?.toIntOrNull() ?: gutenbergIdForStoryId(storyId)

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

    private suspend fun cacheMissingShelfStories(
        items: List<ShelfSyncItemDto>,
        ownerId: String
    ) {
        val storyIds = items.mapTo(mutableSetOf()) { it.storyId }
        if (storyIds.isEmpty()) return
        val cachedIds = mutableSetOf<String>()
        storyIds.chunked(500).forEach { batch ->
            cachedIds += dao.getStories(batch).map(StoryEntity::id)
        }

        storyIds.filterNot(cachedIds::contains).forEach { storyId ->
            if (activeShelfOwnerId.value != ownerId) return
            val remote = runCatching {
                val gutenbergId = gutenbergIdForStoryId(storyId)
                if (gutenbergId != null) api.fetchGutenbergStory(gutenbergId) else api.fetchStory(storyId)
            }.getOrNull() ?: return@forEach
            val targetStoryId = remote.id.ifBlank { storyId }
            val existing = dao.getStory(targetStoryId)
            dao.upsertStories(listOf(remote.copy(id = targetStoryId).toEntity(existing)))
            val embeddedChapters = remote.chapters
                ?.map { it.toEntity().copy(storyId = targetStoryId) }
                .orEmpty()
            if (embeddedChapters.isNotEmpty()) {
                dao.replaceChapters(targetStoryId, embeddedChapters)
                hydrateStoryFromChapters(targetStoryId, embeddedChapters)
            }
        }
    }

    private fun gutenbergIdForStoryId(storyId: String): Int? {
        val compactId = storyId.replace("-", "")
        if (compactId.length != 32 || compactId.take(24).any { it != '0' }) return null
        return compactId.takeLast(8).toLongOrNull(16)
            ?.takeIf { it in 1..Int.MAX_VALUE.toLong() }
            ?.toInt()
    }

    private fun isCurrentShelfSession(ownerId: String, token: String): Boolean =
        activeShelfOwnerId.value == ownerId && accessTokenProvider() == token

    private fun currentUtcTimestamp(): String = Instant.now().toString()

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
