package com.example.fable.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fable.domain.model.GenreCategory
import com.example.fable.domain.model.Story
import com.example.fable.ui.components.EmptyState
import com.example.fable.ui.components.SectionTitle
import com.example.fable.ui.components.StoryCover
import com.example.fable.ui.components.StoryRow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    stories: List<Story> = emptyList(),
    genres: List<GenreCategory> = emptyList(),
    isRefreshing: Boolean = false,
    errorMessage: String? = null,
    onRefresh: () -> Unit = {},
    onStorySelected: (Story) -> Unit = {},
    onToggleBookmark: (Story) -> Unit = {}
) {
    var selectedGenre by rememberSaveable { mutableStateOf("All") }
    val filteredStories = remember(stories, selectedGenre) {
        if (selectedGenre == "All") stories else stories.filter { it.genre.equals(selectedGenre, true) }
    }
    val featured = filteredStories.firstOrNull { it.isTaleOfTheDay }
        ?: filteredStories.firstOrNull { it.isCuratorSpotlight }
        ?: filteredStories.firstOrNull()
    val continueReading = filteredStories.filter { it.progressPercent > 0 && !it.isCompleted }
    val recent = filteredStories.filter { it.isRecentSubmission }.ifEmpty { filteredStories }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())).uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("Library", style = MaterialTheme.typography.displaySmall)
                }
                IconButton(onClick = onRefresh, enabled = !isRefreshing, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Refresh catalog")
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(selected = selectedGenre == "All", onClick = { selectedGenre = "All" }, label = { Text("All") })
                }
                items(genres, key = GenreCategory::id) { genre ->
                    FilterChip(
                        selected = selectedGenre == genre.name,
                        onClick = { selectedGenre = genre.name },
                        label = { Text(genre.name) }
                    )
                }
            }
        }

        if (errorMessage != null) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(errorMessage, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }

        if (featured != null) {
            item { FeaturedStory(story = featured, onClick = { onStorySelected(featured) }, onBookmark = { onToggleBookmark(featured) }) }
        } else {
            item {
                EmptyState(
                    title = "Your library is quiet",
                    message = if (isRefreshing) "Loading stories" else "No stories are available yet.",
                    actionLabel = if (isRefreshing) null else "Refresh",
                    onAction = onRefresh.takeUnless { isRefreshing }
                )
            }
        }

        if (continueReading.isNotEmpty()) {
            item { SectionTitle("Continue reading") }
            items(continueReading, key = Story::id) { story ->
                StoryRow(story, onClick = { onStorySelected(story) }, onToggleBookmark = { onToggleBookmark(story) })
            }
        }

        if (recent.isNotEmpty()) {
            item { SectionTitle("Recent stories") }
            items(recent.take(8), key = Story::id) { story ->
                StoryRow(story, onClick = { onStorySelected(story) }, onToggleBookmark = { onToggleBookmark(story) })
            }
        }
    }
}

@Composable
private fun FeaturedStory(story: Story, onClick: () -> Unit, onBookmark: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.48f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp, top = 14.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        story.badgeText ?: if (story.isTaleOfTheDay) "TALE OF THE DAY" else "EDITOR'S PICK",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
            IconButton(onClick = onBookmark, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = if (story.isBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (story.isBookmarked) "Remove bookmark" else "Save story",
                    tint = if (story.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        StoryCover(story, Modifier.fillMaxWidth().height(190.dp).padding(horizontal = 12.dp))
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(story.author, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                story.title,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                story.synopsis,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
