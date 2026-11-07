package com.example.fable.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun WriteScreen(
    modifier: Modifier = Modifier,
    isSignedIn: Boolean = true,
    isPublishing: Boolean = false,
    publishErrorMessage: String? = null,
    onSignIn: () -> Unit = {},
    onPublish: (title: String, genre: String, synopsis: String, content: String) -> Unit = { _, _, _, _ -> }
) {
    var title by rememberSaveable { mutableStateOf("") }
    var genre by rememberSaveable { mutableStateOf("") }
    var synopsis by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 22.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("CREATE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text("Write", style = MaterialTheme.typography.displaySmall)
        }
        item {
            OutlinedTextField(title, { title = it.take(120) }, modifier = Modifier.fillMaxWidth(), label = { Text("Story title") }, singleLine = true)
        }
        item {
            OutlinedTextField(genre, { genre = it.take(50) }, modifier = Modifier.fillMaxWidth(), label = { Text("Genre") }, singleLine = true)
        }
        item {
            OutlinedTextField(
                synopsis,
                { synopsis = it.take(2_000) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Synopsis") },
                minLines = 3,
                supportingText = { Text("${synopsis.length} / 2,000", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) }
            )
        }
        item {
            OutlinedTextField(
                content,
                { content = it.take(50_000) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Opening chapter") },
                minLines = 8,
                supportingText = { Text("${content.trim().split(Regex("\\s+")).count { word -> word.isNotEmpty() }} words", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) }
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
                    if (isSignedIn) onPublish(title.trim(), genre.trim(), synopsis.trim(), content.trim())
                    else onSignIn()
                },
                enabled = !isPublishing && (!isSignedIn || title.isNotBlank() && genre.isNotBlank() && synopsis.isNotBlank() && content.isNotBlank()),
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
