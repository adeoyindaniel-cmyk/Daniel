package com.example.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

object ThemeManager {
    private const val PREFS_NAME = "gemini_chat_theme_prefs"
    private const val KEY_THEME_MODE = "key_theme_mode"
    private const val KEY_DYNAMIC_COLOR = "key_dynamic_color"

    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    var dynamicColorEnabled by mutableStateOf(true)
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val modeName = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        themeMode = try {
            ThemeMode.valueOf(modeName ?: ThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
        dynamicColorEnabled = prefs.getBoolean(KEY_DYNAMIC_COLOR, true)
    }

    fun setTheme(context: Context, mode: ThemeMode) {
        themeMode = mode
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, mode.name)
            .apply()
    }

    fun toggleDynamicColor(context: Context, enabled: Boolean) {
        dynamicColorEnabled = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DYNAMIC_COLOR, enabled)
            .apply()
    }
}
