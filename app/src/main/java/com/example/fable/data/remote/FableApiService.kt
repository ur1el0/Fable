package com.example.fable.data.remote

import com.example.fable.data.remote.dto.ApiErrorDto
import com.example.fable.data.remote.dto.AuthResponseDto
import com.example.fable.data.remote.dto.ChapterDto
import com.example.fable.data.remote.dto.GenreDto
import com.example.fable.data.remote.dto.LoginRequestDto
import com.example.fable.data.remote.dto.RegisterRequestDto
import com.example.fable.data.remote.dto.StoryDto
import com.example.fable.data.remote.dto.UpdateFeedDto
import com.example.fable.data.remote.dto.WriterDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class FableApiService(
    baseUrl: String,
    private val client: OkHttpClient = OkHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    private val baseUrl: HttpUrl? = baseUrl.takeIf(String::isNotBlank)?.toHttpUrlOrNull()

    suspend fun fetchStories(
        genre: String? = null,
        search: String? = null,
        since: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): List<StoryDto> = get(
        url("stories") {
            addQueryParameter("genre", genre)
            addQueryParameter("search", search)
            addQueryParameter("since", since)
            addQueryParameter("limit", limit.toString())
            addQueryParameter("offset", offset.toString())
        }
    )

    suspend fun fetchStory(storyId: String): StoryDto = get(url("stories", storyId))

    suspend fun fetchChapters(storyId: String): List<ChapterDto> =
        get(url("stories", storyId, "chapters"))

    suspend fun fetchChapter(storyId: String, chapterNumber: Int): ChapterDto =
        get(url("stories", storyId, "chapters", chapterNumber.toString()))

    suspend fun fetchGutenbergStories(topic: String? = null, search: String? = null): List<StoryDto> =
        get(url("public", "gutenberg") {
            addQueryParameter("topic", topic)
            addQueryParameter("search", search)
        })

    suspend fun fetchGutenbergStory(gutenbergId: Int): StoryDto =
        get(url("public", "gutenberg", gutenbergId.toString()))

    suspend fun fetchGutenbergChapters(gutenbergId: Int): List<ChapterDto> =
        get(url("public", "gutenberg", gutenbergId.toString(), "chapters"))

    suspend fun fetchMangaStories(
        search: String? = null,
        limit: Int = 20,
        offset: Int = 0
    ): List<StoryDto> = get(
        url("public", "manga") {
            addQueryParameter("search", search)
            addQueryParameter("limit", limit.toString())
            addQueryParameter("offset", offset.toString())
        }
    )

    suspend fun fetchMangaStory(mangaId: String): StoryDto =
        get(url("public", "manga", mangaId))

    suspend fun fetchMangaChapters(mangaId: String, limit: Int = 20): List<ChapterDto> =
        get(url("public", "manga", mangaId, "chapters") {
            addQueryParameter("limit", limit.toString())
        })

    suspend fun fetchGenres(): List<GenreDto> = get(url("genres"))

    suspend fun fetchTopAuthors(): List<WriterDto> = get(url("authors", "top"))

    suspend fun fetchUpdateFeed(): UpdateFeedDto = get(url("updates"))

    suspend fun register(request: RegisterRequestDto): AuthResponseDto =
        postJson(url("auth", "register"), request)

    suspend fun login(request: LoginRequestDto): AuthResponseDto =
        postJson(url("auth", "login"), request)

    private fun url(vararg segments: String, query: HttpUrl.Builder.() -> Unit = {}): HttpUrl =
        (baseUrl ?: throw FableApiException(0, "Fable API URL is not configured."))
            .newBuilder()
            .addPathSegments(segments.joinToString("/"))
            .apply(query)
            .build()

    private suspend inline fun <reified T> get(url: HttpUrl): T = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            val payload = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val detail = runCatching { json.decodeFromString<ApiErrorDto>(payload).detail }
                    .getOrNull()
                    ?.takeIf(String::isNotBlank)
                throw FableApiException(response.code, detail ?: "The server could not complete the request.")
            }
            json.decodeFromString<T>(payload)
        }
    }

    private suspend inline fun <reified T, reified B> postJson(url: HttpUrl, body: B): T =
        withContext(Dispatchers.IO) {
            val requestBody = json.encodeToString(body)
                .toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(url).post(requestBody).build()
            client.newCall(request).execute().use { response ->
                val payload = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val detail = runCatching { json.decodeFromString<ApiErrorDto>(payload).detail }
                        .getOrNull()
                        ?.takeIf(String::isNotBlank)
                    throw FableApiException(response.code, detail ?: "The server could not complete the request.")
                }
                json.decodeFromString<T>(payload)
            }
        }
}

class FableApiException(val statusCode: Int, message: String) : Exception(message)
