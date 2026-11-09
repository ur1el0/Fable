package com.example.fable.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.fable.domain.model.Story
import com.example.fable.ui.theme.FableTheme
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PublishConfirmationSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun displaysPublishedStoryAndOpensItOnRequest() {
        val viewRequested = AtomicBoolean(false)

        composeRule.setContent {
            FableTheme {
                PublishConfirmationSheet(
                    story = publishedStory(),
                    onDismiss = {},
                    onViewStory = { viewRequested.set(true) },
                    onReturnToLibrary = {},
                    onShare = {}
                )
            }
        }

        composeRule.onNodeWithText("Story published").assertIsDisplayed()
        composeRule.onNodeWithText("The title is now available in the Community Library.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Public  ·  Fantasy  ·  3 words").assertIsDisplayed()
        composeRule.onNodeWithText("View story now").performClick()

        composeRule.waitUntil(timeoutMillis = 3_000) { viewRequested.get() }
    }

    @Test
    fun returnsToLibraryOnRequest() {
        val libraryRequested = AtomicBoolean(false)

        composeRule.setContent {
            FableTheme {
                PublishConfirmationSheet(
                    story = publishedStory(),
                    onDismiss = {},
                    onViewStory = {},
                    onReturnToLibrary = { libraryRequested.set(true) },
                    onShare = {}
                )
            }
        }

        composeRule.onNodeWithText("Return to library").performClick()

        composeRule.waitUntil(timeoutMillis = 3_000) { libraryRequested.get() }
    }

    @Test
    fun sharesStoryDetailsOnRequest() {
        val shareRequested = AtomicBoolean(false)

        composeRule.setContent {
            FableTheme {
                PublishConfirmationSheet(
                    story = publishedStory(),
                    onDismiss = {},
                    onViewStory = {},
                    onReturnToLibrary = {},
                    onShare = { shareRequested.set(true) }
                )
            }
        }

        composeRule.onNodeWithText("Share story details").performClick()

        assertTrue(shareRequested.get())
    }

    private fun publishedStory() = Story(
        id = "story-1",
        title = "The title",
        author = "Writer",
        genre = "Fantasy",
        synopsis = "A short synopsis.",
        content = "One two\nthree"
    )
}
