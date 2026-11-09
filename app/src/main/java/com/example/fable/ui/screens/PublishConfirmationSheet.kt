package com.example.fable.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fable.domain.model.Story
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishConfirmationSheet(
    story: Story,
    onDismiss: () -> Unit,
    onViewStory: () -> Unit,
    onReturnToLibrary: () -> Unit,
    onShare: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val wordCount = story.content.split(Regex("\\s+")).count(String::isNotEmpty)

    fun hideThen(action: () -> Unit) {
        scope.launch {
            sheetState.hide()
            action()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Story published", style = MaterialTheme.typography.headlineSmall)
            Text(
                "${story.title} is now available in the Community Library.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Public  ·  ${story.genre}  ·  $wordCount words",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = { hideThen(onViewStory) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View story now")
            }
            TextButton(
                onClick = { hideThen(onReturnToLibrary) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Return to library")
            }
            TextButton(
                onClick = onShare,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Share story details")
            }
        }
    }
}
