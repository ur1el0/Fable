package com.example.fable.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.fable.domain.model.GenreCategory
import com.example.fable.domain.model.Story
import com.example.fable.domain.model.StoryCatalog
import com.example.fable.domain.model.Writer
import com.example.fable.ui.components.EmptyState
import com.example.fable.ui.components.SectionTitle
import com.example.fable.ui.components.StoryRow

@Composable
fun ExploreScreen(
    modifier: Modifier = Modifier,
    stories: List<Story> = emptyList(),
    providerStories: List<Story> = emptyList(),
    genres: List<GenreCategory> = emptyList(),
    writers: List<Writer> = emptyList(),
    isSearchingProvider: Boolean = false,
    providerErrorMessage: String? = null,
    onSearchProvider: (StoryCatalog, String) -> Unit = { _, _ -> },
    onStorySelected: (Story) -> Unit = {},
    onToggleBookmark: (Story) -> Unit = {}
) {
    var query by rememberSaveable { mutableStateOf("") }
    var format by rememberSaveable { mutableStateOf("All") }
    var selectedCatalog by rememberSaveable { mutableStateOf(StoryCatalog.FABLE) }
    val catalogStories = if (selectedCatalog == StoryCatalog.FABLE) stories else providerStories
    val filtered = remember(catalogStories, selectedCatalog, query, format) {
        catalogStories.filter { story ->
            val matchesQuery = selectedCatalog != StoryCatalog.FABLE ||
                query.isBlank() || story.title.contains(query, true) || story.author.contains(query, true)
            val matchesFormat = format == "All" || story.contentFormat.name.equals(format, true)
            matchesQuery && matchesFormat
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 22.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("DISCOVERY", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text("Explore", style = MaterialTheme.typography.displaySmall)
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    if (selectedCatalog != StoryCatalog.FABLE) onSearchProvider(selectedCatalog, it)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                placeholder = { Text("Stories, authors, genres") },
                shape = MaterialTheme.shapes.small
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(StoryCatalog.entries, key = StoryCatalog::name) { catalog ->
                    FilterChip(
                        selected = selectedCatalog == catalog,
                        onClick = {
                            selectedCatalog = catalog
                            format = "All"
                            onSearchProvider(catalog, query)
                        },
                        label = {
                            Text(
                                when (catalog) {
                                    StoryCatalog.FABLE -> "Fable"
                                    StoryCatalog.GUTENBERG -> "Gutenberg"
                                    StoryCatalog.MANGA -> "MangaDex"
                                }
                            )
                        }
                    )
                }
            }
        }
        if (selectedCatalog == StoryCatalog.FABLE) item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("All", "PROSE", "MANGA")) { option ->
                    FilterChip(selected = format == option, onClick = { format = option }, label = { Text(option.lowercase().replaceFirstChar(Char::uppercase)) })
                }
            }
        }

        if (genres.isNotEmpty()) {
            item { SectionTitle("Popular genres") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(genres, key = GenreCategory::id) { genre ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                query = genre.name
                                if (selectedCatalog != StoryCatalog.FABLE) onSearchProvider(selectedCatalog, genre.name)
                            },
                            label = { Text(genre.name) }
                        )
                    }
                }
            }
        }

        if (writers.isNotEmpty()) {
            item { SectionTitle("Trending writers") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    items(writers, key = Writer::id) { writer ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(76.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(56.dp)
                            ) {
                                if (!writer.avatarImageUrl.isNullOrBlank()) {
                                    AsyncImage(model = writer.avatarImageUrl, contentDescription = "Portrait of ${writer.name}", modifier = Modifier.clip(CircleShape))
                                } else {
                                    BoxInitials(writer.name)
                                }
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(writer.name, style = MaterialTheme.typography.labelMedium, maxLines = 2)
                            Text("${writer.storyCount} stories", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        item {
            SectionTitle(
                when {
                    query.isNotBlank() -> "Search results"
                    selectedCatalog == StoryCatalog.GUTENBERG -> "Project Gutenberg"
                    selectedCatalog == StoryCatalog.MANGA -> "Manga"
                    else -> "Stories"
                }
            )
        }
        if (!providerErrorMessage.isNullOrBlank() && filtered.isNotEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(
                            text = "Catalog refresh failed. Showing saved results. $providerErrorMessage",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        TextButton(
                            onClick = { onSearchProvider(selectedCatalog, query) },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
        if (filtered.isEmpty()) {
            item {
                EmptyState(
                    title = if (query.isBlank()) "No stories to explore" else "No matches",
                    message = when {
                        isSearchingProvider -> "Searching ${selectedCatalog.name.lowercase().replaceFirstChar(Char::uppercase)}"
                        providerErrorMessage != null && selectedCatalog != StoryCatalog.FABLE -> providerErrorMessage
                        query.isBlank() -> "The catalog will appear here when it is available."
                        else -> "Try a different title, author, or genre."
                    }
                )
            }
        } else {
            items(filtered, key = Story::id) { story ->
                StoryRow(story, onClick = { onStorySelected(story) }, onToggleBookmark = { onToggleBookmark(story) })
            }
        }
    }
}

@Composable
private fun BoxInitials(name: String) {
    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Text(
            name.split(' ').take(2).mapNotNull(String::firstOrNull).joinToString("").uppercase(),
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary
        )
    }
}
