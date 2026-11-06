package com.example.fable.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import coil3.compose.AsyncImage
import com.example.fable.domain.model.Chapter
import com.example.fable.domain.model.ContentFormat
import com.example.fable.domain.model.Story
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.collect
import androidx.compose.runtime.snapshotFlow
import kotlin.math.roundToInt

@OptIn(FlowPreview::class)
@Composable
fun StoryReaderScreen(
    story: Story,
    chapters: List<Chapter>,
    chapterNumber: Int,
    errorMessage: String?,
    onBack: () -> Unit,
    onSelectChapter: (Int) -> Unit,
    onSaveProgress: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val chapterIndex = chapters.indexOfFirst { it.chapterNumber == chapterNumber }
    val chapter = chapters.getOrNull(chapterIndex)
    val chapterTotal = maxOf(story.totalChapters, chapters.size, 1)
    val content = chapter?.content?.takeIf(String::isNotBlank) ?: story.content

    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back to story")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(story.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(chapter?.title ?: "Chapter $chapterNumber", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Text("${story.progressPercent}%", style = MaterialTheme.typography.labelMedium)
        }
        LinearProgressIndicator(
            progress = { story.progressPercent.coerceIn(0, 100) / 100f },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.tertiary
        )
        if (errorMessage != null) {
            Text(errorMessage, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        key(story.id, chapterNumber) {
            val scrollState = rememberScrollState()
            LaunchedEffect(scrollState, chapterNumber) {
                snapshotFlow {
                    if (scrollState.maxValue == 0) 0
                    else (scrollState.value * 100f / scrollState.maxValue).roundToInt().coerceIn(0, 100) / 5 * 5
                }
                    .distinctUntilChanged()
                    .filter { it in 5..100 }
                    .debounce(900)
                    .map { localProgress ->
                        val overall = ((chapterNumber - 1).coerceAtLeast(0) + localProgress / 100f) / chapterTotal * 100
                        overall.roundToInt().coerceIn(1, 100)
                    }
                    .collect { progress -> onSaveProgress(progress, chapterNumber) }
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    chapter?.title ?: story.chapter.ifBlank { "Chapter $chapterNumber" },
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = FontFamily.Serif
                )
                if (story.contentFormat == ContentFormat.MANGA && chapter?.pageUrls?.isNotEmpty() == true) {
                    chapter.pageUrls.forEachIndexed { index, pageUrl ->
                        AsyncImage(
                            model = pageUrl,
                            contentDescription = "Page ${index + 1}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth().height(460.dp).clip(RoundedCornerShape(4.dp))
                        )
                    }
                } else if (content.isNotBlank()) {
                    Text(content, style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Serif, fontSize = 18.sp, lineHeight = 31.sp))
                } else {
                    Text(
                        "This chapter is not available offline yet.",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { chapters.getOrNull(chapterIndex - 1)?.let { onSelectChapter(it.chapterNumber) } },
                enabled = chapterIndex > 0,
                modifier = Modifier.weight(1f)
            ) { Text("Previous") }
            Button(
                onClick = {
                    val next = chapters.getOrNull(chapterIndex + 1)
                    if (next != null) onSelectChapter(next.chapterNumber)
                    else onSaveProgress(100, chapterNumber)
                },
                modifier = Modifier.weight(1f)
            ) { Text(if (chapterIndex in 0 until chapters.lastIndex) "Next chapter" else "Finish") }
        }
    }
}
