package com.example.fable.domain.reading

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderPagePackerTest {
    @Test
    fun emptyInputProducesNoPages() {
        assertEquals(emptyList<String>(), ReaderPagePacker.chunkIntoPages(" \n\n ", 100))
    }

    @Test
    fun paragraphsAreGroupedWithoutSplittingThem() {
        val text = listOf(words(20), words(20), words(20)).joinToString("\n\n")

        assertEquals(
            listOf("${words(20)}\n\n${words(20)}", words(20)),
            ReaderPagePacker.chunkIntoPages(text, fontScalePercent = 100, baseWordsPerPage = 50)
        )
    }

    @Test
    fun largerFontScaleProducesMorePages() {
        val text = listOf(words(65), words(65), words(65)).joinToString("\n\n")

        val compactPages = ReaderPagePacker.chunkIntoPages(text, fontScalePercent = 80)
        val largePages = ReaderPagePacker.chunkIntoPages(text, fontScalePercent = 150)

        assertEquals(1, compactPages.size)
        assertEquals(3, largePages.size)
    }

    @Test
    fun normalizesWindowsLineEndingsAndKeepsOversizedParagraphIntact() {
        val oversizedParagraph = words(60)
        val text = "${words(20)}\r\n\r\n$oversizedParagraph"

        assertEquals(
            listOf(words(20), oversizedParagraph),
            ReaderPagePacker.chunkIntoPages(text, fontScalePercent = 100, baseWordsPerPage = 50)
        )
    }

    private fun words(count: Int) = (1..count).joinToString(" ") { "word$it" }
}
