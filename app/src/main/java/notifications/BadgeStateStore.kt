package com.chatforia.android.notifications

import android.content.Context

class BadgeStateStore(
    context: Context
) {
    private val preferences =
        context.applicationContext.getSharedPreferences(
            "chatforia_badge_state",
            Context.MODE_PRIVATE
        )

    fun save(state: BadgeStateDto) {
        preferences.edit()
            .putInt(KEY_UNREAD_CONVERSATIONS, state.unreadConversations)
            .putInt(KEY_MISSED_CALLS, state.missedCalls)
            .putInt(KEY_UNREAD_VOICEMAILS, state.unreadVoicemails)
            .putInt(KEY_TOTAL, state.total.coerceAtLeast(0))
            .apply()
    }

    fun total(): Int {
        return preferences
            .getInt(KEY_TOTAL, 0)
            .coerceAtLeast(0)
    }

    companion object {
        private const val KEY_UNREAD_CONVERSATIONS =
            "unread_conversations"

        private const val KEY_MISSED_CALLS =
            "missed_calls"

        private const val KEY_UNREAD_VOICEMAILS =
            "unread_voicemails"

        private const val KEY_TOTAL =
            "total"
    }
}
