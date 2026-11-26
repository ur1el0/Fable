package com.example.fable.domain.reading

import com.example.fable.domain.model.Story
import com.example.fable.domain.model.StoryHighlight

object HighlightExporter {
    fun exportAsMarkdown(
        story: Story,
        highlights: List<StoryHighlight>
    ): String {
        val sorted = highlights.sortedWith(
            compareBy<StoryHighlight> { it.chapterNumber }
                .thenBy { it.paragraphIndex }
                .thenBy { it.startOffset }
        )
        val builder = StringBuilder()
        builder.appendLine("# Highlights & Marginalia: ${story.title}")
        builder.appendLine("**Author:** ${story.author}")
        builder.appendLine("**Total Highlights:** ${sorted.size}")
        builder.appendLine()
        builder.appendLine("---")

        var currentChapter: Int? = null
        for (highlight in sorted) {
            if (highlight.chapterNumber != currentChapter) {
                currentChapter = highlight.chapterNumber
                builder.appendLine()
                builder.appendLine("## Chapter ${highlight.chapterNumber}")
                builder.appendLine()
            }
            builder.appendLine("> “${highlight.selectedText.trim()}”")
            builder.appendLine(">")
            builder.appendLine("> — *${highlight.color.displayName} highlight*")
            val trimmedNote = highlight.note.trim()
            if (trimmedNote.isNotEmpty()) {
                builder.appendLine()
                builder.appendLine("*Note: $trimmedNote*")
            }
            builder.appendLine()
            builder.appendLine("---")
        }
        return builder.toString().trimEnd()
    }

    fun formatQuoteShareText(
        quoteText: String,
        storyTitle: String,
        storyAuthor: String,
        chapterNumber: Int?,
        note: String?
    ): String = buildString {
        append("“")
        append(quoteText.trim())
        appendLine("”")
        val trimmedNote = note?.trim().orEmpty()
        if (trimmedNote.isNotEmpty()) {
            appendLine()
            append("Note: ")
            appendLine(trimmedNote)
        }
        appendLine()
        append("— ")
        append(storyTitle)
        if (storyAuthor.isNotBlank()) {
            append(" by ")
            append(storyAuthor)
        }
        if (chapterNumber != null) {
            append(" (Chapter ")
            append(chapterNumber)
            append(")")
        }
    }
}
