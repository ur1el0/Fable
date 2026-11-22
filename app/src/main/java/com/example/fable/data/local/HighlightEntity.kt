package com.example.fable.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.fable.domain.model.HighlightColor
import com.example.fable.domain.model.StoryHighlight

@Entity(
    tableName = "story_highlights",
    indices = [
        Index(value = ["ownerId", "storyId"]),
        Index(value = ["storyId"])
    ]
)
data class HighlightEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val storyId: String,
    val chapterNumber: Int,
    val paragraphIndex: Int,
    val startOffset: Int,
    val endOffset: Int,
    val selectedText: String,
    val note: String,
    val color: String,
    val createdAtUtc: String
)

fun HighlightEntity.toDomain() = StoryHighlight(
    id = id,
    ownerId = ownerId,
    storyId = storyId,
    chapterNumber = chapterNumber,
    paragraphIndex = paragraphIndex,
    startOffset = startOffset,
    endOffset = endOffset,
    selectedText = selectedText,
    note = note,
    color = HighlightColor.fromWireValue(color),
    createdAtUtc = createdAtUtc
)

fun StoryHighlight.toEntity() = HighlightEntity(
    id = id,
    ownerId = ownerId,
    storyId = storyId,
    chapterNumber = chapterNumber,
    paragraphIndex = paragraphIndex,
    startOffset = startOffset,
    endOffset = endOffset,
    selectedText = selectedText,
    note = note,
    color = color.wireValue,
    createdAtUtc = createdAtUtc
)
