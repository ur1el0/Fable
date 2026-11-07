package com.example.fable.domain.model

data class ReaderPreferences(
    val font: ReaderFont = ReaderFont.SERIF,
    val fontScalePercent: Int = 100,
    val theme: ReaderTheme = ReaderTheme.SEPIA,
    val lineSpacing: ReaderLineSpacing = ReaderLineSpacing.NORMAL,
    val layout: ReaderLayout = ReaderLayout.SCROLL
) {
    init {
        require(fontScalePercent in MIN_FONT_SCALE_PERCENT..MAX_FONT_SCALE_PERCENT)
        require(fontScalePercent % FONT_SCALE_STEP_PERCENT == 0)
    }

    companion object {
        const val MIN_FONT_SCALE_PERCENT = 80
        const val MAX_FONT_SCALE_PERCENT = 150
        const val FONT_SCALE_STEP_PERCENT = 5
    }
}

enum class ReaderFont {
    SERIF,
    SANS,
    ROUNDED,
    MONO
}

enum class ReaderTheme {
    WHITE,
    PAPER,
    SEPIA,
    CHARCOAL,
    MIDNIGHT,
    OLED
}

enum class ReaderLineSpacing {
    COMPACT,
    NORMAL,
    SPACIOUS
}

enum class ReaderLayout {
    SCROLL,
    PAGINATED
}
