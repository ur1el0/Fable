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
    ).map(::withProxyUrls)

    suspend fun fetchMangaStory(mangaId: String): StoryDto =
        withProxyUrls(get(url("public", "manga", mangaId)))

    suspend fun fetchMangaChapters(mangaId: String, limit: Int = 20): List<ChapterDto> =
        get<List<ChapterDto>>(url("public", "manga", mangaId, "chapters") {
            addQueryParameter("limit", limit.toString())
        }).map { chapter ->
            chapter.copy(pageUrls = chapter.pageUrls.map(::resolveProxyUrl))
        }

    private fun withProxyUrls(story: StoryDto): StoryDto = story.copy(
        coverImageUrl = story.coverImageUrl?.let(::resolveProxyUrl),
        chapters = story.chapters?.map { chapter ->
            chapter.copy(pageUrls = chapter.pageUrls.map(::resolveProxyUrl))
        }
    )

    private fun resolveProxyUrl(path: String): String {
        val normalizedPath = path.trimStart('/')
        val pathOnly = normalizedPath.substringBefore('?')
        val query = normalizedPath.substringAfter('?', missingDelimiterValue = "")
        require(
            (pathOnly.startsWith("images/covers/") || pathOnly.startsWith("images/pages/")) &&
                pathOnly.split('/').none { it.isBlank() || it == "." || it == ".." } &&
                (query.isEmpty() || pathOnly.startsWith("images/covers/") && query in setOf("size=256", "size=512"))
        ) { "Manga API returned an invalid image path." }
        val base = baseUrl ?: throw FableApiException(0, "Manga API URL is not configured.")
        val resolved = base.resolve(normalizedPath)
            ?: throw FableApiException(0, "Manga API returned an invalid image path.")
        require(
            resolved.scheme == base.scheme &&
                resolved.host == base.host &&
                resolved.encodedPath.startsWith("${base.encodedPath}images/")
        ) { "Manga API returned an invalid image path." }
        return resolved.toString()
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
