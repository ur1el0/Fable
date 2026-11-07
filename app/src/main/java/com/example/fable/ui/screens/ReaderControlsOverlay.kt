package com.example.fable.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fable.domain.model.Chapter
import com.example.fable.domain.model.Story
import com.example.fable.ui.theme.FableAccent
import com.example.fable.ui.theme.ReaderPalette

@Composable
fun ReaderControlsOverlay(
    visible: Boolean,
    story: Story,
    chapter: Chapter?,
    chapterNumber: Int,
    errorMessage: String?,
    palette: ReaderPalette,
    canGoToPreviousChapter: Boolean,
    hasNextChapter: Boolean,
    onBack: () -> Unit,
    onDisplayOptions: () -> Unit,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier.fillMaxSize(),
        enter = fadeIn(tween(durationMillis = 160)),
        exit = fadeOut(tween(durationMillis = 120))
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(palette.background)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back to story",
                            tint = palette.foreground
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            story.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.foreground,
                            maxLines = 1
                        )
                        Text(
                            chapter?.title ?: "Chapter $chapterNumber",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.foreground.copy(alpha = 0.84f),
                            maxLines = 1
                        )
                    }
                    Text(
                        "${story.progressPercent}%",
                        modifier = Modifier.padding(horizontal = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.foreground
                    )
                    IconButton(onClick = onDisplayOptions, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.Tune, contentDescription = "Display options", tint = palette.foreground)
                    }
                }

                LinearProgressIndicator(
                    progress = { story.progressPercent.coerceIn(0, 100) / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = FableAccent,
                    trackColor = palette.foreground.copy(alpha = 0.12f)
                )
                if (errorMessage != null) {
                    Text(
                        errorMessage,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(palette.background)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onPreviousChapter,
                    enabled = canGoToPreviousChapter,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.foreground),
                    border = BorderStroke(1.dp, palette.foreground.copy(alpha = 0.38f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Previous")
                }
                Button(
                    onClick = if (hasNextChapter) onNextChapter else onFinish,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.foreground,
                        contentColor = palette.background
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (hasNextChapter) "Next chapter" else "Finish")
                }
            }
        }
    }
}
