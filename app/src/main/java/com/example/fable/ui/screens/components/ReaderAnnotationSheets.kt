package com.example.fable.ui.screens.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fable.domain.model.HighlightColor
import com.example.fable.domain.model.StoryHighlight
import com.example.fable.domain.reading.InBookSearchEngine
import com.example.fable.domain.reading.InBookSearchMatch

@Composable
fun QuoteCardView(
    quoteText: String,
    storyTitle: String,
    storyAuthor: String,
    chapterNumber: Int?,
    note: String?,
    highlightColor: HighlightColor,
    modifier: Modifier = Modifier
) {
    val accent = highlightColor.accentColor()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFCFBF8),
        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.FormatQuote,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "FABLE READS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = Color(0xFF5A6474)
                )
                Spacer(Modifier.weight(1f))
                if (chapterNumber != null) {
                    Surface(
                        color = Color(0x12000000),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            "CHAPTER $chapterNumber",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF5A6474)
                        )
                    }
                }
            }

            Text(
                "“${quoteText.trim()}”",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 17.sp,
                    lineHeight = 25.sp
                ),
                color = Color(0xFF111721)
            )

            val trimmedNote = note?.trim().orEmpty()
            if (trimmedNote.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = accent.copy(alpha = 0.12f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "READER NOTE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accent
                        )
                        Text(
                            trimmedNote,
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                            color = Color(0xFF334155)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFE3E8ED))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        storyTitle,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF111721),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "— $storyAuthor",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF5A6474)
                    )
                }
                Text(
                    "fable",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF3B59F6)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassageHighlightEditorSheet(
    chapterNumber: Int,
    paragraphIndex: Int,
    paragraphText: String,
    existingHighlight: StoryHighlight? = null,
    onSave: (
        chapterNumber: Int,
        paragraphIndex: Int,
        startOffset: Int,
        endOffset: Int,
        selectedText: String,
        note: String,
        color: HighlightColor,
        existingId: String?
    ) -> Unit,
    onDelete: ((String) -> Unit)? = null,
    onOpenQuoteCard: (quoteText: String, note: String, color: HighlightColor, chapterNumber: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = remember(paragraphText) { InBookSearchEngine.tokenizeWords(paragraphText) }
    val lastTokenIndex = (tokens.size - 1).coerceAtLeast(0)
    val initialStartWord = remember(tokens, existingHighlight) {
        if (existingHighlight == null || tokens.isEmpty()) {
            0
        } else {
            tokens.indexOfFirst { it.startOffset >= existingHighlight.startOffset }
                .takeIf { it >= 0 } ?: 0
        }
    }
    val initialEndWord = remember(tokens, existingHighlight, initialStartWord) {
        if (existingHighlight == null || tokens.isEmpty()) {
            lastTokenIndex
        } else {
            tokens.indexOfLast { it.endOffset <= existingHighlight.endOffset }
                .takeIf { it >= initialStartWord } ?: lastTokenIndex
        }
    }

    var startWordIndex by rememberSaveable(paragraphText, existingHighlight?.id) {
        mutableIntStateOf(initialStartWord)
    }
    var endWordIndex by rememberSaveable(paragraphText, existingHighlight?.id) {
        mutableIntStateOf(initialEndWord)
    }
    var selectedColor by rememberSaveable(existingHighlight?.id) {
        mutableStateOf(existingHighlight?.color ?: HighlightColor.TERRACOTTA)
    }
    var note by rememberSaveable(existingHighlight?.id) {
        mutableStateOf(existingHighlight?.note.orEmpty())
    }

    val selection = remember(paragraphText, startWordIndex, endWordIndex) {
        InBookSearchEngine.selectWordRange(paragraphText, startWordIndex, endWordIndex)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (existingHighlight != null) "Edit highlight" else "Highlight passage",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                if (existingHighlight != null && onDelete != null) {
                    IconButton(
                        onClick = {
                            onDelete(existingHighlight.id)
                            onDismiss()
                        }
                    ) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete highlight",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = selectedColor.highlightFillColor(),
                border = BorderStroke(1.dp, selectedColor.accentColor().copy(alpha = 0.45f))
            ) {
                Text(
                    "“${selection.selectedText}”",
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic
                    ),
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (tokens.size > 1) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Passage range (${endWordIndex - startWordIndex + 1} of ${tokens.size} words)",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                startWordIndex = 0
                                endWordIndex = lastTokenIndex
                            }
                        ) {
                            Text("Full paragraph")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RangeStepperCard(
                            label = "Start: ${tokens.getOrNull(startWordIndex)?.word.orEmpty()}",
                            canDecrement = startWordIndex > 0,
                            canIncrement = startWordIndex < endWordIndex,
                            onDecrement = { startWordIndex = (startWordIndex - 1).coerceAtLeast(0) },
                            onIncrement = { startWordIndex = (startWordIndex + 1).coerceAtMost(endWordIndex) },
                            modifier = Modifier.weight(1f)
                        )
                        RangeStepperCard(
                            label = "End: ${tokens.getOrNull(endWordIndex)?.word.orEmpty()}",
                            canDecrement = endWordIndex > startWordIndex,
                            canIncrement = endWordIndex < lastTokenIndex,
                            onDecrement = { endWordIndex = (endWordIndex - 1).coerceAtLeast(startWordIndex) },
                            onIncrement = { endWordIndex = (endWordIndex + 1).coerceAtMost(lastTokenIndex) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Highlight color", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HighlightColor.entries.forEach { color ->
                        FilterChip(
                            selected = selectedColor == color,
                            onClick = { selectedColor = color },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(color.accentColor(), CircleShape)
                                )
                            },
                            label = { Text(color.displayName) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(500) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Marginalia note (optional)") },
                minLines = 2,
                supportingText = {
                    Text(
                        "${note.length} / 500",
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onOpenQuoteCard(selection.selectedText, note, selectedColor, chapterNumber)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Quote card")
                }
                Button(
                    onClick = {
                        onSave(
                            chapterNumber,
                            paragraphIndex,
                            selection.startOffset,
                            selection.endOffset,
                            selection.selectedText,
                            note,
                            selectedColor,
                            existingHighlight?.id
                        )
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save highlight")
                }
            }
        }
    }
}

@Composable
private fun RangeStepperCard(
    label: String,
    canDecrement: Boolean,
    canIncrement: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onDecrement,
                    enabled = canDecrement,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.weight(1f).height(34.dp)
                ) {
                    Text("−")
                }
                OutlinedButton(
                    onClick = onIncrement,
                    enabled = canIncrement,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.weight(1f).height(34.dp)
                ) {
                    Text("+")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderHighlightsSheet(
    highlights: List<StoryHighlight>,
    onSelectHighlightChapter: (Int) -> Unit,
    onEditHighlight: (StoryHighlight) -> Unit,
    onDeleteHighlight: (String) -> Unit,
    onShareQuoteCard: (StoryHighlight) -> Unit,
    onExportMarkdown: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.84f)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Highlights & Notes", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${highlights.size} saved · Long-press any paragraph in the reader to add",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (highlights.isNotEmpty()) {
                    OutlinedButton(onClick = onExportMarkdown) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Export MD")
                    }
                }
            }

            if (highlights.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No highlights yet. Long-press any paragraph while reading to highlight a passage or add a note.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(highlights, key = StoryHighlight::id) { highlight ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectHighlightChapter(highlight.chapterNumber)
                                    onDismiss()
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, highlight.color.accentColor().copy(alpha = 0.45f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(highlight.color.accentColor(), CircleShape)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Chapter ${highlight.chapterNumber} · ${highlight.color.displayName}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { onShareQuoteCard(highlight) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Share,
                                            contentDescription = "Share quote card",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onEditHighlight(highlight) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Edit,
                                            contentDescription = "Edit highlight",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteHighlight(highlight.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.DeleteOutline,
                                            contentDescription = "Delete highlight",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    "“${highlight.selectedText}”",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontStyle = FontStyle.Italic
                                    )
                                )
                                if (highlight.note.isNotBlank()) {
                                    Text(
                                        highlight.note,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteCardShareSheet(
    quoteText: String,
    storyTitle: String,
    storyAuthor: String,
    chapterNumber: Int?,
    note: String?,
    color: HighlightColor,
    onShareImage: () -> Unit,
    onShareText: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Quote Card", style = MaterialTheme.typography.titleLarge)
            QuoteCardView(
                quoteText = quoteText,
                storyTitle = storyTitle,
                storyAuthor = storyAuthor,
                chapterNumber = chapterNumber,
                note = note,
                highlightColor = color
            )
            Button(
                onClick = onShareImage,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Share Quote Card (PNG)")
            }
            OutlinedButton(
                onClick = onShareText,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Share as Plain Text")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSearchSheet(
    query: String,
    matches: List<InBookSearchMatch>,
    onQueryChange: (String) -> Unit,
    onSelectMatch: (InBookSearchMatch) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.84f)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Search in Book", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                        }
                    }
                } else {
                    null
                },
                placeholder = { Text("Find phrase or character...") }
            )

            if (query.isNotBlank()) {
                Text(
                    "${matches.size} ${if (matches.size == 1) "match" else "matches"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (query.isNotBlank() && matches.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No passages matched \"$query\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        items = matches,
                        key = { "${it.chapterNumber}-${it.paragraphIndex}-${it.snippet.hashCode()}" }
                    ) { match ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectMatch(match)
                                    onDismiss()
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "Chapter ${match.chapterNumber} · ${match.chapterTitle}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    match.snippet,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
