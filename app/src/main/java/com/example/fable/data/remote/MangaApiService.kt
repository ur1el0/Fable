package com.example.fable.data.remote

import com.example.fable.data.remote.dto.ChapterDto
import com.example.fable.data.remote.dto.StoryDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

private val MANGADEX_UUID_PATTERN = Regex("(?i)[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}")
private val MANGADEX_HASH_PATTERN = Regex("(?i)[0-9a-f]{32}")
private val MANGADEX_COVER_PATTERN =
    Regex("[A-Za-z0-9_-]{1,120}\\.jpg\\.(256|512)\\.jpg")
private val MANGADEX_PAGE_PATTERN = Regex(
    "[A-Za-z0-9_-]{1,180}\\.(jpg|jpeg|png|webp)",
    RegexOption.IGNORE_CASE
)
private val MANGADEX_PATH_SEGMENT_PATTERN = Regex("[A-Za-z0-9._-]+")

class MangaApiService(
    baseUrl: String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    private val baseUrl: HttpUrl? = baseUrl.takeIf(String::isNotBlank)?.toHttpUrlOrNull()

    suspend fun fetchMangaStories(
        search: String? = null,
        limit: Int = 20,
        offset: Int = 0
    ): List<StoryDto> = get<List<StoryDto>>(
        url("public", "manga") {
            search?.takeIf(String::isNotBlank)?.let { addQueryParameter("search", it) }
            addQueryParameter("limit", limit.toString())
            addQueryParameter("offset", offset.toString())
        }
    ).map(::withValidatedImageUrls)

    suspend fun fetchMangaStory(mangaId: String): StoryDto =
        withValidatedImageUrls(get(url("public", "manga", mangaId)))

    suspend fun fetchMangaChapters(mangaId: String, limit: Int = 20): List<ChapterDto> =
        get<List<ChapterDto>>(url("public", "manga", mangaId, "chapters") {
            addQueryParameter("limit", limit.toString())
        }).map { chapter ->
            chapter.copy(pageUrls = chapter.pageUrls.map(::resolveImageUrl))
        }

    private fun withValidatedImageUrls(story: StoryDto): StoryDto = story.copy(
        coverImageUrl = story.coverImageUrl?.let(::resolveImageUrl),
        chapters = story.chapters?.map { chapter ->
            chapter.copy(pageUrls = chapter.pageUrls.map(::resolveImageUrl))
        }
    )

    private fun resolveImageUrl(value: String): String {
        val imageUrl = value.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Manga API returned an invalid image URL.")
        val segments = imageUrl.encodedPath.removePrefix("/").split('/')
        val validSegments = segments.all { segment ->
            segment.isNotBlank() && segment != "." && segment != ".." &&
                segment.matches(MANGADEX_PATH_SEGMENT_PATTERN)
        }
        val trustedCover = imageUrl.host == "uploads.mangadex.org" &&
            segments.size == 3 && segments[0] == "covers" &&
            MANGADEX_UUID_PATTERN.matches(segments[1]) &&
            MANGADEX_COVER_PATTERN.matches(segments[2])
        val trustedPage = imageUrl.host.endsWith(".mangadex.network") &&
            segments.size == 3 && segments[0] in setOf("data", "data-saver") &&
            MANGADEX_HASH_PATTERN.matches(segments[1]) &&
            MANGADEX_PAGE_PATTERN.matches(segments[2])

        require(
            imageUrl.scheme == "https" && imageUrl.port == 443 &&
                imageUrl.username.isEmpty() && imageUrl.password.isEmpty() &&
                imageUrl.query == null && imageUrl.fragment == null && validSegments &&
                (trustedCover || trustedPage)
        ) { "Manga API returned an image URL outside the supported MangaDex hosts." }
        return imageUrl.toString()
    }

    private fun url(vararg segments: String, query: HttpUrl.Builder.() -> Unit = {}): HttpUrl =
        (baseUrl ?: throw FableApiException(0, "Manga API URL is not configured."))
            .newBuilder()
            .addPathSegments(segments.joinToString("/"))
            .apply(query)
            .build()

    private suspend inline fun <reified T> get(url: HttpUrl): T = withContext(Dispatchers.IO) {
        client.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
            val payload = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val detail = parseErrorDetail(payload)
                throw FableApiException(response.code, detail ?: "The Manga service could not complete the request.")
            }
            json.decodeFromString<T>(payload)
        }
    }

    private fun parseErrorDetail(payload: String): String? = runCatching {
        val detail = (json.parseToJsonElement(payload) as? JsonObject)?.get("detail") ?: return null
        when (detail) {
            is JsonPrimitive -> detail.content.takeIf(String::isNotBlank)
            else -> detail.jsonArray.joinToString { item ->
                val itemObject = item.jsonObject
                val location = itemObject["loc"]?.jsonArray
                    ?.mapNotNull { value -> value.jsonPrimitive.contentOrNull }
                    ?.filterNot { it == "query" || it == "path" || it == "body" }
                    ?.joinToString(".")
                val message = itemObject["msg"]?.jsonPrimitive?.contentOrNull
                listOfNotNull(location, message).joinToString(": ").ifBlank { "Invalid request." }
            }.takeIf(String::isNotBlank)
        }
    }.getOrNull()
}
