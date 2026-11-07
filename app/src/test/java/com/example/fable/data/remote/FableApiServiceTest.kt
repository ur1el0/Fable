package com.example.fable.data.remote

import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.MediaType.Companion.toMediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.ConcurrentLinkedQueue

class FableApiServiceTest {
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
