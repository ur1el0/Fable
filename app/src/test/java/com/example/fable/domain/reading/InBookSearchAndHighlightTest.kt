package com.example.fable.domain.reading

import com.example.fable.domain.model.Chapter
import com.example.fable.domain.model.HighlightColor
import com.example.fable.domain.model.Story
import com.example.fable.domain.model.StoryHighlight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InBookSearchAndHighlightTest {
    @Test
    fun selectWordRangeExtractsExactCharacterOffsetsAndSubstring() {
        val paragraph = "The old forest whispered tales of forgotten kings."
        val selection = InBookSearchEngine.selectWordRange(
            paragraph = paragraph,
            startWordIndex = 1,
            endWordIndex = 4
        )

        assertEquals("old forest whispered tales", selection.selectedText)
        assertEquals(4, selection.startOffset)
        assertEquals(30, selection.endOffset)
        assertEquals(
            selection.selectedText,
            paragraph.substring(selection.startOffset, selection.endOffset)
        )
    }

    @Test
    fun searchFindsCaseInsensitiveMatchesAcrossChaptersWithContextSnippets() {
        val chapters = listOf(
            Chapter(
                id = "c1",
                storyId = "s1",
                chapterNumber = 1,
                title = "The Harbor",
                content = "Fog rolled over the quiet harbor at dawn.\n\nA lantern flickered on the pier."
            ),
            Chapter(
                id = "c2",
                storyId = "s1",
                chapterNumber = 2,
                title = "The Voyage",
                content = "By midnight the lantern was their only guide across the dark water."
            )
        )

        val matches = InBookSearchEngine.search(query = "LANTERN", chapters = chapters)

        assertEquals(2, matches.size)
        assertEquals(1, matches[0].chapterNumber)
        assertEquals("The Harbor", matches[0].chapterTitle)
        assertEquals(1, matches[0].paragraphIndex)
        assertTrue(matches[0].snippet.contains("lantern", ignoreCase = true))

        assertEquals(2, matches[1].chapterNumber)
        assertEquals(0, matches[1].paragraphIndex)
    }

    @Test
    fun exportAsMarkdownOrdersHighlightsByChapterAndIncludesNotes() {
        val story = Story(
            id = "s1",
            title = "Winter Sea",
            author = "Clara Vance",
            genre = "Drama",
            synopsis = "Synopsis",
            content = ""
        )
        val highlights = listOf(
            StoryHighlight(
                id = "h2",
                ownerId = "u1",
                storyId = "s1",
                chapterNumber = 2,
                paragraphIndex = 0,
                startOffset = 0,
                endOffset = 18,
                selectedText = "Stars above the mast",
                note = "Turning point",
                color = HighlightColor.SAGE
            ),
            StoryHighlight(
                id = "h1",
                ownerId = "u1",
                storyId = "s1",
                chapterNumber = 1,
                paragraphIndex = 2,
                startOffset = 4,
                endOffset = 22,
                selectedText = "Quiet harbor at dawn",
                note = "",
                color = HighlightColor.TERRACOTTA
            )
        )

        val markdown = HighlightExporter.exportAsMarkdown(story, highlights)

        assertTrue(markdown.contains("# Highlights & Marginalia: Winter Sea"))
        assertTrue(markdown.contains("**Author:** Clara Vance"))
        assertTrue(markdown.contains("**Total Highlights:** 2"))
        val ch1Index = markdown.indexOf("## Chapter 1")
        val ch2Index = markdown.indexOf("## Chapter 2")
        assertTrue(ch1Index in 0 until ch2Index)
        assertTrue(markdown.contains("> “Quiet harbor at dawn”"))
        assertTrue(markdown.contains("*Note: Turning point*"))
    }
}
