package com.chatforia.android.calls

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class TerminalCallPushEvent(
    val callId: Int?,
    val status: String?,
    val reason: String?,
    val mode: String?
)

object CallLifecyclePushEvents {
    private val _terminalEvents =
        MutableSharedFlow<TerminalCallPushEvent>(
            extraBufferCapacity = 16
        )

    val terminalEvents: SharedFlow<TerminalCallPushEvent> =
        _terminalEvents.asSharedFlow()

    private val _historyRefreshEvents =
        MutableSharedFlow<Unit>(
            extraBufferCapacity = 16
        )

    val historyRefreshEvents: SharedFlow<Unit> =
        _historyRefreshEvents.asSharedFlow()

    fun notifyTerminal(
        callId: Int?,
        status: String?,
        reason: String?,
        mode: String?
    ) {
        _terminalEvents.tryEmit(
            TerminalCallPushEvent(
                callId = callId,
                status = status,
                reason = reason,
                mode = mode
            )
        )
    }

    fun notifyHistoryRefresh() {
        _historyRefreshEvents.tryEmit(Unit)
    }
}
