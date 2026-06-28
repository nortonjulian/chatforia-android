package com.chatforia.android.calls

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object TwilioVoiceCallEvents {
    private val _remoteEnded =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val remoteEnded: SharedFlow<Unit> =
        _remoteEnded.asSharedFlow()

    fun notifyRemoteEnded() {
        _remoteEnded.tryEmit(Unit)
    }
}