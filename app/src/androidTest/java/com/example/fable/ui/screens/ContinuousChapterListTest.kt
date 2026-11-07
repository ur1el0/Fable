package com.example.fable.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.fable.domain.model.Chapter
import com.example.fable.domain.model.ReaderPreferences
import com.example.fable.domain.model.Story
import com.example.fable.ui.screens.components.ContinuousChapterList
import com.example.fable.ui.theme.FableTheme
import com.example.fable.ui.theme.readerPalette
import java.util.concurrent.ConcurrentLinkedQueue
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ContinuousChapterListTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersAdjacentChaptersAndLoadsMissingVisibleContent() {
        val story = Story(
            id = "story-1",
            title = "Test story",
            author = "Reader",
            genre = "Fantasy",
            synopsis = "A test story",
            content = "First chapter body.",
            totalChapters = 2,
            lastReadChapterNumber = 1
        )
        val chapters = listOf(
            Chapter(
                id = "chapter-1",
                storyId = story.id,
                chapterNumber = 1,
                title = "Chapter one",
                content = "First chapter body."
            ),
            Chapter(
                id = "chapter-2",
                storyId = story.id,
                chapterNumber = 2,
                title = "Chapter two"
            )
        )
        val requestedChapters = ConcurrentLinkedQueue<Int>()

        composeRule.setContent {
            FableTheme {
                ContinuousChapterList(
                    story = story,
                    chapters = chapters,
                    selectedChapterNumber = 1,
                    chapterTotal = 2,
                    readerPreferences = ReaderPreferences(),
                    palette = readerPalette(ReaderPreferences().theme),
                    onSaveProgress = { _, _ -> },
                    onEnsureChapterLoaded = { requestedChapters.add(it) },
                    onActiveChapterChanged = {}
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 3_000) { 2 in requestedChapters }
        composeRule.onNodeWithText("Chapter one").assertIsDisplayed()
        composeRule.onNodeWithText("Chapter two").assertIsDisplayed()
        assertTrue(2 in requestedChapters)
    }

    @Test
    fun loadsSelectedChapterWhenMetadataOnlyStoryHasNoCachedChaptersYet() {
        val story = Story(
            id = "gutenberg-11",
            title = "Alice's Adventures in Wonderland",
            author = "Lewis Carroll",
            genre = "Classic",
            synopsis = "",
            content = "",
            totalChapters = 0
        )
        val requestedChapters = ConcurrentLinkedQueue<Int>()

        composeRule.setContent {
            FableTheme {
                ContinuousChapterList(
                    story = story,
                    chapters = emptyList(),
                    selectedChapterNumber = 1,
                    chapterTotal = 1,
                    readerPreferences = ReaderPreferences(),
                    palette = readerPalette(ReaderPreferences().theme),
                    onSaveProgress = { _, _ -> },
                    onEnsureChapterLoaded = { requestedChapters.add(it) },
                    onActiveChapterChanged = {}
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 3_000) { 1 in requestedChapters }
        assertTrue(1 in requestedChapters)
    }
}
