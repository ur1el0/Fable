package com.example.fable.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fable.domain.model.Story
import com.example.fable.domain.model.ReadingStats
import com.example.fable.ui.components.EmptyState
import com.example.fable.ui.components.StoryRow

@Composable
fun ShelfScreen(
    modifier: Modifier = Modifier,
    stories: List<Story> = emptyList(),
    offlineStoryIds: Set<String> = emptySet(),
    readingStats: ReadingStats = ReadingStats(),
    showReadingStats: Boolean = false,
    onStorySelected: (Story) -> Unit = {},
    onToggleBookmark: (Story) -> Unit = {}
) {
    var selectedShelf by rememberSaveable { mutableStateOf("Saved") }
    val visibleStories = when (selectedShelf) {
        "Finished" -> stories.filter(Story::isCompleted)
        "Offline" -> stories.filter { it.id in offlineStoryIds }
        else -> stories.filter { it.isBookmarked || it.progressPercent > 0 }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 22.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("MY SHELF", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text("Your shelf", style = MaterialTheme.typography.displaySmall)
        }
        if (showReadingStats) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReadingStatMetric(
                        value = readingStats.storiesReadCount.toString(),
                        label = "Stories finished",
                        modifier = Modifier.weight(1f)
                    )
                    ReadingStatMetric(
                        value = readingStats.totalMinutesRead.toString(),
                        label = "Minutes read",
                        modifier = Modifier.weight(1f)
                    )
                    ReadingStatMetric(
                        value = readingStats.streakDays.toString(),
                        label = "Day streak",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Saved", "Finished", "Offline").forEach { tab ->
                    FilterChip(selected = selectedShelf == tab, onClick = { selectedShelf = tab }, label = { Text(tab) })
                }
            }
        }
        item {
            Text(
                when (selectedShelf) {
                    "Finished" -> "Finished stories"
                    "Offline" -> "Saved on this device"
                    else -> "Saved and in progress"
                },
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
        if (visibleStories.isEmpty()) {
            item {
                EmptyState(
                    title = when (selectedShelf) {
                        "Finished" -> "No finished stories"
                        "Offline" -> "No offline stories"
                        else -> "Your shelf is empty"
                    },
                    message = when (selectedShelf) {
                        "Finished" -> "Stories you finish will be collected here."
                        "Offline" -> "Download chapters from a story page to read offline."
                        else -> "Saved stories and reading progress will show here."
                    }
                )
            }
        } else {
            items(visibleStories, key = Story::id) { story ->
                StoryRow(
                    story = story,
                    onClick = { onStorySelected(story) },
                    onToggleBookmark = { onToggleBookmark(story) },
                    isAvailableOffline = story.id in offlineStoryIds
                )
            }
        }
    }
}

@Composable
private fun ReadingStatMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
