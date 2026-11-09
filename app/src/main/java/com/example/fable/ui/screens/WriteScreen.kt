package com.example.fable.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.fable.domain.model.WriterDraft
import com.example.fable.ui.viewmodel.WriterDraftUiState

@Composable
fun WriteScreen(
    modifier: Modifier = Modifier,
    expectedOwnerId: String,
    draftState: WriterDraftUiState,
    isSignedIn: Boolean = true,
    isPublishing: Boolean = false,
    publishErrorMessage: String? = null,
    onSignIn: () -> Unit = {},
    onDraftChanged: (WriterDraft) -> Unit = {},
    onDiscardDraft: () -> Unit = {},
    onPublish: (WriterDraft) -> Unit = {}
) {
    val draft = draftState.draft
    val isDraftReady = !draftState.isLoading && draftState.ownerId == expectedOwnerId
    val hasDraftContent = draft.title.isNotBlank() || draft.genre.isNotBlank() ||
        draft.chapterTitle.isNotBlank() && draft.chapterTitle != "Chapter 1" ||
        draft.synopsis.isNotBlank() || draft.content.isNotBlank()
    var showDiscardConfirmation by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 22.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("CREATE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Write", style = MaterialTheme.typography.displaySmall)
                }
                if (isDraftReady && hasDraftContent) {
                    IconButton(
                        onClick = { showDiscardConfirmation = true },
                        enabled = !draftState.isDiscarding && !isPublishing
                    ) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = "Discard draft",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        if (!isDraftReady) {
            item { Text("Restoring draft...", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            item {
                OutlinedTextField(
                    draft.title,
                    { onDraftChanged(draft.copy(title = it.take(120))) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !draftState.isDiscarding,
                    label = { Text("Story title") },
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    draft.genre,
                    { onDraftChanged(draft.copy(genre = it.take(50))) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !draftState.isDiscarding,
                    label = { Text("Genre") },
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    draft.chapterTitle,
                    { onDraftChanged(draft.copy(chapterTitle = it.take(120))) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !draftState.isDiscarding,
                    label = { Text("Chapter title") },
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    draft.synopsis,
                    { onDraftChanged(draft.copy(synopsis = it.take(2_000))) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !draftState.isDiscarding,
                    label = { Text("Synopsis") },
                    minLines = 3,
                    supportingText = {
                        Text(
                            "${draft.synopsis.length} / 2,000",
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                )
            }
            item {
                OutlinedTextField(
                    draft.content,
                    { onDraftChanged(draft.copy(content = it.take(50_000))) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !draftState.isDiscarding,
                    label = { Text("Opening chapter") },
                    minLines = 8,
                    supportingText = {
                        Text(
                            "${draft.content.trim().split(Regex("\\s+")).count { word -> word.isNotEmpty() }} words",
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                )
            }
            item {
                Text(
                    when {
                        draftState.errorMessage != null -> draftState.errorMessage
                        draftState.isDiscarding -> "Discarding draft..."
                        draftState.isSaving || draftState.hasUnsavedChanges -> "Saving draft..."
                        !hasDraftContent -> "No draft saved yet."
                        else -> "Draft saved on this device."
                    },
                    color = if (draftState.errorMessage != null) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (!isSignedIn) {
                item {
                    Text("Sign in to publish a story.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (publishErrorMessage != null) {
                item {
                    Text(publishErrorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                Button(
                    onClick = {
                        if (isSignedIn) onPublish(draft)
                        else onSignIn()
                    },
                    enabled = !isPublishing && !draftState.isDiscarding && (!isSignedIn || draft.title.isNotBlank() && draft.genre.isNotBlank() && draft.synopsis.isNotBlank() && draft.content.isNotBlank()),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(when {
                        isPublishing -> "Publishing..."
                        isSignedIn -> "Publish story"
                        else -> "Sign in to publish"
                    })
                }
            }
        }
    }

    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = { Text("Discard draft?") },
            text = { Text("This removes the saved title, genre, chapter, synopsis, and manuscript from this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirmation = false
                        onDiscardDraft()
                    }
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
