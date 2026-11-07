package com.example.fable.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.click
import com.example.fable.domain.model.Chapter
import com.example.fable.domain.model.ReaderLayout
import com.example.fable.domain.model.ReaderPreferences
import com.example.fable.domain.model.Story
import com.example.fable.ui.theme.FableTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReaderControlsOverlayTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tapTogglesChromeWithoutBlockingHorizontalPageSwipes() {
        val firstParagraph = (1..110).joinToString(" ") { "word$it" }
        val secondParagraph = (111..220).joinToString(" ") { "word$it" }
        val chapterContent = "$firstParagraph\n\n$secondParagraph"
        val story = Story(
            id = "story-1",
            title = "Test story",
            author = "Reader",
            genre = "Fantasy",
            synopsis = "A test story",
            content = chapterContent,
            totalChapters = 1
        )
        val chapter = Chapter(
            id = "chapter-1",
            storyId = story.id,
            chapterNumber = 1,
            title = "Chapter one",
            content = chapterContent
        )

        composeRule.setContent {
            FableTheme {
                StoryReaderScreen(
                    story = story,
                    chapters = listOf(chapter),
                    chapterNumber = 1,
                    errorMessage = null,
                    readerPreferences = ReaderPreferences(layout = ReaderLayout.PAGINATED),
                    onBack = {},
                    onSelectChapter = {},
                    onSaveProgress = { _, _ -> },
                    onReadingSessionEnded = { _, _, _ -> },
                    onReaderPreferencesChanged = {},
                    onResetReaderPreferences = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("Display options").assertIsDisplayed()
        composeRule.onNodeWithText("Page 1 of 2").performTouchInput { swipeLeft() }
        composeRule.onNodeWithText("Page 2 of 2").assertIsDisplayed()
        composeRule.onNodeWithText("Page 2 of 2").performTouchInput { click() }
        assertTrue(
            composeRule.onAllNodesWithContentDescription("Display options")
                .fetchSemanticsNodes()
                .isEmpty()
        )
        composeRule.onNodeWithText("Page 2 of 2").performTouchInput { click() }
        composeRule.onNodeWithContentDescription("Display options").assertIsDisplayed()
    }
}
