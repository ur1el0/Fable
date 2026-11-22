package com.example.fable.domain.reading

import com.example.fable.domain.model.Chapter

data class InBookSearchMatch(
    val chapterNumber: Int,
    val chapterTitle: String,
    val paragraphIndex: Int,
    val snippet: String,
    val matchedQuery: String
)

data class WordToken(
    val word: String,
    val startOffset: Int,
    val endOffset: Int
)

data class PassageSelection(
    val startOffset: Int,
    val endOffset: Int,
    val selectedText: String
)

object InBookSearchEngine {
    private val PARAGRAPH_SPLIT_REGEX = Regex("\\n\\s*\\n")
    private val WORD_TOKEN_REGEX = Regex("\\S+")

    fun paragraphs(text: String): List<String> {
        val normalized = text.replace("\r\n", "\n").replace('\r', '\n')
        return normalized
            .split(PARAGRAPH_SPLIT_REGEX)
            .map(String::trim)
            .filter(String::isNotEmpty)
    }

    fun tokenizeWords(paragraph: String): List<WordToken> =
        WORD_TOKEN_REGEX.findAll(paragraph)
            .map { match ->
                WordToken(
                    word = match.value,
                    startOffset = match.range.first,
                    endOffset = match.range.last + 1
                )
            }
            .toList()

    fun selectWordRange(
        paragraph: String,
        startWordIndex: Int,
        endWordIndex: Int
    ): PassageSelection {
        val tokens = tokenizeWords(paragraph)
        if (tokens.isEmpty()) {
            return PassageSelection(0, paragraph.length, paragraph.trim())
        }
        val safeStart = startWordIndex.coerceIn(0, tokens.lastIndex)
        val safeEnd = endWordIndex.coerceIn(safeStart, tokens.lastIndex)
        val charStart = tokens[safeStart].startOffset
        val charEnd = tokens[safeEnd].endOffset
        return PassageSelection(
            startOffset = charStart,
            endOffset = charEnd,
            selectedText = paragraph.substring(charStart, charEnd)
        )
    }

    fun generateSnippet(
        query: String,
        paragraph: String,
        contextRadius: Int = 44
    ): String {
        val trimmedQuery = query.trim()
        val matchIndex = paragraph.indexOf(trimmedQuery, ignoreCase = true)
        if (matchIndex < 0) {
            return paragraph.take(contextRadius * 2).replace('\n', ' ')
        }
        val start = (matchIndex - contextRadius).coerceAtLeast(0)
        val end = (matchIndex + trimmedQuery.length + contextRadius).coerceAtMost(paragraph.length)
        val rawSnippet = paragraph.substring(start, end).replace('\n', ' ').trim()
        val prefix = if (start > 0) "..." else ""
        val suffix = if (end < paragraph.length) "..." else ""
        return "$prefix$rawSnippet$suffix"
    }

    fun search(
        query: String,
        chapters: List<Chapter>,
        fallbackContent: String = "",
        fallbackChapterTitle: String = "Chapter 1"
    ): List<InBookSearchMatch> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        val matches = mutableListOf<InBookSearchMatch>()
        val orderedChapters = chapters.sortedBy(Chapter::chapterNumber)
        if (orderedChapters.isNotEmpty()) {
            for (chapter in orderedChapters) {
                val body = chapter.content.takeIf(String::isNotBlank)
                    ?: fallbackContent.takeIf { chapter.chapterNumber == 1 }.orEmpty()
                val chapterTitle = chapter.title.ifBlank { "Chapter ${chapter.chapterNumber}" }
                paragraphs(body).forEachIndexed { paragraphIndex, paragraph ->
                    if (paragraph.contains(trimmed, ignoreCase = true)) {
                        matches += InBookSearchMatch(
                            chapterNumber = chapter.chapterNumber,
                            chapterTitle = chapterTitle,
                            paragraphIndex = paragraphIndex,
                            snippet = generateSnippet(trimmed, paragraph),
                            matchedQuery = trimmed
                        )
                    }
                }
            }
        } else if (fallbackContent.isNotBlank()) {
            val title = fallbackChapterTitle.ifBlank { "Chapter 1" }
            paragraphs(fallbackContent).forEachIndexed { paragraphIndex, paragraph ->
                if (paragraph.contains(trimmed, ignoreCase = true)) {
                    matches += InBookSearchMatch(
                        chapterNumber = 1,
                        chapterTitle = title,
                        paragraphIndex = paragraphIndex,
                        snippet = generateSnippet(trimmed, paragraph),
                        matchedQuery = trimmed
                    )
                }
            }
        }
        return matches
    }
}
