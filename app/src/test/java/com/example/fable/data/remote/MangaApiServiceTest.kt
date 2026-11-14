package com.example.fable.data.remote

import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.concurrent.ConcurrentLinkedQueue

class MangaApiServiceTest {
    @Test
    fun catalogUsesSeparateApiAndResolvesRelativeCoverThroughMangaService() = runTest {
        val recorder = JsonResponseInterceptor(
            body = """[{"id":"story-id","title":"Manga","coverImageUrl":"images/covers/manga-id/cover.jpg?size=256"}]"""
        )
        val service = service(recorder)

        val story = service.fetchMangaStories(search = "Manga", limit = 12, offset = 5).single()

        assertEquals("https://manga.test/api/v1/images/covers/manga-id/cover.jpg?size=256", story.coverImageUrl)
        val request = recorder.requests.single()
        assertEquals("/api/v1/public/manga", request.url.encodedPath)
        assertEquals("Manga", request.url.queryParameter("search"))
        assertEquals("12", request.url.queryParameter("limit"))
        assertEquals("5", request.url.queryParameter("offset"))
        assertNull(request.header("Authorization"))
    }

    @Test
    fun chapterPagesResolveToDedicatedImageProxy() = runTest {
        val recorder = JsonResponseInterceptor(
            body = """[{"id":"chapter","chapterNumber":1,"title":"Chapter 1","pageUrls":["images/pages/provider-chapter/001.jpg"]}]"""
        )
        val service = service(recorder)

        val chapter = service.fetchMangaChapters("manga-id", limit = 30).single()

        assertEquals(
            listOf("https://manga.test/api/v1/images/pages/provider-chapter/001.jpg"),
            chapter.pageUrls
        )
        val request = recorder.requests.single()
        assertEquals("/api/v1/public/manga/manga-id/chapters", request.url.encodedPath)
        assertEquals("30", request.url.queryParameter("limit"))
    }

    @Test
    fun rejectsExternalImageUrlsFromMangaApiResponse() = runTest {
        val recorder = JsonResponseInterceptor(
            body = """[{"id":"story-id","coverImageUrl":"https://example.com/image.jpg"}]"""
        )
        val error = runCatching { service(recorder).fetchMangaStories() }.exceptionOrNull()

        assertEquals(IllegalArgumentException::class.java, error?.javaClass)
    }

    @Test
    fun rejectsImagePathsThatEscapeTheConfiguredBasePath() = runTest {
        val recorder = JsonResponseInterceptor(
            body = """[{"id":"story-id","coverImageUrl":"images/covers/../../../../outside.jpg"}]"""
        )
        val error = runCatching { service(recorder).fetchMangaStories() }.exceptionOrNull()

        assertEquals(IllegalArgumentException::class.java, error?.javaClass)
    }

    private fun service(interceptor: JsonResponseInterceptor) = MangaApiService(
        baseUrl = "https://manga.test/api/v1/",
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
