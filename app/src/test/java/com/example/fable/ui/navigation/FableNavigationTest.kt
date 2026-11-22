package com.example.fable.ui.navigation

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FableNavigationTest {
    @Test
    fun openingStoryFromAccountPreservesAccountOnBack() {
        val stack = FableNavBackStack()
            .selectTab(FableTab.EXPLORE)
            .openAccount()
            .openStory("story-1")

        assertEquals(FableDestination.StoryDetail("story-1"), stack.current)
        assertFalse(stack.showBottomBar)
        assertTrue(stack.canGoBack)

        val backToAccount = stack.popBack()
        assertEquals(FableDestination.Account, backToAccount.current)
        assertEquals(FableTab.EXPLORE, backToAccount.currentTab)

        val backToExplore = backToAccount.popBack()
        assertEquals(FableDestination.TabRoot(FableTab.EXPLORE), backToExplore.current)
        assertTrue(backToExplore.showBottomBar)
        assertFalse(backToExplore.canGoBack)
    }

    @Test
    fun deduplicatesRepeatedTopDestinationsAndReplacesActiveReaderChapter() {
        val detailStack = FableNavBackStack()
            .openStory("story-1")
            .openStory("story-1")
        assertEquals(2, detailStack.entries.size)

        val readerStack = detailStack
            .openReader("story-1", 1)
            .openReader("story-1", 3)
        assertEquals(3, readerStack.entries.size)
        assertEquals(FableDestination.Reader("story-1", 3), readerStack.current)
    }

    @Test
    fun popStoryRemovesDetailReaderAndEditEntriesForDeletedStory() {
        val stack = FableNavBackStack()
            .openAccount()
            .openStory("story-deleted")
            .openReader("story-deleted", 2)
            .openEditStory("story-deleted")

        val afterDelete = stack.popStory("story-deleted")

        assertEquals(FableDestination.Account, afterDelete.current)
        assertEquals(2, afterDelete.entries.size)
    }

    @Test
    fun saverRoundTripsBackStackWithSpecialCharactersAndFallsBackOnMalformedTokens() {
        val original = FableNavBackStack()
            .selectTab(FableTab.SHELF)
            .openAccount()
            .openStory("ns:id%42")
            .openReader("ns:id%42", 5)
            .openEditStory("ns:id%42")

        val restored = with(FableNavBackStack.Saver) {
            val scope = SaverScope { true }
            val saved = scope.save(original)!!
            restore(saved)!!
        }

        assertEquals(original, restored)

        val fallback = FableNavBackStack.Saver.restore(listOf("account", "invalid:token"))
        assertEquals(FableNavBackStack(), fallback)
    }
}
