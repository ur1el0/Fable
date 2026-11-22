package com.example.fable.data.repository

import com.example.fable.data.local.ChapterEntity
import com.example.fable.data.local.ShelfStateEntity
import com.example.fable.data.local.StoryDao
import com.example.fable.data.local.StoryEntity
import com.example.fable.data.local.toEntity
import com.example.fable.data.remote.FableApiService
import com.example.fable.data.remote.dto.StoryDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomStoryRepositoryManagementTest {
    @Test
    fun updateStoryPersistsEditedMetadataAndChapterOneLocally() = runTest {
        val storyId = "story-owned"
        val dao = InMemoryStoryDao()
        dao.upsertStories(
            listOf(
                StoryDto(
                    id = storyId,
                    title = "Old Title",
                    author = "Writer",
                    genre = "Fantasy",
                    chapter = "Old Chapter",
                    synopsis = "Old synopsis",
                    content = "Old chapter body",
                    ownerUserId = "owner-1"
                ).toEntity()
            )
        )
        dao.upsertChapters(
            listOf(
                ChapterEntity(
                    id = "chap-1",
                    storyId = storyId,
                    chapterNumber = 1,
                    title = "Old Chapter",
                    content = "Old chapter body",
                    wordCount = 3,
                    createdAtUtc = "2026-11-01T00:00:00Z",
                    pageUrls = emptyList()
                )
            )
        )
        val api = FableApiService(
            baseUrl = "https://fable.test/api/v1/",
            client = OkHttpClient.Builder().addInterceptor(
                Interceptor { chain ->
                    val request = chain.request()
                    assertEquals("PATCH", request.method)
                    assertEquals("/api/v1/stories/$storyId", request.url.encodedPath)
                    jsonResponse(
                        request,
                        200,
                        """
                        {
                          "id":"$storyId",
                          "title":"New Title",
                          "author":"Writer",
                          "genre":"Mystery",
                          "chapter":"New Chapter",
                          "synopsis":"New synopsis",
                          "content":"One two three four five",
                          "readTimeMinutes":1,
                          "ownerUserId":"owner-1",
                          "updatedAtUtc":"2026-11-22T00:00:00Z",
                          "chapters":[
                            {
                              "id":"chap-1",
                              "storyId":"$storyId",
                              "chapterNumber":1,
                              "title":"New Chapter",
                              "content":"One two three four five",
                              "wordCount":5,
                              "createdAtUtc":"2026-11-01T00:00:00Z"
                            }
                          ]
                        }
                        """.trimIndent()
                    )
                }
            ).build(),
            accessTokenProvider = { "token-1" }
        )
        val repository = RoomStoryRepository(
            api = api,
            dao = dao,
            deviceId = "device-1",
            accessTokenProvider = { "token-1" }
        )

        val updated = repository.updateStory(
            storyId = storyId,
            title = " New Title ",
            genre = " Mystery ",
            chapterTitle = " New Chapter ",
            synopsis = " New synopsis ",
            content = "One two three four five"
        ).getOrThrow()

        assertEquals("New Title", updated.title)
        assertEquals("owner-1", updated.ownerUserId)
        val storedStory = repository.observeStory(storyId).first()!!
        assertEquals("New Title", storedStory.title)
        assertEquals("Mystery", storedStory.genre)
        assertEquals("New Chapter", storedStory.chapter)
        assertEquals("One two three four five", storedStory.content)
        val storedChapter = repository.observeChapters(storyId).first().single()
        assertEquals("New Chapter", storedChapter.title)
        assertEquals("One two three four five", storedChapter.content)
    }

    @Test
    fun deleteStoryRemovesLocalCacheOnlyAfterRemoteSuccess() = runTest {
        val storyId = "story-to-delete"
        val dao = InMemoryStoryDao()
        dao.upsertStories(
            listOf(
                StoryDto(
                    id = storyId,
                    title = "Owned Story",
                    author = "Writer",
                    genre = "Fantasy",
                    synopsis = "Synopsis",
                    content = "Body",
                    ownerUserId = "owner-1"
                ).toEntity()
            )
        )
        dao.upsertShelfState(
            ShelfStateEntity(
                ownerId = "owner-1",
                storyId = storyId,
                isBookmarked = true,
                isCompleted = false,
                progressPercent = 40,
                currentPage = 3,
                lastReadChapterId = null,
                lastReadChapterNumber = 1,
                updatedAtUtc = "2026-11-01T00:00:00Z"
            )
        )
        var statusCode = 500
        val api = FableApiService(
            baseUrl = "https://fable.test/api/v1/",
            client = OkHttpClient.Builder().addInterceptor { chain ->
                jsonResponse(chain.request(), statusCode, if (statusCode == 204) "" else """{"detail":"Server error"}""")
            }.build(),
            accessTokenProvider = { "token-1" }
        )
        val repository = RoomStoryRepository(
            api = api,
            dao = dao,
            deviceId = "device-1",
            accessTokenProvider = { "token-1" }
        )

        assertTrue(repository.deleteStory(storyId).isFailure)
        assertEquals(storyId, dao.getStory(storyId)?.id)

        statusCode = 204
        assertTrue(repository.deleteStory(storyId).isSuccess)
        assertNull(dao.getStory(storyId))
        assertNull(dao.getShelfState("owner-1", storyId))
    }

    @Test
    fun downloadAndRemoveOfflineChaptersManageOfflineIdsWithoutDroppingShelfProgress() = runTest {
        val storyId = "story-multi"
        val dao = InMemoryStoryDao()
        dao.upsertStories(
            listOf(
                StoryDto(
                    id = storyId,
                    title = "Serial Novel",
                    author = "Writer",
                    genre = "Drama",
                    synopsis = "Synopsis",
                    content = "",
                    totalChapters = 2
                ).toEntity()
            )
        )
        dao.upsertShelfState(
            ShelfStateEntity(
                ownerId = "owner-1",
                storyId = storyId,
                isBookmarked = true,
                isCompleted = false,
                progressPercent = 50,
                currentPage = 4,
                lastReadChapterId = "chap-1",
                lastReadChapterNumber = 1,
                updatedAtUtc = "2026-11-10T00:00:00Z"
            )
        )
        val api = FableApiService(
            baseUrl = "https://fable.test/api/v1/",
            client = OkHttpClient.Builder().addInterceptor { chain ->
                val request = chain.request()
                when (request.url.encodedPath) {
                    "/api/v1/stories/$storyId" -> jsonResponse(
                        request,
                        200,
                        """
                        {
                          "id":"$storyId",
                          "title":"Serial Novel",
                          "author":"Writer",
                          "genre":"Drama",
                          "synopsis":"Synopsis",
                          "content":"",
                          "totalChapters":2
                        }
                        """.trimIndent()
                    )
                    "/api/v1/stories/$storyId/chapters" -> jsonResponse(
                        request,
                        200,
                        """
                        [
                          {"id":"chap-1","storyId":"$storyId","chapterNumber":1,"title":"Chapter 1","content":"","wordCount":120},
                          {"id":"chap-2","storyId":"$storyId","chapterNumber":2,"title":"Chapter 2","content":"","wordCount":140}
                        ]
                        """.trimIndent()
                    )
                    "/api/v1/stories/$storyId/chapters/1" -> jsonResponse(
                        request,
                        200,
                        """{"id":"chap-1","storyId":"$storyId","chapterNumber":1,"title":"Chapter 1","content":"Chapter one full text","wordCount":120}"""
                    )
                    "/api/v1/stories/$storyId/chapters/2" -> jsonResponse(
                        request,
                        200,
                        """{"id":"chap-2","storyId":"$storyId","chapterNumber":2,"title":"Chapter 2","content":"Chapter two full text","wordCount":140}"""
                    )
                    else -> error("Unexpected path: ${request.url.encodedPath}")
                }
            }.build()
        )
        val repository = RoomStoryRepository(
            api = api,
            dao = dao,
            deviceId = "device-1",
            accessTokenProvider = { null }
        )
        repository.setShelfOwner("owner-1")

        assertEquals(emptySet<String>(), repository.observeDownloadedStoryIds().first())

        val downloadedChapters = repository.downloadStoryForOffline(storyId).getOrThrow()

        assertEquals(2, downloadedChapters)
        assertEquals(setOf(storyId), repository.observeDownloadedStoryIds().first())
        assertEquals(2, repository.observeChapters(storyId).first().size)

        repository.removeOfflineChapters(storyId).getOrThrow()

        assertEquals(emptySet<String>(), repository.observeDownloadedStoryIds().first())
        assertEquals(emptyList<com.example.fable.domain.model.Chapter>(), repository.observeChapters(storyId).first())
        val shelfStory = repository.observeShelf().first().single()
        assertTrue(shelfStory.isBookmarked)
        assertEquals(50, shelfStory.progressPercent)
        assertEquals("", shelfStory.content)
    }

    @Test
    fun purgeUserDataDeletesOnlyDeletedUsersStoriesAndShelfStates() = runTest {
        val dao = InMemoryStoryDao()
        dao.upsertStories(
            listOf(
                StoryDto(id = "user-a-story", title = "A Story", ownerUserId = "user-a").toEntity(),
                StoryDto(id = "user-b-story", title = "B Story", ownerUserId = "user-b").toEntity(),
                StoryDto(id = "public-story", title = "Public Story", ownerUserId = null).toEntity()
            )
        )
        dao.upsertShelfStates(
            listOf(
                ShelfStateEntity("user-a", "public-story", true, false, 20, 1, null, 1, "2026-11-10T00:00:00Z"),
                ShelfStateEntity("user-b", "public-story", true, false, 60, 2, null, 2, "2026-11-10T00:00:00Z")
            )
        )
        val repository = RoomStoryRepository(
            api = FableApiService(baseUrl = "https://fable.test/api/v1/"),
            dao = dao,
            deviceId = "device-1",
            accessTokenProvider = { null }
        )

        repository.purgeUserData("user-a").getOrThrow()

        assertNull(dao.getStory("user-a-story"))
        assertEquals("user-b-story", dao.getStory("user-b-story")?.id)
        assertEquals("public-story", dao.getStory("public-story")?.id)
        assertNull(dao.getShelfState("user-a", "public-story"))
        assertFalse(dao.getShelfStates("user-b").isEmpty())
    }

    private fun jsonResponse(request: Request, statusCode: Int, body: String): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(statusCode)
            .message(if (statusCode in 200..299) "OK" else "Error")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()

    private class InMemoryStoryDao : StoryDao {
        private val storiesState = MutableStateFlow<Map<String, StoryEntity>>(emptyMap())
        private val shelfState = MutableStateFlow<Map<Pair<String, String>, ShelfStateEntity>>(emptyMap())
        private val chaptersState = MutableStateFlow<Map<String, List<ChapterEntity>>>(emptyMap())

        override fun observeCatalog(): Flow<List<StoryEntity>> =
            storiesState.map { it.values.toList() }

        override fun observeStory(storyId: String): Flow<StoryEntity?> =
            storiesState.map { it[storyId] }

        override suspend fun getStory(storyId: String): StoryEntity? =
            storiesState.value[storyId]

        override suspend fun getStories(storyIds: List<String>): List<StoryEntity> =
            storyIds.mapNotNull(storiesState.value::get)

        override suspend fun upsertStories(stories: List<StoryEntity>) {
            storiesState.value = storiesState.value.toMutableMap().apply {
                stories.forEach { put(it.id, it) }
            }
        }

        override fun observeShelfStates(ownerId: String): Flow<List<ShelfStateEntity>> =
            shelfState.map { map -> map.values.filter { it.ownerId == ownerId } }

        override suspend fun getShelfStates(ownerId: String): List<ShelfStateEntity> =
            shelfState.value.values.filter { it.ownerId == ownerId }

        override suspend fun getShelfState(ownerId: String, storyId: String): ShelfStateEntity? =
            shelfState.value[ownerId to storyId]

        override suspend fun upsertShelfState(state: ShelfStateEntity) {
            shelfState.value = shelfState.value.toMutableMap().apply {
                put(state.ownerId to state.storyId, state)
            }
        }

        override suspend fun upsertShelfStates(states: List<ShelfStateEntity>) {
            shelfState.value = shelfState.value.toMutableMap().apply {
                states.forEach { put(it.ownerId to it.storyId, it) }
            }
        }

        override fun observeChapters(storyId: String): Flow<List<ChapterEntity>> =
            chaptersState.map { (it[storyId] ?: emptyList()).sortedBy(ChapterEntity::chapterNumber) }

        override suspend fun getChapter(storyId: String, chapterNumber: Int): ChapterEntity? =
            chaptersState.value[storyId]?.firstOrNull { it.chapterNumber == chapterNumber }

        override suspend fun deleteChapters(storyId: String) {
            chaptersState.value = chaptersState.value.toMutableMap().apply { remove(storyId) }
        }

        override suspend fun upsertChapters(chapters: List<ChapterEntity>) {
            if (chapters.isEmpty()) return
            val updated = chaptersState.value.toMutableMap()
            chapters.groupBy(ChapterEntity::storyId).forEach { (storyId, incoming) ->
                val byNumber = (updated[storyId] ?: emptyList())
                    .associateBy(ChapterEntity::chapterNumber)
                    .toMutableMap()
                incoming.forEach { byNumber[it.chapterNumber] = it }
                updated[storyId] = byNumber.values.sortedBy(ChapterEntity::chapterNumber)
            }
            chaptersState.value = updated
        }

        override fun observeDownloadedStoryIds(): Flow<List<String>> =
            combine(storiesState, chaptersState) { storyMap, chapterMap ->
                buildSet {
                    storyMap.values.filter { it.content.isNotBlank() }.forEach { add(it.id) }
                    chapterMap.forEach { (id, list) ->
                        if (list.any { it.content.isNotBlank() || it.pageUrls.isNotEmpty() }) add(id)
                    }
                }.toList()
            }

        override suspend fun clearStoryContent(storyId: String) {
            storiesState.value = storiesState.value.toMutableMap().apply {
                this[storyId]?.let { put(storyId, it.copy(content = "")) }
            }
        }

        override suspend fun deleteStory(storyId: String) {
            storiesState.value = storiesState.value.toMutableMap().apply { remove(storyId) }
            deleteChapters(storyId)
        }

        override suspend fun deleteStoriesByOwner(ownerId: String) {
            val removedIds = storiesState.value.values.filter { it.ownerUserId == ownerId }.map(StoryEntity::id)
            storiesState.value = storiesState.value.toMutableMap().apply {
                removedIds.forEach { remove(it) }
            }
            removedIds.forEach { deleteChapters(it) }
        }

        override suspend fun deleteShelfStatesForStory(storyId: String) {
            shelfState.value = shelfState.value.filterKeys { (_, id) -> id != storyId }
        }

        override suspend fun deleteShelfStatesForOwner(ownerId: String) {
            shelfState.value = shelfState.value.filterKeys { (owner, _) -> owner != ownerId }
        }
    }
}
