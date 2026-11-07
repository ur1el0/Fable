package com.example.fable.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fable.domain.model.ReaderFont
import com.example.fable.domain.model.ReaderLayout
import com.example.fable.domain.model.ReaderLineSpacing
import com.example.fable.domain.model.ReaderPreferences
import com.example.fable.domain.model.ReaderTheme
import com.example.fable.ui.theme.FableAccent
import com.example.fable.ui.theme.readerPalette
import com.example.fable.ui.theme.scaledFontSize
import com.example.fable.ui.theme.scaledLineHeight
import com.example.fable.ui.theme.toFontFamily
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderDisplayOptionsSheet(
    preferences: ReaderPreferences,
    showLayoutOptions: Boolean,
    onPreferencesChanged: (ReaderPreferences) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember(preferences) { mutableStateOf(preferences) }
    val palette = readerPalette(draft.theme)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Display options", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = {
                        draft = ReaderPreferences()
                        onReset()
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Reset")
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = palette.background,
                contentColor = palette.foreground,
                border = BorderStroke(1.dp, palette.foreground.copy(alpha = 0.14f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Preview", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "The old forest whispered tales of forgotten kings and quiet dawn.",
                        fontFamily = draft.font.toFontFamily(),
                        fontSize = draft.scaledFontSize(15f).sp,
                        lineHeight = draft.scaledLineHeight(25f, 15f).sp
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingLabel("Typography", draft.font.displayLabel())
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReaderFont.values().forEach { font ->
                        FilterChip(
                            selected = draft.font == font,
                            onClick = {
                                draft = draft.copy(font = font)
                                onPreferencesChanged(draft)
                            },
                            label = { Text(font.displayLabel(), fontFamily = font.toFontFamily()) }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SettingLabel("Text size", "${draft.fontScalePercent}%")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("A", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = draft.fontScalePercent.toFloat(),
                        onValueChange = { value ->
                            draft = draft.copy(fontScalePercent = value.roundToInt())
                        },
                        valueRange = ReaderPreferences.MIN_FONT_SCALE_PERCENT.toFloat()..
                            ReaderPreferences.MAX_FONT_SCALE_PERCENT.toFloat(),
                        steps = (ReaderPreferences.MAX_FONT_SCALE_PERCENT -
                            ReaderPreferences.MIN_FONT_SCALE_PERCENT) /
                            ReaderPreferences.FONT_SCALE_STEP_PERCENT - 1,
                        onValueChangeFinished = { onPreferencesChanged(draft) },
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                    )
                    Text("A", style = MaterialTheme.typography.titleLarge)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingLabel("Reading background", draft.theme.displayLabel())
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ReaderTheme.values()) { theme ->
                        val swatch = readerPalette(theme)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = {
                                    draft = draft.copy(theme = theme)
                                    onPreferencesChanged(draft)
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .semantics {
                                        contentDescription = theme.displayLabel()
                                        selected = draft.theme == theme
                                    }
                            ) {
                                Surface(
                                    modifier = Modifier.size(34.dp),
                                    shape = CircleShape,
                                    color = swatch.background,
                                    contentColor = swatch.foreground,
                                    border = BorderStroke(
                                        if (draft.theme == theme) 2.dp else 1.dp,
                                        if (draft.theme == theme) FableAccent else swatch.foreground.copy(alpha = 0.28f)
                                    )
                                ) {
                                    if (draft.theme == theme) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = null,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                            Text(theme.displayLabel(), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingLabel("Line spacing")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReaderLineSpacing.values().forEach { spacing ->
                        FilterChip(
                            selected = draft.lineSpacing == spacing,
                            onClick = {
                                draft = draft.copy(lineSpacing = spacing)
                                onPreferencesChanged(draft)
                            },
                            label = { Text(spacing.displayLabel()) }
                        )
                    }
                }
            }

            if (showLayoutOptions) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingLabel("Reading layout")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReaderLayout.values().forEach { layout ->
                            FilterChip(
                                selected = draft.layout == layout,
                                onClick = {
                                    draft = draft.copy(layout = layout)
                                    onPreferencesChanged(draft)
                                },
                                label = { Text(layout.displayLabel()) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingLabel(title: String, value: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        if (value != null) {
            Text(
                value,
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun ReaderFont.displayLabel() = when (this) {
    ReaderFont.SERIF -> "Serif"
    ReaderFont.SANS -> "Sans"
    ReaderFont.ROUNDED -> "Rounded"
    ReaderFont.MONO -> "Mono"
}

private fun ReaderTheme.displayLabel() = when (this) {
    ReaderTheme.WHITE -> "White"
    ReaderTheme.PAPER -> "Paper"
    ReaderTheme.SEPIA -> "Sepia"
    ReaderTheme.CHARCOAL -> "Charcoal"
    ReaderTheme.MIDNIGHT -> "Midnight"
    ReaderTheme.OLED -> "OLED"
}

private fun ReaderLineSpacing.displayLabel() = when (this) {
    ReaderLineSpacing.COMPACT -> "Compact"
    ReaderLineSpacing.NORMAL -> "Normal"
    ReaderLineSpacing.SPACIOUS -> "Spacious"
}

private fun ReaderLayout.displayLabel() = when (this) {
    ReaderLayout.SCROLL -> "Scroll"
    ReaderLayout.PAGINATED -> "Paginated"
}

