package com.chatforia.android.notifications

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.chatforia.android.calls.IncomingCallPayload
import com.chatforia.android.calls.IncomingCallSheet
import com.chatforia.android.ui.theme.ChatforiaTheme

/** Shows only the ringing controls; MainActivity opens when the call is answered. */
class IncomingCallActivity : ComponentActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var payload by mutableStateOf<IncomingCallPayload?>(null)
    private var shownAt = 0L

    private val checkCall = object : Runnable {
        override fun run() {
            if (!isCurrentCall() || SystemClock.elapsedRealtime() - shownAt >= 40_000L) {
                finish()
            } else {
                handler.postDelayed(this, 500L)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        loadCall(intent)
        if (payload == null) {
            finish()
            return
        }

        setContent {
            ChatforiaTheme {
                payload?.let { current ->
                    IncomingCallSheet(
                        payload = current,
                        onAccept = ::accept,
                        onDecline = ::decline
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        loadCall(intent)
        if (payload == null) finish()
    }

    override fun onStart() {
        super.onStart()
        handler.removeCallbacks(checkCall)
        handler.post(checkCall)
    }

    override fun onStop() {
        handler.removeCallbacks(checkCall)
        super.onStop()
    }

    private fun loadCall(intent: Intent) {
        val data = IncomingCallDisplayStore.recent(applicationContext)
        if (data == null || data["callId"] != intent.getStringExtra("callId")) {
            payload = null
            return
        }

        payload = IncomingCallPayload(
            callId = data["callId"]?.toIntOrNull(),
            callerId = data["callerId"]?.toIntOrNull(),
            callerName = data["callerName"],
            fromNumber = data["fromNumber"],
            mode = data["mode"],
            roomName = data["roomName"]
        )
        shownAt = SystemClock.elapsedRealtime()
    }

    private fun isCurrentCall(): Boolean {
        val current = payload ?: return false
        val data = IncomingCallDisplayStore.recent(applicationContext) ?: return false
        return data["callId"]?.toIntOrNull() == current.callId &&
            data["roomName"] == current.roomName &&
            data["fromNumber"] == current.fromNumber
    }

    private fun accept() {
        if (!isCurrentCall()) {
            finish()
            return
        }

        val data = IncomingCallDisplayStore.recent(applicationContext) ?: return
        startActivity(
            NotificationCoordinator(this).incomingCallIntent(data, callAction = "answer")
        )
        finish()
    }

    private fun decline() {
        if (!isCurrentCall()) {
            finish()
            return
        }

        val current = payload ?: return
        sendBroadcast(
            Intent(this, IncomingCallActionReceiver::class.java).apply {
                action = IncomingCallActionReceiver.ACTION_DECLINE_CALL
                putExtra("callId", current.callId?.toString())
                putExtra("mode", current.mode)
                putExtra("roomName", current.roomName)
            }
        )
        finish()
    }
}
