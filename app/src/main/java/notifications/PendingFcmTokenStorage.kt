package com.chatforia.android.notifications

import android.content.Context

interface PendingFcmTokenStore {

    fun save(token: String)

    fun read(): String?

    fun clearIfMatches(token: String)
}

class PendingFcmTokenStorage(
    context: Context
) : PendingFcmTokenStore {

    private val preferences =
        context.applicationContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    override fun save(token: String) {
        val normalized =
            token.trim()

        if (normalized.isBlank()) {
            return
        }

        preferences
            .edit()
            .putString(
                KEY_TOKEN,
                normalized
            )
            .apply()
    }

    override fun read(): String? {
        return preferences
            .getString(
                KEY_TOKEN,
                null
            )
            ?.trim()
            ?.takeIf {
                it.isNotBlank()
            }
    }

    override fun clearIfMatches(
        token: String
    ) {
        val normalized =
            token.trim()

        if (
            normalized.isBlank() ||
            read() != normalized
        ) {
            return
        }

        preferences
            .edit()
            .remove(KEY_TOKEN)
            .apply()
    }

    private companion object {
        const val PREFS_NAME =
            "chatforia_push_reconciliation"

        const val KEY_TOKEN =
            "pending_fcm_token"
    }
}
