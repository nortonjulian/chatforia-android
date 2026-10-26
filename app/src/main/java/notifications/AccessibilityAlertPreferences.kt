package com.chatforia.android.notifications

import android.content.Context

/**
 * Account-scoped local mirror of server-saved Accessibility preferences.
 * FCM and incoming call Activity operate outside the authenticated Compose UI.
 */
object AccessibilityAlertPreferences {
    private const val NAME = "chatforia_accessibility_alerts"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_VISUAL = "visual"
    private const val KEY_VIBRATE = "vibrate"
    private const val KEY_FLASH = "flash"

    fun save(context: Context, userId: Int, visual: Boolean, vibrate: Boolean, flash: Boolean) {
        context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_USER_ID, userId)
            .putBoolean(KEY_VISUAL, visual)
            .putBoolean(KEY_VIBRATE, vibrate)
            .putBoolean(KEY_FLASH, flash)
            .apply()
    }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    // A setting has no authority once the associated user signs out.
    private fun hasAccount(context: Context) = prefs(context).getInt(KEY_USER_ID, -1) > 0

    fun visual(context: Context): Boolean =
        hasAccount(context) && prefs(context).getBoolean(KEY_VISUAL, false)

    fun vibrate(context: Context): Boolean =
        hasAccount(context) && prefs(context).getBoolean(KEY_VIBRATE, false)

    fun flash(context: Context): Boolean =
        hasAccount(context) && prefs(context).getBoolean(KEY_FLASH, false)
}
