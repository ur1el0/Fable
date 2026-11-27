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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "CREATE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Write", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        "WRITER'S STUDIO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Card 1: IDENTITY
        item {
            StudioCard(sectionTitle = "IDENTITY") {
                OutlinedTextField(
                    title,
                    { title = it.take(120) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Story title") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    genre,
                    { genre = it.take(50) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Genre") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Card 2: SYNOPSIS
        item {
            StudioCard(sectionTitle = "SYNOPSIS") {
                OutlinedTextField(
                    synopsis,
                    { synopsis = it.take(2_000) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Synopsis") },
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    supportingText = {
                        Text("${synopsis.length} / 2,000", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                    }
                )
            }
        }

        // Card 3: MANUSCRIPT
        item {
            StudioCard(sectionTitle = "MANUSCRIPT") {
                val words = content.trim().split(Regex("\\s+")).count { word -> word.isNotEmpty() }
                OutlinedTextField(
                    content,
                    { content = it.take(50_000) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Opening chapter") },
                    minLines = 8,
                    shape = RoundedCornerShape(12.dp),
                    supportingText = {
                        Text("$words words", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                    }
                )
            }
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
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when {
                        isPublishing -> "Publishing..."
                        isSignedIn -> "Publish story"
                        else -> "Sign in to publish"
                    },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun StudioCard(
    sectionTitle: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                sectionTitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}
