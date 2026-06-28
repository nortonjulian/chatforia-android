package com.chatforia.android.calls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatforia.android.auth.UserDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import analytics.AnalyticsManager
import analytics.AnalyticsTracker
class AndroidCallManager(
    context: Context,
    private val socketManager: CallRealtimeEvents,
    private val callService: CallBackendService,
    private val videoRepository: VideoCallBackend,
    private val voiceManager: CallAudioClient,
    private val videoManager: CallVideoClient,
    private val ringtonePlayer: CallRingtonePlayer = AudioCallRingtonePlayer(context),
    private val callDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val analytics: AnalyticsTracker = AnalyticsManager
) : ViewModel() {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val _state =
        MutableStateFlow<AndroidCallState>(AndroidCallState.Idle)

    val state: StateFlow<AndroidCallState> = _state

    private var activeCallAnalytics: CallAnalyticsContext? = null

    init {
        observeSockets()
    }

    fun startAudioCall(
        calleeId: Int,
        displayName: String = "Audio Call"
    ) {
        viewModelScope.launch(callDispatcher) {
            try {
                val callId =
                    callService.createAppCall(
                        calleeId = calleeId,
                        video = false
                    )

                val session =
                    CallSession(
                        callId = callId,
                        displayName = displayName,
                        isVideo = false
                    )

                _state.value = AndroidCallState.Connecting(session)

                val token = callService.fetchVoiceToken()

                voiceManager.startCall(
                    accessToken = token.token,
                    to = calleeId.toString(),
                    listener = object : CallAudioClient.Listener {

                        override fun onConnected() {
                            _state.value = AndroidCallState.Active(session)

                            trackCallStarted(
                                session = session,
                                callType = "audio",
                                direction = "outbound"
                            )
                        }

                        override fun onFailed(message: String) {
                            viewModelScope.launch(callDispatcher) {
                                runCatching {
                                    callService.endCall(
                                        callId = callId,
                                        reason = "failed"
                                    )
                                }
                            }

                            val lower = message.lowercase()

                            if (
                                lower.contains("cancel") ||
                                lower.contains("declin") ||
                                lower.contains("disconnect") ||
                                lower.contains("busy") ||
                                lower.contains("no answer")
                            ) {
                                trackCallEnded("remote_ended")
                                _state.value = AndroidCallState.Ended()
                            } else {
                                _state.value = AndroidCallState.Failed(message)
                            }
                        }

                        override fun onDisconnected() {
                            trackCallEnded("disconnected")
                            _state.value = AndroidCallState.Ended()
                        }
                    }
                )

            } catch (e: Exception) {
                _state.value =
                    AndroidCallState.Failed(
                        e.message ?: "Failed to start audio call."
                    )
            }
        }
    }

    fun startPhoneCall(phoneNumber: String) {
        viewModelScope.launch(callDispatcher) {
            try {
                val callId =
                    callService.startExternalCall(phoneNumber)

                val session =
                    CallSession(
                        callId = callId,
                        displayName = phoneNumber,
                        isVideo = false
                    )

                _state.value = AndroidCallState.Connecting(session)

                val token = callService.fetchVoiceToken()

                voiceManager.startCall(
                    accessToken = token.token,
                    to = phoneNumber,
                    listener = object : CallAudioClient.Listener {

                        override fun onConnected() {
                            _state.value = AndroidCallState.Active(session)

                            trackCallStarted(
                                session = session,
                                callType = "phone",
                                direction = "outbound"
                            )
                        }

                        override fun onFailed(message: String) {
                            viewModelScope.launch(callDispatcher) {
                                runCatching {
                                    callService.endCall(
                                        callId = callId,
                                        reason = "failed"
                                    )
                                }
                            }

                            val lower = message.lowercase()

                            if (
                                lower.contains("cancel") ||
                                lower.contains("declin") ||
                                lower.contains("disconnect") ||
                                lower.contains("busy") ||
                                lower.contains("no answer")
                            ) {
                                trackCallEnded("remote_ended")
                                _state.value = AndroidCallState.Ended()
                            } else {
                                _state.value = AndroidCallState.Failed(message)
                            }
                        }

                        override fun onDisconnected() {
                            trackCallEnded("disconnected")
                            _state.value = AndroidCallState.Ended()
                        }
                    }
                )

            } catch (e: Exception) {
                _state.value =
                    AndroidCallState.Failed(
                        e.message ?: "Failed to start phone call."
                    )
            }
        }
    }

    fun startVideoCall(
        currentUser: UserDto,
        calleeId: Int,
        displayName: String = "Video Call",
        chatRoomId: Int? = null
    ) {
        viewModelScope.launch(callDispatcher) {
            try {
                val start =
                    videoRepository.startVideo(
                        calleeId = calleeId,
                        chatRoomId = chatRoomId
                    )

                val session =
                    CallSession(
                        callId = start.callId,
                        roomName = start.roomName,
                        displayName = displayName,
                        isVideo = true,
                        speakerEnabled = true
                    )

                _state.value = AndroidCallState.Connecting(session)

                val token =
                    videoRepository.fetchVideoToken(
                        identity = currentUser.id.toString(),
                        roomName = start.roomName
                    )

                videoManager.connect(
                    accessToken = token.token,
                    roomName = start.roomName,
                    listener = object : CallVideoClient.Listener {
                        override fun onConnected() {
                            _state.value = AndroidCallState.Active(session)

                            trackCallStarted(
                                session = session,
                                callType = "video",
                                direction = "outbound"
                            )
                        }

                        override fun onFailed(message: String) {
                            _state.value = AndroidCallState.Failed(message)
                        }

                        override fun onDisconnected() {
                            trackCallEnded("disconnected")
                            _state.value = AndroidCallState.Ended()
                        }
                    }
                )

            } catch (e: Exception) {
                _state.value =
                    AndroidCallState.Failed(
                        e.message ?: "Failed to start video call."
                    )
            }
        }
    }

    fun restoreIncomingCall(payload: IncomingCallPayload) {
        ringtonePlayer.playSavedRingtone()

        _state.value =
            AndroidCallState.Ringing(payload)
    }


    fun acceptIncoming(currentUser: UserDto) {
        ringtonePlayer.stop()
        val ringing =
            _state.value as? AndroidCallState.Ringing ?: return

        val payload = ringing.payload

        if (payload.mode?.uppercase() == "VIDEO" || payload.roomName != null) {
            acceptIncomingVideo(currentUser, payload)
        } else {
            acceptIncomingAudio(payload)
        }
    }

    private fun acceptIncomingAudio(payload: IncomingCallPayload) {
        val session =
            CallSession(
                callId = payload.callId,
                displayName =
                    payload.callerName
                        ?: payload.fromNumber
                        ?: "Incoming Call",
                isVideo = false
            )

        val accepted =
            voiceManager.acceptCall(
                object : CallAudioClient.Listener {
                    override fun onConnected() {
                        _state.value = AndroidCallState.Active(session)

                        trackCallStarted(
                            session = session,
                            callType = "audio",
                            direction = "inbound"
                        )
                    }

                    override fun onFailed(message: String) {
                        _state.value = AndroidCallState.Failed(message)
                    }

                    override fun onDisconnected() {
                        trackCallEnded("disconnected")
                        _state.value = AndroidCallState.Ended()
                    }
                }
            )

        if (!accepted) {
            _state.value =
                AndroidCallState.Failed(
                    "Incoming audio calls are not available yet on this device."
                )
            return
        }

        _state.value = AndroidCallState.Connecting(session)
    }

    private fun acceptIncomingVideo(
        currentUser: UserDto,
        payload: IncomingCallPayload
    ) {
        viewModelScope.launch(callDispatcher) {
            try {
                val roomName =
                    payload.roomName
                        ?: throw Exception("Missing video room.")

                val token =
                    videoRepository.fetchVideoToken(
                        identity = currentUser.id.toString(),
                        roomName = roomName
                    )

                val session =
                    CallSession(
                        callId = payload.callId,
                        roomName = roomName,
                        displayName = payload.callerName ?: "Video Call",
                        isVideo = true,
                        speakerEnabled = true
                    )

                videoManager.connect(
                    accessToken = token.token,
                    roomName = roomName,
                    listener = object : CallVideoClient.Listener {
                        override fun onConnected() {
                            _state.value = AndroidCallState.Active(session)

                            trackCallStarted(
                                session = session,
                                callType = "video",
                                direction = "inbound"
                            )
                        }

                        override fun onFailed(message: String) {
                            _state.value = AndroidCallState.Failed(message)
                        }

                        override fun onDisconnected() {
                            trackCallEnded("disconnected")
                            _state.value = AndroidCallState.Ended()
                        }
                    }
                )

            } catch (e: Exception) {
                _state.value =
                    AndroidCallState.Failed(
                        e.message ?: "Failed to accept video call."
                    )
            }
        }
    }

    fun declineIncoming() {
        ringtonePlayer.stop()
        val ringing =
            _state.value as? AndroidCallState.Ringing ?: return

        val callId = ringing.payload.callId

        voiceManager.rejectIncomingCall()

        if (callId != null) {
            viewModelScope.launch(callDispatcher) {
                runCatching {
                    callService.endCall(
                        callId = callId,
                        reason = "declined"
                    )
                }
            }
        }

        _state.value = AndroidCallState.Idle
    }

    fun toggleMute() {
        val active =
            _state.value as? AndroidCallState.Active ?: return

        val updated =
            active.session.copy(
                muted = !active.session.muted
            )

        if (updated.isVideo) {
            videoManager.setMuted(updated.muted)
        } else {
            voiceManager.setMuted(updated.muted)
        }

        _state.value = AndroidCallState.Active(updated)
    }

    fun toggleSpeaker() {
        val active =
            _state.value as? AndroidCallState.Active ?: return

        val updated =
            active.session.copy(
                speakerEnabled = !active.session.speakerEnabled
            )

        voiceManager.setSpeaker(updated.speakerEnabled)

        _state.value = AndroidCallState.Active(updated)
    }

    fun toggleCamera() {
        val active =
            _state.value as? AndroidCallState.Active ?: return

        if (!active.session.isVideo) return

        val updated =
            active.session.copy(
                cameraEnabled = !active.session.cameraEnabled
            )

        videoManager.setCameraEnabled(updated.cameraEnabled)

        _state.value = AndroidCallState.Active(updated)
    }

    fun flipCamera() {
        videoManager.flipCamera()
    }

    fun endCall() {
        ringtonePlayer.stop()
        val current = _state.value

        val callId =
            when (current) {
                is AndroidCallState.Active -> current.session.callId
                is AndroidCallState.Connecting -> current.session.callId
                else -> null
            }

        trackCallEnded("hangup")

        voiceManager.endCall()
        videoManager.disconnect()

        if (callId != null) {
            viewModelScope.launch(callDispatcher) {
                runCatching {
                    callService.endCall(
                        callId = callId,
                        reason = "hangup"
                    )
                }
            }
        }

        _state.value = AndroidCallState.Ended()
    }

    fun clearEndedState() {
        _state.value = AndroidCallState.Idle
    }


    private data class CallAnalyticsContext(
        val callType: String,
        val direction: String,
        val startedAtMillis: Long
    )

    private fun trackCallStarted(
        session: CallSession,
        callType: String,
        direction: String
    ) {
        if (activeCallAnalytics != null) return

        activeCallAnalytics =
            CallAnalyticsContext(
                callType = callType,
                direction = direction,
                startedAtMillis = System.currentTimeMillis()
            )

        analytics.capture(
            "call started",
            mapOf(
                "call_type" to callType,
                "direction" to direction
            )
        )
    }

    private fun trackCallEnded(reason: String) {
        val current = activeCallAnalytics ?: return

        val durationSec =
            ((System.currentTimeMillis() - current.startedAtMillis) / 1000)
                .toInt()
                .coerceAtLeast(0)

        analytics.capture(
            "call ended",
            mapOf(
                "call_type" to current.callType,
                "direction" to current.direction,
                "ended_reason" to reason,
                "duration_bucket" to durationBucket(durationSec)
            )
        )

        activeCallAnalytics = null
    }

    private fun durationBucket(durationSec: Int): String {
        return when {
            durationSec < 10 -> "0-9s"
            durationSec < 60 -> "10-59s"
            durationSec < 300 -> "1-5m"
            durationSec < 900 -> "5-15m"
            else -> "15m+"
        }
    }
    private fun observeSockets() {
        viewModelScope.launch {
            socketManager.incomingCalls.collect { raw ->
                val payload =
                    runCatching {
                        json.decodeFromString<IncomingCallPayload>(raw)
                    }.getOrNull()

                if (payload != null) {
                    ringtonePlayer.playSavedRingtone()
                    _state.value = AndroidCallState.Ringing(payload)
                }
            }
        }

        viewModelScope.launch {
            TwilioVoiceCallEvents.remoteEnded.collect {
                ringtonePlayer.stop()
                voiceManager.endCall()
                videoManager.disconnect()
                trackCallEnded("remote_ended")
                _state.value = AndroidCallState.Ended()
            }
        }

        viewModelScope.launch {
            socketManager.incomingVideoCalls.collect { raw ->
                val payload =
                    runCatching {
                        json.decodeFromString<IncomingCallPayload>(raw)
                    }.getOrNull()

                if (payload != null) {
                    ringtonePlayer.playSavedRingtone()

                    _state.value =
                        AndroidCallState.Ringing(
                            payload.copy(mode = payload.mode ?: "VIDEO")
                        )
                }
            }
        }

        viewModelScope.launch {
            socketManager.callEnded.collect {
                ringtonePlayer.stop()
                voiceManager.endCall()
                videoManager.disconnect()
                trackCallEnded("remote_ended")
                _state.value = AndroidCallState.Ended()
            }
        }

        viewModelScope.launch {
            socketManager.videoCallEnded.collect {
                ringtonePlayer.stop()
                videoManager.disconnect()
                trackCallEnded("remote_ended")
                _state.value = AndroidCallState.Ended()
            }
        }
    }
}