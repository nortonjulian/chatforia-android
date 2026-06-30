package com.chatforia.android.calls

import com.twilio.video.LocalVideoTrack
import com.twilio.video.RemoteVideoTrack

data class CallSession(
    val callId: Int? = null,
    val roomName: String? = null,
    val displayName: String = "Call",
    val isVideo: Boolean = false,
    val startedAtMillis: Long = System.currentTimeMillis(),
    val muted: Boolean = false,
    val speakerEnabled: Boolean = false,
    val cameraEnabled: Boolean = true,
    val localVideoTrack: LocalVideoTrack? = null,
    val remoteVideoTrack: RemoteVideoTrack? = null
)

sealed class AndroidCallState {
    data object Idle : AndroidCallState()
    data class Ringing(val payload: IncomingCallPayload) : AndroidCallState()
    data class Connecting(val session: CallSession) : AndroidCallState()
    data class Active(val session: CallSession) : AndroidCallState()
    data class Ended(val reason: String? = null) : AndroidCallState()
    data class Failed(val message: String) : AndroidCallState()
}