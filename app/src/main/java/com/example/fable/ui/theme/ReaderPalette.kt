package com.example.fable.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.fable.domain.model.ReaderTheme

data class ReaderPalette(
    val background: Color,
    val foreground: Color
)

fun readerPalette(theme: ReaderTheme): ReaderPalette = when (theme) {
    ReaderTheme.WHITE -> ReaderPalette(Color(0xFFFFFFFF), Color(0xFF292929))
    ReaderTheme.PAPER -> ReaderPalette(Color(0xFFF9F7F0), Color(0xFF35332F))
    ReaderTheme.SEPIA -> ReaderPalette(Color(0xFFEEE1CD), Color(0xFF423729))
    ReaderTheme.CHARCOAL -> ReaderPalette(Color(0xFF292929), Color(0xFFF0EEE9))
    ReaderTheme.MIDNIGHT -> ReaderPalette(Color(0xFF101820), Color(0xFFE6EAF0))
    ReaderTheme.OLED -> ReaderPalette(Color(0xFF000000), Color(0xFFF2F2F2))
}
