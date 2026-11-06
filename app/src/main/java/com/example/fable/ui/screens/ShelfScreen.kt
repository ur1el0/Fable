package com.example.fable.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fable.domain.model.Story
import com.example.fable.ui.components.EmptyState
import com.example.fable.ui.components.StoryRow

@Composable
fun ShelfScreen(
    modifier: Modifier = Modifier,
    stories: List<Story> = emptyList(),
    onStorySelected: (Story) -> Unit = {},
    onToggleBookmark: (Story) -> Unit = {}
) {
    var selectedShelf by rememberSaveable { mutableStateOf("Saved") }
    val visibleStories = when (selectedShelf) {
        "Finished" -> stories.filter(Story::isCompleted)
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
        item {
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Saved", "Finished").forEach { tab ->
                    FilterChip(selected = selectedShelf == tab, onClick = { selectedShelf = tab }, label = { Text(tab) })
                }
            }
        }
        item {
            Text(
                if (selectedShelf == "Finished") "Finished stories" else "Saved and in progress",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
        if (visibleStories.isEmpty()) {
            item {
                EmptyState(
                    title = if (selectedShelf == "Finished") "No finished stories" else "Your shelf is empty",
                    message = if (selectedShelf == "Finished") "Stories you finish will be collected here." else "Saved stories and reading progress will show here."
                )
            }
        } else {
            items(visibleStories, key = Story::id) { story ->
                StoryRow(story, onClick = { onStorySelected(story) }, onToggleBookmark = { onToggleBookmark(story) })
            }
        }
    }
}
