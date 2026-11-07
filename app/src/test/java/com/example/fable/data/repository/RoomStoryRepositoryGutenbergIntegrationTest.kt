package com.example.fable.data.repository

import com.example.fable.data.local.ChapterEntity
import com.example.fable.data.local.ShelfStateEntity
import com.example.fable.data.local.StoryDao
import com.example.fable.data.local.StoryEntity
import com.example.fable.data.remote.FableApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

class RoomStoryRepositoryGutenbergIntegrationTest {
    @Test
    fun openingCachedGutenbergStoryHydratesChaptersWithoutCallingUncachedStoryDetailEndpoint() = runTest {
        val storyId = "00000000-0000-0000-0000-00000000000b"
        val dao = InMemoryStoryDao()
        val requests = CopyOnWriteArrayList<String>()
        val api = FableApiService(
            baseUrl = "https://fable.test/api/v1/",
            client = OkHttpClient.Builder().addInterceptor(
                Interceptor { chain ->
                    val request = chain.request()
                    val path = request.url.encodedPath
                    requests += path
                    when (path) {
                        "/api/v1/public/gutenberg" -> jsonResponse(
                            request,
                            200,
                            """
                            [
                              {
                                "id": "$storyId",
                                "title": "Alice's Adventures in Wonderland",
                                "author": "Lewis Carroll",
                                "genre": "Fantasy",
                                "chapter": "",
                                "synopsis": "",
                                "content": "",
                                "readTimeMinutes": 0,
                                "totalChapters": 0,
                                "sourceProvider": "GUTENBERG",
                                "providerId": "11",
                                "providerDownloadCount": 64000
                              }
                            ]
                            """.trimIndent()
                        )
                        "/api/v1/public/gutenberg/11" -> jsonResponse(
                            request,
                            502,
                            """{"detail":"Gutenberg catalog service is unavailable"}"""
                        )
                        "/api/v1/public/gutenberg/11/chapters" -> jsonResponse(
                            request,
                            200,
                            """
                            [
                              {
                                "id": "00000000-0000-0000-0001-0000000b0001",
                                "storyId": "$storyId",
                                "chapterNumber": 1,
                                "title": "CHAPTER I. Down the Rabbit-Hole",
                                "content": "Alice was beginning to get very tired of sitting by her sister on the bank, and of having nothing to do.",
                                "wordCount": 2200,
                                "createdAtUtc": "2025-01-01T00:00:00Z"
                              },
                              {
                                "id": "00000000-0000-0000-0001-0000000b0002",
                                "storyId": "$storyId",
                                "chapterNumber": 2,
                                "title": "CHAPTER II. The Pool of Tears",
                                "content": "Curiouser and curiouser! cried Alice (she was so much surprised, that for the moment she quite forgot how to speak good English).",
                                "wordCount": 2100,
                                "createdAtUtc": "2025-01-01T00:00:00Z"
                              }
                            ]
                            """.trimIndent()
                        )
                        else -> error("Unexpected path: $path")
                    }
                }
            ).build()
        )
        val repository = RoomStoryRepository(
            api = api,
            dao = dao,
            deviceId = "device-test",
            accessTokenProvider = { null }
        )

        val initialSearch = repository.searchGutenberg(search = "Alice").getOrThrow()
        assertEquals(1, initialSearch.size)
        assertEquals(0, initialSearch.single().totalChapters)

        assertTrue(repository.refreshStory(storyId).isSuccess)
        assertTrue(repository.refreshChapters(storyId).isSuccess)

        assertEquals(
            listOf("/api/v1/public/gutenberg", "/api/v1/public/gutenberg/11/chapters"),
            requests.toList()
        )

        val hydratedStory = repository.observeStory(storyId).first()!!
        assertEquals(2, hydratedStory.totalChapters)
        assertEquals(21, hydratedStory.readTimeMinutes)
        assertEquals("CHAPTER I. Down the Rabbit-Hole", hydratedStory.chapter)
        assertTrue(hydratedStory.content.startsWith("Alice was beginning to get very tired"))
        assertTrue(hydratedStory.synopsis.startsWith("Alice was beginning to get very tired"))

        val chapters = repository.observeChapters(storyId).first()
        assertEquals(2, chapters.size)
        assertEquals("CHAPTER II. The Pool of Tears", chapters[1].title)

        val secondSearch = repository.searchGutenberg(search = "Alice").getOrThrow()
        assertEquals(2, secondSearch.single().totalChapters)
        assertEquals(21, secondSearch.single().readTimeMinutes)
        assertTrue(secondSearch.single().synopsis.startsWith("Alice was beginning to get very tired"))
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
            chaptersState.value = chaptersState.value.toMutableMap().apply {
                remove(storyId)
            }
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
    }
}
