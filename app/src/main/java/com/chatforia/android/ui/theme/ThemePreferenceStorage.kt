package com.chatforia.android.ui.theme

import android.content.Context

class ThemePreferenceStorage(context: Context) {

    private val prefs =
        context.getSharedPreferences(
            "chatforia_theme_preferences",
            Context.MODE_PRIVATE
        )

    fun readTheme(): String {
        return prefs.getString(
            "chatforia.theme",
            "dawn"
        ) ?: "dawn"
    }

    fun saveTheme(theme: String) {
        prefs.edit()
            .putString("chatforia.theme", theme)
            .apply()
    }
}