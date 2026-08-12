package com.chatforia.android.calls

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/*
 * Process-level bridge between FCM and AndroidCallManager.
 *
 * StateFlow intentionally retains one pending payload so an incoming
 * call is not lost when FCM arrives before AndroidCallManager starts
 * collecting. The matching terminal event clears stale payloads.
 */
object IncomingCallPushEvents {
    private val _pendingIncomingCall =
        MutableStateFlow<IncomingCallPayload?>(null)

    val pendingIncomingCall:
        StateFlow<IncomingCallPayload?> =
        _pendingIncomingCall.asStateFlow()

    @Synchronized
    fun notifyIncoming(payload: IncomingCallPayload) {
        _pendingIncomingCall.value = payload
    }

    @Synchronized
    fun consume(payload: IncomingCallPayload) {
        if (_pendingIncomingCall.value == payload) {
            _pendingIncomingCall.value = null
        }
    }

    @Synchronized
    fun clear(callId: Int?) {
        val pending =
            _pendingIncomingCall.value ?: return

        if (
            callId == null ||
            pending.callId == callId
        ) {
            _pendingIncomingCall.value = null
        }
    }
}
