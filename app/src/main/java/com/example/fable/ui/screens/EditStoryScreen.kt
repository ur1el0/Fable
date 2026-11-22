package com.example.fable.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.fable.domain.model.GenreCategory
import com.example.fable.domain.model.Story
import com.example.fable.domain.reading.ReaderPagePacker
import com.example.fable.ui.components.GenreInputField

@Composable
fun EditStoryScreen(
    story: Story?,
    isLoading: Boolean,
    isSaving: Boolean,
    isDeleting: Boolean,
    errorMessage: String?,
    genres: List<GenreCategory> = emptyList(),
    onBack: () -> Unit,
    onRetryLoad: () -> Unit,
    onSave: (title: String, genre: String, chapterTitle: String, synopsis: String, content: String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
            }
            Text("Edit story", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            if (story != null) {
                IconButton(
                    onClick = { showDeleteConfirmation = true },
                    enabled = !isSaving && !isDeleting,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete story",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        when {
            story == null || isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator()
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                errorMessage ?: "Story could not be loaded for editing.",
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            OutlinedButton(onClick = onRetryLoad) { Text("Retry") }
                        }
                    }
                }
            }
            story.content.isBlank() && errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        OutlinedButton(onClick = onRetryLoad) { Text("Retry") }
                    }
                }
            }
            else -> {
                var title by rememberSaveable(story.id, story.updatedAtUtc, story.content.isNotBlank()) {
                    mutableStateOf(story.title)
                }
                var genre by rememberSaveable(story.id, story.updatedAtUtc, story.content.isNotBlank()) {
                    mutableStateOf(story.genre)
                }
                var chapterTitle by rememberSaveable(story.id, story.updatedAtUtc, story.content.isNotBlank()) {
                    mutableStateOf(story.chapter.ifBlank { "Chapter 1" })
                }
                var synopsis by rememberSaveable(story.id, story.updatedAtUtc, story.content.isNotBlank()) {
                    mutableStateOf(story.synopsis)
                }
                var content by rememberSaveable(story.id, story.updatedAtUtc, story.content.isNotBlank()) {
                    mutableStateOf(story.content)
                }
                var showGenreMenu by rememberSaveable { mutableStateOf(false) }

                val availableGenres = remember(genres) {
                    genres.mapNotNull { it.name.trim().takeIf(String::isNotEmpty) }
                        .distinctBy(String::lowercase)
                }
                val contentWordCount = remember(content) { ReaderPagePacker.countWords(content) }
                val busy = isSaving || isDeleting

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it.take(120) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !busy,
                            label = { Text("Story title") },
                            singleLine = true
                        )
                    }
                    item {
                        GenreInputField(
                            genre = genre,
                            availableGenres = availableGenres,
                            enabled = !busy,
                            expanded = showGenreMenu,
                            onExpandedChange = { showGenreMenu = it },
                            onGenreChange = { genre = it }
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = chapterTitle,
                            onValueChange = { chapterTitle = it.take(120) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !busy,
                            label = { Text("Chapter title") },
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = synopsis,
                            onValueChange = { synopsis = it.take(2_000) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !busy,
                            label = { Text("Synopsis") },
                            minLines = 3,
                            supportingText = {
                                Text(
                                    "${synopsis.length} / 2,000",
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = content,
                            onValueChange = { content = it.take(50_000) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !busy,
                            label = { Text("Opening chapter") },
                            minLines = 8,
                            supportingText = {
                                Text(
                                    "$contentWordCount words",
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        )
                    }
                    if (errorMessage != null) {
                        item {
                            Text(
                                errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    item {
                        Button(
                            onClick = { onSave(title, genre, chapterTitle, synopsis, content) },
                            enabled = !busy &&
                                title.isNotBlank() &&
                                genre.isNotBlank() &&
                                synopsis.isNotBlank() &&
                                content.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isSaving) "Saving..." else "Save changes")
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmation && story != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete \"${story.title}\"?") },
            text = {
                Text("This permanently removes this story and all of its chapters from Fable. This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
