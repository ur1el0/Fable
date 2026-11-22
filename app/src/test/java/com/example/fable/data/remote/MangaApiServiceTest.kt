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
    fun catalogUsesSharedApiAndAcceptsMangaDexCoverUrl() = runTest {
        val recorder = JsonResponseInterceptor(
            body = """[{"id":"story-id","title":"Manga","coverImageUrl":"https://uploads.mangadex.org/covers/77bee52c-d2d6-44ad-a33a-1734c1fe696a/6079dd31-838b-4d61-87c4-121f3ad19158.jpg.256.jpg"}]"""
        )
        val service = service(recorder)

        val story = service.fetchMangaStories(search = "Manga", limit = 12, offset = 5).single()

        assertEquals(
            "https://uploads.mangadex.org/covers/77bee52c-d2d6-44ad-a33a-1734c1fe696a/6079dd31-838b-4d61-87c4-121f3ad19158.jpg.256.jpg",
            story.coverImageUrl
        )
        val request = recorder.requests.single()
        assertEquals("/api/v1/public/manga", request.url.encodedPath)
        assertEquals("Manga", request.url.queryParameter("search"))
        assertEquals("12", request.url.queryParameter("limit"))
        assertEquals("5", request.url.queryParameter("offset"))
        assertNull(request.header("Authorization"))
    }

    @Test
    fun chapterPagesAcceptMangaDexAtHomeUrls() = runTest {
        val recorder = JsonResponseInterceptor(
            body = """[{"id":"chapter","chapterNumber":1,"title":"Chapter 1","pageUrls":["https://cmdxd98sb0x3yprd.mangadex.network/data-saver/cfdcdc22b8556e8adb22744ed24c05e7/x1-2b8404d2e97502adcf8c9d76515a6e8b4aa1d9ec58267e3729ac9e76a33faffe.jpg"]}]"""
        )
        val service = service(recorder)

        val chapter = service.fetchMangaChapters("manga-id", limit = 30).single()

        assertEquals(
            listOf("https://cmdxd98sb0x3yprd.mangadex.network/data-saver/cfdcdc22b8556e8adb22744ed24c05e7/x1-2b8404d2e97502adcf8c9d76515a6e8b4aa1d9ec58267e3729ac9e76a33faffe.jpg"),
            chapter.pageUrls
        )
        val request = recorder.requests.single()
        assertEquals("/api/v1/public/manga/manga-id/chapters", request.url.encodedPath)
        assertEquals("30", request.url.queryParameter("limit"))
    }

    @Test
    fun dropsExternalCoverUrlWithoutFailingValidCatalogItems() = runTest {
        val validCover = "https://uploads.mangadex.org/covers/77bee52c-d2d6-44ad-a33a-1734c1fe696a/6079dd31-838b-4d61-87c4-121f3ad19158.jpg.256.jpg"
        val recorder = JsonResponseInterceptor(
            body = """
                [
                  {"id":"bad-story","coverImageUrl":"https://example.com/image.jpg"},
                  {"id":"good-story","coverImageUrl":"$validCover"}
                ]
            """.trimIndent()
        )

        val stories = service(recorder).fetchMangaStories()

        assertEquals(2, stories.size)
        assertNull(stories[0].coverImageUrl)
        assertEquals(validCover, stories[1].coverImageUrl)
    }

    @Test
    fun dropsMangaDexLookalikeCoverHostsWithoutFailingCatalog() = runTest {
        val recorder = JsonResponseInterceptor(
            body = """[{"id":"story-id","coverImageUrl":"https://uploads.mangadex.org.example.com/covers/77bee52c-d2d6-44ad-a33a-1734c1fe696a/cover.jpg.256.jpg"}]"""
        )

        val story = service(recorder).fetchMangaStories().single()

        assertNull(story.coverImageUrl)
    }

    @Test
    fun dropsUnexpectedPathsOnMangaDexCoverHostsWithoutFailingCatalog() = runTest {
        val recorder = JsonResponseInterceptor(
            body = """[{"id":"story-id","coverImageUrl":"https://uploads.mangadex.org/admin/77bee52c-d2d6-44ad-a33a-1734c1fe696a/cover.jpg.256.jpg"}]"""
        )

        val story = service(recorder).fetchMangaStories().single()

        assertNull(story.coverImageUrl)
    }

    @Test
    fun dropsInsecureMangaDexCoverUrlsWithoutFailingCatalog() = runTest {
        val recorder = JsonResponseInterceptor(
            body = """[{"id":"story-id","coverImageUrl":"http://uploads.mangadex.org/covers/77bee52c-d2d6-44ad-a33a-1734c1fe696a/cover.jpg.256.jpg"}]"""
        )

        val story = service(recorder).fetchMangaStories().single()

        assertNull(story.coverImageUrl)
    }

    @Test
    fun acceptsPngCoverThumbnailAndNormalizesBlankCoverToNull() = runTest {
        val pngCover = "https://uploads.mangadex.org/covers/77bee52c-d2d6-44ad-a33a-1734c1fe696a/cover-image.png.256.jpg"
        val recorder = JsonResponseInterceptor(
            body = """
                [
                  {"id":"png-story","coverImageUrl":"$pngCover"},
                  {"id":"blank-story","coverImageUrl":"   "}
                ]
            """.trimIndent()
        )

        val stories = service(recorder).fetchMangaStories()

        assertEquals(pngCover, stories[0].coverImageUrl)
        assertNull(stories[1].coverImageUrl)
    }

    @Test
    fun acceptsUploadsHostAndCustomPortAtHomePageUrls() = runTest {
        val uploadsPage = "https://uploads.mangadex.org/data-saver/cfdcdc22b8556e8adb22744ed24c05e7/page-1.png"
        val customPortNodePage = "https://node1.mangadex.network:8443/data/cfdcdc22b8556e8adb22744ed24c05e7/page.1.gif"
        val recorder = JsonResponseInterceptor(
            body = """[{"id":"chapter","chapterNumber":1,"title":"Chapter 1","pageUrls":["$uploadsPage","$customPortNodePage"]}]"""
        )

        val chapter = service(recorder).fetchMangaChapters("manga-id").single()

        assertEquals(listOf(uploadsPage, customPortNodePage), chapter.pageUrls)
    }

    @Test
    fun rejectsUntrustedHttpAndLookalikeChapterPageUrls() = runTest {
        val badUrls = listOf(
            "https://example.com/data/cfdcdc22b8556e8adb22744ed24c05e7/page.jpg",
            "http://cmdxd98sb0x3yprd.mangadex.network/data/cfdcdc22b8556e8adb22744ed24c05e7/page.jpg",
            "https://cmdxd98sb0x3yprd.mangadex.network.example.com/data/cfdcdc22b8556e8adb22744ed24c05e7/page.jpg",
            "https://uploads.mangadex.org:8443/data/cfdcdc22b8556e8adb22744ed24c05e7/page.jpg"
        )
        badUrls.forEach { badUrl ->
            val recorder = JsonResponseInterceptor(
                body = """[{"id":"chapter","chapterNumber":1,"title":"Chapter 1","pageUrls":["$badUrl"]}]"""
            )
            val error = runCatching { service(recorder).fetchMangaChapters("manga-id") }.exceptionOrNull()
            assertEquals("Expected rejection for $badUrl", IllegalArgumentException::class.java, error?.javaClass)
        }
    }

    private fun service(interceptor: JsonResponseInterceptor) = MangaApiService(
        baseUrl = "https://fable-api.test/api/v1/",
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
