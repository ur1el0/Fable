package com.example.fable.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.example.fable.domain.model.Chapter
import com.example.fable.domain.model.ContentFormat
import com.example.fable.domain.model.MangaReadingMode
import com.example.fable.domain.model.ReaderTheme
import com.example.fable.domain.model.SourceProvider
import com.example.fable.domain.model.Story
import com.example.fable.ui.theme.FableTheme
import com.example.fable.ui.theme.readerPalette
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MangaChapterReaderTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun restoresSavedPageAfterChapterPagesArriveAsynchronously() {
        val story = mangaStory(progressPercent = 75)
        var chapter by mutableStateOf<Chapter?>(
            Chapter(
                id = "chap-1",
                storyId = story.id,
                chapterNumber = 1,
                title = "Chapter 1",
                pageUrls = emptyList()
            )
        )

        composeRule.setContent {
            FableTheme {
                MangaChapterReader(
                    story = story,
                    chapter = chapter,
                    chapterNumber = 1,
                    chapterTotal = 1,
                    mode = MangaReadingMode.PAGE_LTR,
                    palette = readerPalette(ReaderTheme.CHARCOAL),
                    onSaveProgress = { _, _ -> },
                    onToggleChrome = {}
                )
            }
        }

        composeRule.onNodeWithText("Chapter pages are loading or unavailable.").assertIsDisplayed()

        composeRule.runOnIdle {
            chapter = chapter?.copy(
                pageUrls = listOf(
                    "https://cmdxd98sb0x3yprd.mangadex.network/data-saver/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa/1.jpg",
                    "https://cmdxd98sb0x3yprd.mangadex.network/data-saver/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa/2.jpg",
                    "https://cmdxd98sb0x3yprd.mangadex.network/data-saver/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa/3.jpg",
                    "https://cmdxd98sb0x3yprd.mangadex.network/data-saver/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa/4.jpg"
                )
            )
        }

        composeRule.onNodeWithText("Page 3 of 4").assertIsDisplayed()
    }

    @Test
    fun rtlTapZonesAdvanceOnLeftAndRewindOnRight() {
        val story = mangaStory(progressPercent = 0)
        val chapter = Chapter(
            id = "chap-1",
            storyId = story.id,
            chapterNumber = 1,
            title = "Chapter 1",
            pageUrls = listOf(
                "https://cmdxd98sb0x3yprd.mangadex.network/data-saver/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa/1.jpg",
                "https://cmdxd98sb0x3yprd.mangadex.network/data-saver/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa/2.jpg",
                "https://cmdxd98sb0x3yprd.mangadex.network/data-saver/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa/3.jpg"
            )
        )

        composeRule.setContent {
            FableTheme {
                MangaChapterReader(
                    story = story,
                    chapter = chapter,
                    chapterNumber = 1,
                    chapterTotal = 1,
                    mode = MangaReadingMode.PAGE_RTL,
                    palette = readerPalette(ReaderTheme.CHARCOAL),
                    onSaveProgress = { _, _ -> },
                    onToggleChrome = {}
                )
            }
        }

        composeRule.onNodeWithText("Page 1 of 3").assertIsDisplayed()
        composeRule.onRoot().performTouchInput { click(Offset(width * 0.1f, height * 0.5f)) }
        composeRule.onNodeWithText("Page 2 of 3").assertIsDisplayed()
        composeRule.onRoot().performTouchInput { click(Offset(width * 0.9f, height * 0.5f)) }
        composeRule.onNodeWithText("Page 1 of 3").assertIsDisplayed()
    }

    @Test
    fun ltrTapZonesAdvanceOnRightAndRewindOnLeft() {
        val story = mangaStory(progressPercent = 0)
        val chapter = Chapter(
            id = "chap-1",
            storyId = story.id,
            chapterNumber = 1,
            title = "Chapter 1",
            pageUrls = listOf(
                "https://cmdxd98sb0x3yprd.mangadex.network/data-saver/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa/1.jpg",
                "https://cmdxd98sb0x3yprd.mangadex.network/data-saver/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa/2.jpg",
                "https://cmdxd98sb0x3yprd.mangadex.network/data-saver/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa/3.jpg"
            )
        )

        composeRule.setContent {
            FableTheme {
                MangaChapterReader(
                    story = story,
                    chapter = chapter,
                    chapterNumber = 1,
                    chapterTotal = 1,
                    mode = MangaReadingMode.PAGE_LTR,
                    palette = readerPalette(ReaderTheme.CHARCOAL),
                    onSaveProgress = { _, _ -> },
                    onToggleChrome = {}
                )
            }
        }

        composeRule.onNodeWithText("Page 1 of 3").assertIsDisplayed()
        composeRule.onRoot().performTouchInput { click(Offset(width * 0.9f, height * 0.5f)) }
        composeRule.onNodeWithText("Page 2 of 3").assertIsDisplayed()
        composeRule.onRoot().performTouchInput { click(Offset(width * 0.1f, height * 0.5f)) }
        composeRule.onNodeWithText("Page 1 of 3").assertIsDisplayed()
    }

    @Test
    fun zoomPillTogglesScaleAndLastPageAdvancesToNextChapter() {
        val story = mangaStory(progressPercent = 100)
        val chapter = Chapter(
            id = "chap-1",
            storyId = story.id,
            chapterNumber = 1,
            title = "Chapter 1",
            pageUrls = listOf(
                "https://cmdxd98sb0x3yprd.mangadex.network/data-saver/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa/1.jpg",
                "https://cmdxd98sb0x3yprd.mangadex.network/data-saver/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa/2.jpg"
            )
        )
        var advancedToNextChapter = false

        composeRule.setContent {
            FableTheme {
                MangaChapterReader(
                    story = story,
                    chapter = chapter,
                    chapterNumber = 1,
                    chapterTotal = 2,
                    mode = MangaReadingMode.PAGE_LTR,
                    palette = readerPalette(ReaderTheme.CHARCOAL),
                    hasNextChapter = true,
                    onNextChapter = { advancedToNextChapter = true },
                    onSaveProgress = { _, _ -> },
                    onToggleChrome = {}
                )
            }
        }

        composeRule.onNodeWithText("Zoom").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("250% · Reset").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Zoom").assertIsDisplayed()

        composeRule.onRoot().performTouchInput { click(Offset(width * 0.9f, height * 0.5f)) }
        composeRule.onNodeWithText("Page 2 of 2").assertIsDisplayed()
        composeRule.onNodeWithText("Next chapter").assertIsDisplayed().performClick()
        assertTrue(advancedToNextChapter)
    }

    private fun mangaStory(progressPercent: Int) = Story(
        id = "manga-1",
        title = "Manga Test",
        author = "Mangaka",
        genre = "Action",
        synopsis = "Synopsis",
        content = "",
        progressPercent = progressPercent,
        totalChapters = 1,
        lastReadChapterNumber = 1,
        contentFormat = ContentFormat.MANGA,
        sourceProvider = SourceProvider.MANGADEX,
        providerId = "manga-1"
    )
}
