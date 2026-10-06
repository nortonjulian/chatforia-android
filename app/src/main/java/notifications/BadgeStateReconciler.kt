package com.chatforia.android.notifications

import android.content.Context
import android.util.Log
import com.chatforia.android.auth.TokenStorage
import com.chatforia.android.network.ApiClient

object BadgeStateReconciler {

    fun refresh(context: Context): BadgeStateDto? {
        val appContext = context.applicationContext
        val tokenStorage = TokenStorage(appContext)

        if (tokenStorage.read().isNullOrBlank()) {
            return null
        }

        return try {
            val state =
                BadgeStateRepository(
                    ApiClient(tokenStorage)
                ).fetchBadgeState()

            BadgeStateStore(appContext).save(state)

            Log.d(
                "ChatforiaBadge",
                "Badge state refreshed: " +
                    "conversations=${state.unreadConversations}, " +
                    "missedCalls=${state.missedCalls}, " +
                    "voicemails=${state.unreadVoicemails}, " +
                    "total=${state.total}"
            )

            state
        } catch (error: Exception) {
            Log.w(
                "ChatforiaBadge",
                "Unable to refresh badge state",
                error
            )

            null
        }
    }
}
