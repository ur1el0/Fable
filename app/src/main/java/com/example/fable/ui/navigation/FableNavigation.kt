package com.example.fable.ui.navigation

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

enum class FableTab(val label: String) {
    LIBRARY("Library"),
    EXPLORE("Explore"),
    WRITE("Write"),
    SHELF("Shelf")
}

sealed interface FableDestination {
    data class TabRoot(val tab: FableTab) : FableDestination
    data object Account : FableDestination
    data class StoryDetail(val storyId: String) : FableDestination
    data class Reader(val storyId: String, val chapterNumber: Int?) : FableDestination
    data class EditStory(val storyId: String) : FableDestination
}

data class FableNavBackStack(
    val entries: List<FableDestination> = listOf(FableDestination.TabRoot(FableTab.LIBRARY))
) {
    init {
        require(entries.isNotEmpty() && entries.first() is FableDestination.TabRoot) {
            "FableNavBackStack must start at a TabRoot destination."
        }
    }

    val current: FableDestination
        get() = entries.last()

    val currentTab: FableTab
        get() = (entries.first() as FableDestination.TabRoot).tab

    val showBottomBar: Boolean
        get() = current is FableDestination.TabRoot

    val canGoBack: Boolean
        get() = entries.size > 1

    fun selectTab(tab: FableTab): FableNavBackStack =
        FableNavBackStack(listOf(FableDestination.TabRoot(tab)))

    fun openAccount(): FableNavBackStack =
        if (current == FableDestination.Account) this else copy(entries = entries + FableDestination.Account)

    fun openStory(storyId: String): FableNavBackStack {
        val destination = FableDestination.StoryDetail(storyId)
        return if (current == destination) this else copy(entries = entries + destination)
    }

    fun openReader(storyId: String, chapterNumber: Int?): FableNavBackStack {
        val destination = FableDestination.Reader(storyId, chapterNumber)
        return when (val top = current) {
            is FableDestination.Reader if top.storyId == storyId ->
                copy(entries = entries.dropLast(1) + destination)
            else -> copy(entries = entries + destination)
        }
    }

    fun openEditStory(storyId: String): FableNavBackStack {
        val destination = FableDestination.EditStory(storyId)
        return if (current == destination) this else copy(entries = entries + destination)
    }

    fun popBack(): FableNavBackStack =
        if (entries.size > 1) copy(entries = entries.dropLast(1)) else this

    fun popStory(storyId: String): FableNavBackStack {
        val remaining = entries.filterNot { destination ->
            when (destination) {
                is FableDestination.StoryDetail -> destination.storyId == storyId
                is FableDestination.Reader -> destination.storyId == storyId
                is FableDestination.EditStory -> destination.storyId == storyId
                is FableDestination.TabRoot, FableDestination.Account -> false
            }
        }
        return if (remaining.firstOrNull() is FableDestination.TabRoot) {
            FableNavBackStack(remaining)
        } else {
            FableNavBackStack()
        }
    }

    companion object {
        val Saver: Saver<FableNavBackStack, Any> = listSaver(
            save = { backStack -> backStack.entries.map(::encodeDestination) },
            restore = { saved ->
                val decoded = saved.mapNotNull(::decodeDestination)
                if (decoded.firstOrNull() is FableDestination.TabRoot) {
                    FableNavBackStack(decoded)
                } else {
                    FableNavBackStack()
                }
            }
        )

        private fun encodeDestination(destination: FableDestination): String = when (destination) {
            is FableDestination.TabRoot -> "tab:${destination.tab.name}"
            FableDestination.Account -> "account"
            is FableDestination.StoryDetail -> "detail:${escapeToken(destination.storyId)}"
            is FableDestination.Reader ->
                "reader:${escapeToken(destination.storyId)}:${destination.chapterNumber?.toString().orEmpty()}"
            is FableDestination.EditStory -> "edit:${escapeToken(destination.storyId)}"
        }

        private fun decodeDestination(token: String): FableDestination? {
            return when {
                token.startsWith("tab:") -> {
                    val tab = FableTab.entries.firstOrNull { it.name == token.removePrefix("tab:") }
                        ?: return null
                    FableDestination.TabRoot(tab)
                }
                token == "account" -> FableDestination.Account
                token.startsWith("detail:") -> {
                    val id = unescapeToken(token.removePrefix("detail:")).takeIf(String::isNotBlank)
                        ?: return null
                    FableDestination.StoryDetail(id)
                }
                token.startsWith("reader:") -> {
                    val payload = token.removePrefix("reader:")
                    val sep = payload.lastIndexOf(':')
                    if (sep <= 0) return null
                    val id = unescapeToken(payload.substring(0, sep)).takeIf(String::isNotBlank)
                        ?: return null
                    val chapter = payload.substring(sep + 1).takeIf(String::isNotBlank)?.toIntOrNull()
                    FableDestination.Reader(id, chapter)
                }
                token.startsWith("edit:") -> {
                    val id = unescapeToken(token.removePrefix("edit:")).takeIf(String::isNotBlank)
                        ?: return null
                    FableDestination.EditStory(id)
                }
                else -> null
            }
        }

        private fun escapeToken(value: String): String =
            value.replace("%", "%25").replace(":", "%3A")

        private fun unescapeToken(value: String): String {
            val out = StringBuilder(value.length)
            var index = 0
            while (index < value.length) {
                when {
                    value.startsWith("%3A", index) -> {
                        out.append(':')
                        index += 3
                    }
                    value.startsWith("%25", index) -> {
                        out.append('%')
                        index += 3
                    }
                    else -> {
                        out.append(value[index])
                        index += 1
                    }
                }
            }
            return out.toString()
        }
    }
}
