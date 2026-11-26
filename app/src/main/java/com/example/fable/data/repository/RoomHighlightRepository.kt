package com.example.fable.data.repository

import com.example.fable.data.local.HighlightDao
import com.example.fable.data.local.toDomain
import com.example.fable.data.local.toEntity
import com.example.fable.domain.model.HighlightColor
import com.example.fable.domain.model.StoryHighlight
import com.example.fable.domain.repository.HighlightRepository
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomHighlightRepository(
    private val dao: HighlightDao
) : HighlightRepository {
    override fun observeHighlights(ownerId: String, storyId: String): Flow<List<StoryHighlight>> =
        dao.observeHighlights(normalizeOwner(ownerId), storyId)
            .map { list -> list.map { it.toDomain() } }

    override suspend fun saveHighlight(
        ownerId: String,
        storyId: String,
        chapterNumber: Int,
        paragraphIndex: Int,
        startOffset: Int,
        endOffset: Int,
        selectedText: String,
        note: String,
        color: HighlightColor,
        existingId: String?
    ): Result<StoryHighlight> = runCatching {
        val trimmedText = selectedText.trim()
        require(trimmedText.isNotEmpty()) { "Select at least one word to highlight." }
        val highlight = StoryHighlight(
            id = existingId?.takeIf(String::isNotBlank) ?: UUID.randomUUID().toString(),
            ownerId = normalizeOwner(ownerId),
            storyId = storyId,
            chapterNumber = chapterNumber.coerceAtLeast(1),
            paragraphIndex = paragraphIndex.coerceAtLeast(0),
            startOffset = startOffset.coerceAtLeast(0),
            endOffset = endOffset.coerceAtLeast(startOffset.coerceAtLeast(0)),
            selectedText = trimmedText,
            note = note.trim().take(500),
            color = color,
            createdAtUtc = Instant.now().toString()
        )
        dao.upsertHighlight(highlight.toEntity())
        highlight
    }

    override suspend fun deleteHighlight(ownerId: String, highlightId: String): Result<Unit> = runCatching {
        dao.deleteHighlight(normalizeOwner(ownerId), highlightId)
    }

    override suspend fun clearOwnerData(ownerId: String): Result<Unit> = runCatching {
        if (ownerId.isNotBlank() && ownerId != GUEST_OWNER_ID) {
            dao.deleteHighlightsForOwner(ownerId)
        }
    }

    override suspend fun clearStoryHighlights(storyId: String): Result<Unit> = runCatching {
        if (storyId.isNotBlank()) {
            dao.deleteHighlightsForStory(storyId)
        }
    }

    private fun normalizeOwner(ownerId: String): String =
        ownerId.takeIf(String::isNotBlank) ?: GUEST_OWNER_ID

    private companion object {
        const val GUEST_OWNER_ID = "guest"
    }
}
