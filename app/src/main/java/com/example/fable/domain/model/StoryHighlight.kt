package com.example.fable.domain.model

enum class HighlightColor(val wireValue: String, val displayName: String) {
    TERRACOTTA("TERRACOTTA", "Terracotta"),
    AMBER("AMBER", "Amber"),
    SAGE("SAGE", "Sage");

    companion object {
        fun fromWireValue(value: String): HighlightColor =
            entries.firstOrNull { it.wireValue.equals(value, ignoreCase = true) } ?: TERRACOTTA
    }
}

data class StoryHighlight(
    val id: String,
    val ownerId: String,
    val storyId: String,
    val chapterNumber: Int,
    val paragraphIndex: Int,
    val startOffset: Int,
    val endOffset: Int,
    val selectedText: String,
    val note: String = "",
    val color: HighlightColor = HighlightColor.TERRACOTTA,
    val createdAtUtc: String = ""
)
