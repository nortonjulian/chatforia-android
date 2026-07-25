package com.chatforia.android.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.chatforia.android.auth.TokenStorage
import com.chatforia.android.calls.CallService
import com.chatforia.android.calls.TwilioIncomingCallStore
import com.chatforia.android.calls.TwilioVoiceManager
import com.chatforia.android.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IncomingCallActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_DECLINE_CALL =
            "com.chatforia.android.action.DECLINE_CALL"
    }

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        if (intent.action != ACTION_DECLINE_CALL) {
            return
        }

        val appContext = context.applicationContext
        val callId =
            intent
                .getStringExtra("callId")
                ?.toIntOrNull()

        val isVideo =
            intent
                .getStringExtra("mode")
                ?.equals("VIDEO", ignoreCase = true) == true ||
                    !intent
                        .getStringExtra("roomName")
                        .isNullOrBlank()

        NotificationCoordinator(appContext)
            .cancelIncomingCallNotification()

        IncomingCallDisplayStore.clear(appContext)
        IncomingCallActionEvents.notifyDeclined(callId)

        val pendingResult = goAsync()

        CoroutineScope(
            SupervisorJob() + Dispatchers.IO
        ).launch {
            try {
                if (callId != null) {
                    runCatching {
                        CallService(
                            ApiClient(
                                TokenStorage(appContext)
                            )
                        ).endCall(
                            callId = callId,
                            reason = "declined"
                        )
                    }.onFailure { error ->
                        Log.e(
                            "ChatforiaNotifications",
                            "Failed to persist notification decline",
                            error
                        )
                    }
                }

                withContext(Dispatchers.Main.immediate) {
                    if (isVideo) {
                        TwilioIncomingCallStore.clear()
                    } else {
                        TwilioVoiceManager(appContext)
                            .rejectIncomingCall()
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
