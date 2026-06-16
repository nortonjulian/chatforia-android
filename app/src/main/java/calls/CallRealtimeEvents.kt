package com.chatforia.android.calls

import kotlinx.coroutines.flow.SharedFlow

interface CallRealtimeEvents {
    val incomingCalls: SharedFlow<String>
    val callEnded: SharedFlow<String>
    val incomingVideoCalls: SharedFlow<String>
    val videoCallEnded: SharedFlow<String>
}