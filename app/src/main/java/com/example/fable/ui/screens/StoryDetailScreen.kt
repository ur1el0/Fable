package com.example.fable.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.fable.domain.model.Chapter
import com.example.fable.domain.model.Story
import com.example.fable.ui.components.StoryCover

@Composable
fun StoryDetailScreen(
    story: Story,
    chapters: List<Chapter>,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onRead: (Int?) -> Unit,
    onSelectChapter: (Int) -> Unit,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
            }
            Text("Story", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onToggleBookmark, modifier = Modifier.size(48.dp)) {
                Icon(
                    if (story.isBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (story.isBookmarked) "Remove bookmark" else "Save story",
                    tint = if (story.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StoryCover(story, Modifier.fillMaxWidth().height(230.dp))
            Text(story.title, style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Serif)
            Text(story.author, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "${story.genre}  ·  ${story.readTimeMinutes.coerceAtLeast(1)} min read  ·  ${story.contentFormat.name.lowercase().replaceFirstChar(Char::uppercase)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(story.synopsis.ifBlank { "No synopsis is available yet." }, style = MaterialTheme.typography.bodyLarge)
            if (story.progressPercent > 0) {
                LinearProgressIndicator(
                    progress = { story.progressPercent.coerceIn(0, 100) / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text("${story.progressPercent.coerceIn(0, 100)}% read", style = MaterialTheme.typography.labelMedium)
            }
            if (errorMessage != null) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = { onRead(story.lastReadChapterNumber ?: chapters.firstOrNull()?.chapterNumber) },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (story.progressPercent > 0) "Continue reading" else "Start reading")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Chapters", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (isLoading) Text("Loading", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (chapters.isEmpty() && !isLoading) {
                Text("Chapter information is not available offline.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            chapters.forEach { chapter ->
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onSelectChapter(chapter.chapterNumber) },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                chapter.title.ifBlank { "Chapter ${chapter.chapterNumber}" },
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text("${chapter.wordCount} words", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("Read", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
