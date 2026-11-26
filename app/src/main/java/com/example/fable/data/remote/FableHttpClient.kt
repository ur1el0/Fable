package com.example.fable.data.remote

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resumeWithException

object FableHttpClientFactory {
    fun create(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .build()
}

internal suspend fun Call.awaitResponse(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(
        object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response) { _, res, _ -> res.close() }
            }
        }
    )
}

internal fun parseApiErrorDetail(json: Json, payload: String): String? = runCatching {
    val element = json.parseToJsonElement(payload)
    val detail = (element as? JsonObject)?.get("detail") ?: return null
    when (detail) {
        is JsonPrimitive -> detail.content.takeIf(String::isNotBlank)
        is JsonArray -> detail
            .mapNotNull { item ->
                (item as? JsonObject)?.get("msg")?.let { (it as? JsonPrimitive)?.content }
                    ?: (item as? JsonPrimitive)?.content
            }
            .filter(String::isNotBlank)
            .joinToString("; ")
            .takeIf(String::isNotBlank)
        else -> null
    }
}.getOrNull()
