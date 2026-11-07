package com.example.fable.data.remote.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingStatsDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun sessionRequestUsesBackendCamelCaseFields() {
        val request = ReadingSessionRequestDto(
            id = "session-id",
            storyId = "story-id",
            secondsRead = 42,
            readAtUtc = "2026-11-06T12:00:00Z",
            isCompleted = true
        )

        val fields = json.parseToJsonElement(json.encodeToString(request)).jsonObject

        assertEquals("session-id", fields.getValue("id").jsonPrimitive.content)
        assertEquals("story-id", fields.getValue("storyId").jsonPrimitive.content)
        assertEquals("42", fields.getValue("secondsRead").jsonPrimitive.content)
        assertEquals("2026-11-06T12:00:00Z", fields.getValue("readAtUtc").jsonPrimitive.content)
        assertEquals("true", fields.getValue("isCompleted").jsonPrimitive.content)
    }

    @Test
    fun statsResponseDecodesBackendCamelCaseFields() {
        val stats = json.decodeFromString<ReadingStatsDto>(
            """{"storiesReadCount":3,"totalMinutesRead":81,"streakDays":4}"""
        )

        assertEquals(3, stats.storiesReadCount)
        assertEquals(81, stats.totalMinutesRead)
        assertEquals(4, stats.streakDays)
    }
}
