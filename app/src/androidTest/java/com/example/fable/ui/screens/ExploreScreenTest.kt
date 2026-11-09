package com.example.fable.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.fable.domain.model.SourceProvider
import com.example.fable.domain.model.Story
import com.example.fable.domain.model.StoryCatalog
import com.example.fable.ui.theme.FableTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ExploreScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun cachedProviderResultsShowRefreshErrorAndRetry() {
        val searchRequests = mutableListOf<Pair<StoryCatalog, String>>()
        val cachedStory = Story(
            id = "gutenberg-11",
            title = "Alice's Adventures",
            author = "Lewis Carroll",
            genre = "Fantasy",
            synopsis = "A cached story",
            content = "",
            sourceProvider = SourceProvider.GUTENBERG
        )

        composeRule.setContent {
            FableTheme {
                ExploreScreen(
                    providerStories = listOf(cachedStory),
                    providerErrorMessage = "Provider unavailable",
                    onSearchProvider = { catalog, query -> searchRequests += catalog to query }
                )
            }
        }

        composeRule.onNodeWithText("Gutenberg").performClick()
        composeRule.onNodeWithText("Alice's Adventures").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Catalog refresh failed. Showing saved results. Provider unavailable"
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()

        assertEquals(
            listOf(
                StoryCatalog.GUTENBERG to "",
                StoryCatalog.GUTENBERG to ""
            ),
            searchRequests
        )
    }
}
