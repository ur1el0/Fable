package com.example.fable.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.fable.domain.model.ReaderFont
import com.example.fable.domain.model.ReaderLayout
import com.example.fable.domain.model.ReaderLineSpacing
import com.example.fable.domain.model.ReaderPreferences
import com.example.fable.domain.model.ReaderTheme
import com.example.fable.domain.repository.ReaderPreferencesRepository

class SharedPreferencesReaderPreferencesRepository(context: Context) : ReaderPreferencesRepository {
    private val preferences = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE)

    override fun read(): ReaderPreferences {
        val defaults = ReaderPreferences()
        return ReaderPreferences(
            font = preferences.readEnum(KEY_FONT, defaults.font),
            fontScalePercent = normalizeFontScale(
                preferences.getInt(KEY_FONT_SCALE_PERCENT, defaults.fontScalePercent)
            ),
            theme = preferences.readEnum(KEY_THEME, defaults.theme),
            lineSpacing = preferences.readEnum(KEY_LINE_SPACING, defaults.lineSpacing),
            layout = preferences.readEnum(KEY_LAYOUT, defaults.layout)
        )
    }

    override fun save(preferences: ReaderPreferences) {
        this.preferences.edit()
            .putString(KEY_FONT, preferences.font.name)
            .putInt(KEY_FONT_SCALE_PERCENT, preferences.fontScalePercent)
            .putString(KEY_THEME, preferences.theme.name)
            .putString(KEY_LINE_SPACING, preferences.lineSpacing.name)
            .putString(KEY_LAYOUT, preferences.layout.name)
            .apply()
    }

    private inline fun <reified T : Enum<T>> SharedPreferences.readEnum(
        key: String,
        default: T
    ): T = getString(key, null)?.let { stored ->
        runCatching { enumValueOf<T>(stored) }.getOrNull()
    } ?: default

    private fun normalizeFontScale(value: Int): Int {
        val boundedValue = value.coerceIn(
            ReaderPreferences.MIN_FONT_SCALE_PERCENT,
            ReaderPreferences.MAX_FONT_SCALE_PERCENT
        )
        return ((boundedValue + ReaderPreferences.FONT_SCALE_STEP_PERCENT / 2) /
            ReaderPreferences.FONT_SCALE_STEP_PERCENT * ReaderPreferences.FONT_SCALE_STEP_PERCENT)
            .coerceIn(ReaderPreferences.MIN_FONT_SCALE_PERCENT, ReaderPreferences.MAX_FONT_SCALE_PERCENT)
    }

    private companion object {
        const val PREFERENCES_FILE = "fable_reader_preferences"
        const val KEY_FONT = "font"
        const val KEY_FONT_SCALE_PERCENT = "font_scale_percent"
        const val KEY_THEME = "theme"
        const val KEY_LINE_SPACING = "line_spacing"
        const val KEY_LAYOUT = "layout"
    }
}
