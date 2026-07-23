package com.chatforia.android.notifications

import android.content.Context

object IncomingCallDisplayStore {
    private const val PREFS_NAME =
        "chatforia_incoming_call_display"

    private const val SAVED_AT_KEY =
        "savedAt"

    private const val MAX_AGE_MS =
        2 * 60 * 1000L

    private val keys =
        listOf(
            "callId",
            "callerId",
            "callerName",
            "fromNumber",
            "mode",
            "roomName"
        )

    fun save(
        context: Context,
        data: Map<String, String>
    ) {
        val editor =
            context
                .getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )
                .edit()
                .clear()
                .putLong(
                    SAVED_AT_KEY,
                    System.currentTimeMillis()
                )

        keys.forEach { key ->
            data[key]
                ?.takeIf { it.isNotBlank() }
                ?.let { value ->
                    editor.putString(key, value)
                }
        }

        editor.apply()
    }

    fun recent(
        context: Context
    ): Map<String, String>? {
        val preferences =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val savedAt =
            preferences.getLong(
                SAVED_AT_KEY,
                0L
            )

        if (
            savedAt == 0L ||
            System.currentTimeMillis() - savedAt > MAX_AGE_MS
        ) {
            clear(context)
            return null
        }

        return keys.mapNotNull { key ->
            preferences
                .getString(key, null)
                ?.takeIf { it.isNotBlank() }
                ?.let { value ->
                    key to value
                }
        }.toMap()
    }

    fun clear(context: Context) {
        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .clear()
            .apply()
    }
}
