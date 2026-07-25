package com.chatforia.android.notifications

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object IncomingCallActionEvents {
    private val _declinedCallIds =
        MutableSharedFlow<Int?>(
            extraBufferCapacity = 4
        )

    val declinedCallIds =
        _declinedCallIds.asSharedFlow()

    fun notifyDeclined(callId: Int?) {
        _declinedCallIds.tryEmit(callId)
    }
}
