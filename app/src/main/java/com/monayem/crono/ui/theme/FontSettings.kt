package com.monayem.crono.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FontSizeSettings(
    val titleLarge: Float = 22f,
    val headlineSmall: Float = 20f,
    val titleMedium: Float = 16f,
    val bodyLarge: Float = 16f,
    val bodyMedium: Float = 14f,
    val bodySmall: Float = 12f,
    val labelLarge: Float = 14f,
    val labelSmall: Float = 11f
)

class FontSettingsManager private constructor(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences("crono_font_settings", Context.MODE_PRIVATE)

    private val _fontSettings = MutableStateFlow(readAllSettings())
    val fontSettings: StateFlow<FontSizeSettings> = _fontSettings.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        _fontSettings.value = readAllSettings()
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun saveFontSize(key: String, size: Float) {
        prefs.edit().putFloat(key, size).apply()
    }

    private fun getFontSize(key: String, default: Float): Float {
        return prefs.getFloat(key, default)
    }

    private fun readAllSettings(): FontSizeSettings {
        return FontSizeSettings(
            titleLarge = getFontSize("titleLarge", 22f),
            headlineSmall = getFontSize("headlineSmall", 20f),
            titleMedium = getFontSize("titleMedium", 16f),
            bodyLarge = getFontSize("bodyLarge", 16f),
            bodyMedium = getFontSize("bodyMedium", 14f),
            bodySmall = getFontSize("bodySmall", 12f),
            labelLarge = getFontSize("labelLarge", 14f),
            labelSmall = getFontSize("labelSmall", 11f)
        )
    }

    fun getAllSettings(): FontSizeSettings = _fontSettings.value

    companion object {
        @Volatile
        private var INSTANCE: FontSettingsManager? = null

        fun getInstance(context: Context): FontSettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FontSettingsManager(context).also { INSTANCE = it }
            }
        }
    }
}

val LocalFontSizes = staticCompositionLocalOf { FontSizeSettings() }
