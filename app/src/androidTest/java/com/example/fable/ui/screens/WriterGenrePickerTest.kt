package com.example.fable.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.fable.domain.model.GenreCategory
import com.example.fable.domain.model.WriterDraft
import com.example.fable.ui.theme.FableTheme
import com.example.fable.ui.viewmodel.WriterDraftUiState
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WriterGenrePickerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectingAvailableGenreUpdatesDraft() {
        val updatedDraft = AtomicReference<WriterDraft>()

        composeRule.setContent {
            FableTheme {
                WriteScreen(
                    expectedOwnerId = "guest",
                    draftState = WriterDraftUiState(isLoading = false),
                    genres = listOf(
                        GenreCategory(id = "fantasy", name = "Fantasy"),
                        GenreCategory(id = "duplicate", name = " fantasy "),
                        GenreCategory(id = "blank", name = " ")
                    ),
                    onDraftChanged = updatedDraft::set
                )
            }
        }

        composeRule.onNodeWithContentDescription("Choose genre").performClick()
        composeRule.onNodeWithText("Fantasy").assertIsDisplayed().performClick()

        assertEquals("Fantasy", updatedDraft.get().genre)
    }

    @Test
    fun genreRemainsEditableWithoutRemoteSuggestions() {
        val updatedDraft = AtomicReference<WriterDraft>()

        composeRule.setContent {
            FableTheme {
                WriteScreen(
                    expectedOwnerId = "guest",
                    draftState = WriterDraftUiState(isLoading = false),
                    onDraftChanged = updatedDraft::set
                )
            }
        }

        composeRule.onNodeWithText("Genre").performTextInput("Romance")

        assertEquals("Romance", updatedDraft.get().genre)
        composeRule.onNodeWithContentDescription("Choose genre").assertDoesNotExist()
    }
}
