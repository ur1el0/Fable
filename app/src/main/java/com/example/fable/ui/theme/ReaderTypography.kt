package com.example.fable.ui.theme

import android.graphics.Typeface
import androidx.compose.ui.text.font.FontFamily
import com.example.fable.domain.model.ReaderFont
import com.example.fable.domain.model.ReaderLineSpacing
import com.example.fable.domain.model.ReaderPreferences

fun ReaderFont.toFontFamily(): FontFamily = when (this) {
    ReaderFont.SERIF -> FontFamily.Serif
    ReaderFont.SANS -> FontFamily.SansSerif
    ReaderFont.ROUNDED -> FontFamily(Typeface.create("sans-serif-rounded", Typeface.NORMAL))
    ReaderFont.MONO -> FontFamily.Monospace
}

fun ReaderPreferences.scaledFontSize(baseSize: Float): Float =
    baseSize * fontScalePercent / 100f

fun ReaderPreferences.scaledLineHeight(baseLineHeight: Float, baseFontSize: Float): Float {
    val scale = fontScalePercent / 100f
    val spacingAdjustment = when (lineSpacing) {
        ReaderLineSpacing.COMPACT -> -4f
        ReaderLineSpacing.NORMAL -> 0f
        ReaderLineSpacing.SPACIOUS -> 6f
    }
    return (baseLineHeight * scale + spacingAdjustment).coerceAtLeast(baseFontSize * scale * 1.2f)
}
