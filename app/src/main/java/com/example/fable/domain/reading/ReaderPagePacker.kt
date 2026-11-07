package com.example.fable.domain.reading

import com.example.fable.domain.model.ReaderPreferences
import kotlin.math.roundToInt

object ReaderPagePacker {
    fun chunkIntoPages(
        text: String,
        fontScalePercent: Int,
        baseWordsPerPage: Int = DEFAULT_WORDS_PER_PAGE
    ): List<String> {
        require(baseWordsPerPage > 0)
        require(fontScalePercent in ReaderPreferences.MIN_FONT_SCALE_PERCENT..ReaderPreferences.MAX_FONT_SCALE_PERCENT)

        val normalized = text.replace("\r\n", "\n").replace('\r', '\n')
        val paragraphs = normalized
            .split(Regex("\\n\\s*\\n"))
            .map(String::trim)
            .filter(String::isNotEmpty)
        if (paragraphs.isEmpty()) return emptyList()

        val scale = fontScalePercent / 100.0
        val targetWords = (baseWordsPerPage / scale).roundToInt().coerceAtLeast(MIN_WORDS_PER_PAGE)
        val pages = mutableListOf<String>()
        val currentParagraphs = mutableListOf<String>()
        var currentWordCount = 0

        paragraphs.forEach { paragraph ->
            val paragraphWordCount = paragraph.split(WORD_SEPARATOR).size
            if (currentWordCount + paragraphWordCount > targetWords && currentParagraphs.isNotEmpty()) {
                pages += currentParagraphs.joinToString("\n\n")
                currentParagraphs.clear()
                currentWordCount = 0
            }
            currentParagraphs += paragraph
            currentWordCount += paragraphWordCount
        }

        if (currentParagraphs.isNotEmpty()) pages += currentParagraphs.joinToString("\n\n")
        return pages
    }

    private const val DEFAULT_WORDS_PER_PAGE = 180
    private const val MIN_WORDS_PER_PAGE = 50
    private val WORD_SEPARATOR = Regex("\\s+")
}
