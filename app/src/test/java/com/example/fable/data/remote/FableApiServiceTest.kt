package com.example.fable.data.remote

import com.example.fable.data.remote.dto.ShelfSyncItemDto
import com.example.fable.data.remote.dto.ShelfSyncRequestDto
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.MediaType.Companion.toMediaType
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.ConcurrentLinkedQueue

class FableApiServiceTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun fetchHealthDecodesBackendHealthPayloadWithoutAuthHeader() = runTest {
        val recorder = JsonResponseInterceptor(
            body = """{"status":"healthy","database":"connected","timestamp_utc":"2026-11-07T12:45:16.295935Z"}"""
        )
        val service = FableApiService(
            baseUrl = "https://fable.test/api/v1/",
            client = OkHttpClient.Builder().addInterceptor(recorder).build(),
            accessTokenProvider = { "unused-token" }
        )

        val health = service.fetchHealth()

        assertEquals("healthy", health.status)
        assertEquals("connected", health.database)
        assertEquals("2026-11-07T12:45:16.295935Z", health.timestampUtc)
        val request = recorder.requests.single()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/health", request.url.encodedPath)
        assertNull(request.header("Authorization"))
    }

    @Test
    fun fetchGutenbergStoriesBuildsVersionedQueryAndDecodesResponse() = runTest {
        val recorder = JsonResponseInterceptor(
            body = """[{"id":"book-42","title":"Alice in Wonderland","providerId":"42"}]"""
        )
        val service = service(recorder)

        val stories = service.fetchGutenbergStories(topic = "Folklore", search = "Alice")

        assertEquals("book-42", stories.single().id)
        assertEquals("Alice in Wonderland", stories.single().title)
        val request = recorder.requests.single()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/public/gutenberg", request.url.encodedPath)
        assertEquals("Folklore", request.url.queryParameter("topic"))
        assertEquals("Alice", request.url.queryParameter("search"))
    }

    @Test
    fun fetchGutenbergStoriesDecodesMetadataOnlyPayloadWithoutChapters() = runTest {
        val recorder = JsonResponseInterceptor(
            body = """
                [
                  {
                    "id":"00000000-0000-0000-0000-00000000000b",
                    "title":"Alice's Adventures in Wonderland",
                    "author":"Lewis Carroll",
                    "genre":"Fantasy",
                    "sourceProvider":"GUTENBERG",
                    "providerId":"11",
                    "providerDownloadCount":64000
                  }
                ]
            """.trimIndent()
        )
        val service = service(recorder)

        val story = service.fetchGutenbergStories(search = "Alice").single()

        assertEquals("00000000-0000-0000-0000-00000000000b", story.id)
        assertEquals("GUTENBERG", story.sourceProvider)
        assertEquals("11", story.providerId)
        assertEquals("", story.content)
        assertEquals("", story.synopsis)
        assertEquals(0, story.totalChapters)
        assertNull(story.chapters)
    }

    @Test
    fun fetchGutenbergChaptersDecodesChapterListFromPublicEndpoint() = runTest {
        val recorder = JsonResponseInterceptor(
            body = """
                [
                  {
                    "id":"00000000-0000-0000-0001-0000000b0001",
                    "storyId":"00000000-0000-0000-0000-00000000000b",
                    "chapterNumber":1,
                    "title":"CHAPTER I. Down the Rabbit-Hole",
                    "content":"Alice was beginning to get very tired of sitting by her sister on the bank.",
                    "wordCount":2180,
                    "createdAtUtc":"2025-01-01T00:00:00Z"
                  }
                ]
            """.trimIndent()
        )
        val service = service(recorder)

        val chapters = service.fetchGutenbergChapters(11)

        assertEquals(1, chapters.size)
        assertEquals(1, chapters.single().chapterNumber)
        assertEquals("CHAPTER I. Down the Rabbit-Hole", chapters.single().title)
        assertEquals(2180, chapters.single().wordCount)
        val request = recorder.requests.single()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/public/gutenberg/11/chapters", request.url.encodedPath)
    }

    @Test
    fun fetchChapterRequestsSingleStoryChapterOnDemand() = runTest {
        val recorder = JsonResponseInterceptor(
            body = """
                {
                  "id":"chapter-2",
                  "storyId":"story-1",
                  "chapterNumber":2,
                  "title":"Chapter Two",
                  "content":"Second chapter body",
                  "wordCount":310
                }
            """.trimIndent()
        )
        val service = service(recorder)

        val chapter = service.fetchChapter(storyId = "story-1", chapterNumber = 2)

        assertEquals("chapter-2", chapter.id)
        assertEquals(2, chapter.chapterNumber)
        assertEquals("Second chapter body", chapter.content)
        val request = recorder.requests.single()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/stories/story-1/chapters/2", request.url.encodedPath)
    }

    @Test
    fun fetchStoriesSendsFilterAndPaginationQueryParameters() = runTest {
        val recorder = JsonResponseInterceptor(body = "[]")
        val service = service(recorder)

        val stories = service.fetchStories(
            genre = "Fantasy",
            search = "Alice",
            since = "2026-11-01T00:00:00Z",
            limit = 25,
            offset = 10
        )

        assertTrue(stories.isEmpty())
        val request = recorder.requests.single()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/stories", request.url.encodedPath)
        assertEquals("Fantasy", request.url.queryParameter("genre"))
        assertEquals("Alice", request.url.queryParameter("search"))
        assertEquals("2026-11-01T00:00:00Z", request.url.queryParameter("since"))
        assertEquals("25", request.url.queryParameter("limit"))
        assertEquals("10", request.url.queryParameter("offset"))
    }

    @Test
    fun fetchStoriesOmitsUnsetAndBlankOptionalQueryParameters() = runTest {
        val recorder = JsonResponseInterceptor(body = "[]")
        val service = service(recorder)

        service.fetchStories()
        service.fetchStories(genre = "", search = "", since = "")

        assertEquals(2, recorder.requests.size)
        recorder.requests.forEach { request ->
            assertEquals(setOf("limit", "offset"), request.url.queryParameterNames)
        }
    }

    @Test
    fun fetchShelfSendsDeviceIdAndBearerToken() = runTest {
        val recorder = JsonResponseInterceptor(body = "[]")
        val service = service(recorder)

        assertTrue(service.fetchShelf(deviceId = "device-1", accessToken = "session-token").isEmpty())

        val request = recorder.requests.single()
        assertEquals("/api/v1/shelf", request.url.encodedPath)
        assertEquals("device-1", request.url.queryParameter("deviceId"))
        assertEquals("Bearer session-token", request.header("Authorization"))
    }

    @Test
    fun syncShelfPostsDeviceIdAndItemsWithBearerToken() = runTest {
        val recorder = JsonResponseInterceptor(
            body = """
                {
                  "status":"ok",
                  "reconciledItems":[
                    {
                      "storyId":"story-1",
                      "readingProgress":0.75,
                      "isBookmarked":true,
                      "isCompleted":false,
                      "updatedAtUtc":"2026-11-07T12:00:00Z"
                    }
                  ],
                  "serverTimeUtc":"2026-11-07T12:00:01Z"
                }
            """.trimIndent()
        )
        val service = service(recorder)

        val response = service.syncShelf(
            request = ShelfSyncRequestDto(
                deviceId = "device-1",
                items = listOf(
                    ShelfSyncItemDto(
                        storyId = "story-1",
                        readingProgress = 0.75,
                        isBookmarked = true,
                        isCompleted = false,
                        updatedAtUtc = "2026-11-07T12:00:00Z"
                    )
                )
            ),
            accessToken = "session-token"
        )

        assertEquals("ok", response.status)
        assertEquals(1, response.reconciledItems.size)
        assertEquals("story-1", response.reconciledItems.single().storyId)
        val request = recorder.requests.single()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/shelf/sync", request.url.encodedPath)
        assertEquals("Bearer session-token", request.header("Authorization"))
        val bodyBuffer = Buffer().also { requireNotNull(request.body).writeTo(it) }
        val payload = json.parseToJsonElement(bodyBuffer.readUtf8()).jsonObject
        assertEquals("device-1", payload.getValue("deviceId").jsonPrimitive.content)
        assertEquals(1, payload.getValue("items").jsonArray.size)
    }

    @Test
    fun fetchGutenbergStoriesMapsBackendErrorToApiException() = runTest {
        val recorder = JsonResponseInterceptor(
            statusCode = 502,
            body = """{"detail":"Gutenberg catalog service is unavailable"}"""
        )
        val service = service(recorder)

        val error = try {
            service.fetchGutenbergStories()
            throw AssertionError("Expected FableApiException")
        } catch (exception: FableApiException) {
            exception
        }

        assertEquals(502, error.statusCode)
        assertEquals("Gutenberg catalog service is unavailable", error.message)
    }

    @Test
    fun blankBaseUrlThrowsApiExceptionWithStatusZero() = runTest {
        val recorder = JsonResponseInterceptor(body = "[]")
        val service = FableApiService(
            baseUrl = "   ",
            client = OkHttpClient.Builder().addInterceptor(recorder).build()
        )

        val error = try {
            service.fetchHealth()
            throw AssertionError("Expected FableApiException")
        } catch (exception: FableApiException) {
            exception
        }

        assertEquals(0, error.statusCode)
        assertEquals("Fable API URL is not configured.", error.message)
        assertTrue(recorder.requests.isEmpty())
    }

    @Test
    fun decodesFastApiValidationErrorsFromListDetailIn422Response() = runTest {
        val recorder = JsonResponseInterceptor(
            statusCode = 422,
            body = """{"detail":[{"loc":["body","password"],"msg":"String should have at least 12 characters","type":"string_too_short"}]}"""
        )
        val service = service(recorder)

        val error = try {
            service.register(com.example.fable.data.remote.dto.RegisterRequestDto("user@test.com", "short", "User"))
            throw AssertionError("Expected FableApiException")
        } catch (exception: FableApiException) {
            exception
        }

        assertEquals(422, error.statusCode)
        assertEquals("String should have at least 12 characters", error.message)
    }

    @Test
    fun fetchStoriesOmitsBlankAndNullFilterQueryParameters() = runTest {
        val recorder = JsonResponseInterceptor(body = "[]")
        val service = service(recorder)

        service.fetchStories(
            genre = null,
            search = "  ",
            since = null,
            limit = 50,
            offset = 0
        )

        val request = recorder.requests.single()
        assertNull(request.url.queryParameter("genre"))
        assertNull(request.url.queryParameter("search"))
        assertNull(request.url.queryParameter("since"))
        assertEquals("50", request.url.queryParameter("limit"))
        assertEquals("0", request.url.queryParameter("offset"))
    }

    private fun service(interceptor: JsonResponseInterceptor) = FableApiService(
        baseUrl = "https://fable.test/api/v1/",
        client = OkHttpClient.Builder().addInterceptor(interceptor).build()
    )

    private class JsonResponseInterceptor(
        private val statusCode: Int = 200,
        private val body: String
    ) : Interceptor {
        val requests = ConcurrentLinkedQueue<Request>()

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            requests.add(request)
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(statusCode)
                .message(if (statusCode in 200..299) "OK" else "Bad Gateway")
                .body(body.toResponseBody("application/json".toMediaType()))
                .build()
        }
    }
}
