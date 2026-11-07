package com.example.fable.data.local

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fable.domain.model.ReaderFont
import com.example.fable.domain.model.ReaderLayout
import com.example.fable.domain.model.ReaderLineSpacing
import com.example.fable.domain.model.ReaderPreferences
import com.example.fable.domain.model.ReaderTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedPreferencesReaderPreferencesRepositoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val storage = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE)
    private val repository = SharedPreferencesReaderPreferencesRepository(context)

    @Before
    fun clearPreferences() {
        storage.edit().clear().commit()
    }

    @After
    fun clearPreferencesAfterTest() {
        storage.edit().clear().commit()
    }

    @Test
    fun readReturnsIosDefaultsWhenNothingIsStored() {
        assertEquals(ReaderPreferences(), repository.read())
    }

    @Test
    fun saveAndReadRoundTripAllOptions() {
        val expected = ReaderPreferences(
            font = ReaderFont.ROUNDED,
            fontScalePercent = 135,
            theme = ReaderTheme.OLED,
            lineSpacing = ReaderLineSpacing.SPACIOUS,
            layout = ReaderLayout.PAGINATED
        )

        repository.save(expected)

        assertEquals(expected, repository.read())
    }

    @Test
    fun readFallsBackForUnknownEnumsAndNormalizesFontScale() {
        storage.edit()
            .putString(KEY_FONT, "UNKNOWN")
            .putInt(KEY_FONT_SCALE_PERCENT, 500)
            .putString(KEY_THEME, "UNKNOWN")
            .putString(KEY_LINE_SPACING, "UNKNOWN")
            .putString(KEY_LAYOUT, "UNKNOWN")
            .commit()

        assertEquals(ReaderPreferences(fontScalePercent = 150), repository.read())
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
