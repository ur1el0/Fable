package com.example.fable.domain.repository

import com.example.fable.domain.model.HighlightColor
import com.example.fable.domain.model.StoryHighlight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface HighlightRepository {
    fun observeHighlights(ownerId: String, storyId: String): Flow<List<StoryHighlight>>

    suspend fun saveHighlight(
        ownerId: String,
        storyId: String,
        chapterNumber: Int,
        paragraphIndex: Int,
        startOffset: Int,
        endOffset: Int,
        selectedText: String,
        note: String,
        color: HighlightColor,
        existingId: String? = null
    ): Result<StoryHighlight>

    suspend fun deleteHighlight(ownerId: String, highlightId: String): Result<Unit>

    suspend fun clearOwnerData(ownerId: String): Result<Unit>

    suspend fun clearStoryHighlights(storyId: String): Result<Unit>
}

object EmptyHighlightRepository : HighlightRepository {
    override fun observeHighlights(ownerId: String, storyId: String): Flow<List<StoryHighlight>> =
        flowOf(emptyList())

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
    ): Result<StoryHighlight> = Result.failure(UnsupportedOperationException())

    override suspend fun deleteHighlight(ownerId: String, highlightId: String): Result<Unit> =
        Result.success(Unit)

    override suspend fun clearOwnerData(ownerId: String): Result<Unit> =
        Result.success(Unit)

    override suspend fun clearStoryHighlights(storyId: String): Result<Unit> =
        Result.success(Unit)
}
