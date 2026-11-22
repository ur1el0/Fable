package com.example.fable.data.remote

import com.example.fable.data.remote.dto.ChapterDto
import com.example.fable.data.remote.dto.StoryDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

private val MANGADEX_UUID_PATTERN = Regex("(?i)[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}")
private val MANGADEX_HASH_PATTERN = Regex("(?i)[0-9a-f]{32}")
private val MANGADEX_COVER_PATTERN = Regex(
    "[A-Za-z0-9._-]{1,160}\\.(jpg|jpeg|png|webp|gif)(\\.(256|512)\\.jpg)?",
    RegexOption.IGNORE_CASE
)
private val MANGADEX_PAGE_PATTERN = Regex(
    "[A-Za-z0-9._-]{1,200}\\.(jpg|jpeg|png|webp|gif)",
    RegexOption.IGNORE_CASE
)
private val MANGADEX_PATH_SEGMENT_PATTERN = Regex("[A-Za-z0-9._-]+")

class MangaApiService(
    baseUrl: String,
    private val client: OkHttpClient = FableHttpClientFactory.create(),
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
            chapter.copy(pageUrls = chapter.pageUrls.map(::resolvePageUrl))
        }

    private fun withValidatedImageUrls(story: StoryDto): StoryDto = story.copy(
        coverImageUrl = sanitizeCoverUrl(story.coverImageUrl),
        chapters = story.chapters?.map { chapter ->
            chapter.copy(pageUrls = chapter.pageUrls.map(::resolvePageUrl))
        }
    )

    private fun sanitizeCoverUrl(value: String?): String? {
        if (value.isNullOrBlank()) return null
        val imageUrl = value.toHttpUrlOrNull() ?: return null
        val segments = validatedPathSegments(imageUrl) ?: return null
        val trustedCover = imageUrl.host == "uploads.mangadex.org" &&
            imageUrl.port == 443 &&
            segments.size == 3 &&
            segments[0] == "covers" &&
            MANGADEX_UUID_PATTERN.matches(segments[1]) &&
            MANGADEX_COVER_PATTERN.matches(segments[2])
        if (!hasTrustedHttpsTransport(imageUrl) || !trustedCover) return null
        return imageUrl.toString()
    }

    private fun resolvePageUrl(value: String): String {
        val imageUrl = value.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Manga API returned an invalid image URL.")
        val segments = validatedPathSegments(imageUrl)
            ?: throw IllegalArgumentException("Manga API returned an image URL with an unsupported path.")
        val trustedHost = (imageUrl.host == "uploads.mangadex.org" && imageUrl.port == 443) ||
            (
                imageUrl.host.endsWith(".mangadex.network") &&
                    imageUrl.host.length > ".mangadex.network".length &&
                    imageUrl.port in 1..65535
                )
        val trustedPage = trustedHost &&
            segments.size == 3 &&
            segments[0] in setOf("data", "data-saver") &&
            MANGADEX_HASH_PATTERN.matches(segments[1]) &&
            MANGADEX_PAGE_PATTERN.matches(segments[2])

        require(hasTrustedHttpsTransport(imageUrl) && trustedPage) {
            "Manga API returned an image URL outside the supported MangaDex hosts."
        }
        return imageUrl.toString()
    }

    private fun hasTrustedHttpsTransport(imageUrl: HttpUrl): Boolean =
        imageUrl.scheme == "https" &&
            imageUrl.username.isEmpty() &&
            imageUrl.password.isEmpty() &&
            imageUrl.query == null &&
            imageUrl.fragment == null

    private fun validatedPathSegments(imageUrl: HttpUrl): List<String>? {
        val segments = imageUrl.encodedPath.removePrefix("/").split('/')
        val valid = segments.all { segment ->
            segment.isNotBlank() &&
                segment != "." &&
                segment != ".." &&
                !segment.contains("..") &&
                segment.matches(MANGADEX_PATH_SEGMENT_PATTERN)
        }
        return if (valid) segments else null
    }

    private fun url(vararg segments: String, query: HttpUrl.Builder.() -> Unit = {}): HttpUrl =
        (baseUrl ?: throw FableApiException(0, "Manga API URL is not configured."))
            .newBuilder()
            .addPathSegments(segments.joinToString("/"))
            .apply(query)
            .build()

    private suspend inline fun <reified T> get(url: HttpUrl): T = withContext(Dispatchers.IO) {
        client.newCall(Request.Builder().url(url).get().build()).awaitResponse().use { response ->
            val payload = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val detail = parseApiErrorDetail(json, payload)
                throw FableApiException(response.code, detail ?: "The Manga service could not complete the request.")
            }
            json.decodeFromString<T>(payload)
        }
    }
}
