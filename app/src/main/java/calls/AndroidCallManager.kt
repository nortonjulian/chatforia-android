package com.chatforia.android.calls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatforia.android.auth.UserDto
import com.chatforia.android.socket.SocketManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class AndroidCallManager(
    private val socketManager: SocketManager,
    private val callService: CallService,
    private val videoRepository: VideoCallRepository,
    private val voiceManager: TwilioVoiceManager,
    private val videoManager: TwilioVideoManager
) : ViewModel() {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val _state =
        MutableStateFlow<AndroidCallState>(AndroidCallState.Idle)

    val state: StateFlow<AndroidCallState> = _state

    init {
        observeSockets()
    }

    fun startAudioCall(
        calleeId: Int,
        displayName: String = "Audio Call"
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val callId =
                    callService.createAppCall(
                        calleeId = calleeId,
                        video = false
                    )

                val token = callService.fetchVoiceToken()

                val session =
                    CallSession(
                        callId = callId,
                        displayName = displayName,
                        isVideo = false
                    )

                _state.value = AndroidCallState.Connecting(session)

                voiceManager.startCall(
                    accessToken = token.token,
                    to = calleeId.toString()
                )

                _state.value = AndroidCallState.Active(session)

            } catch (e: Exception) {
                _state.value =
                    AndroidCallState.Failed(
                        e.message ?: "Failed to start audio call."
                    )
            }
        }
    }

    fun startPhoneCall(phoneNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val callId =
                    callService.startExternalCall(phoneNumber)

                val token = callService.fetchVoiceToken()

                val session =
                    CallSession(
                        callId = callId,
                        displayName = phoneNumber,
                        isVideo = false
                    )

                _state.value = AndroidCallState.Connecting(session)

                voiceManager.startCall(
                    accessToken = token.token,
                    to = phoneNumber
                )

                _state.value = AndroidCallState.Active(session)

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
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val start =
                    videoRepository.startVideo(
                        calleeId = calleeId,
                        chatRoomId = chatRoomId
                    )

                val token =
                    videoRepository.fetchVideoToken(
                        identity = currentUser.id.toString(),
                        roomName = start.roomName
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

                videoManager.connect(
                    accessToken = token.token,
                    roomName = start.roomName
                )

                _state.value = AndroidCallState.Active(session)

            } catch (e: Exception) {
                _state.value =
                    AndroidCallState.Failed(
                        e.message ?: "Failed to start video call."
                    )
            }
        }
    }

    fun acceptIncoming(currentUser: UserDto) {
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

        voiceManager.acceptCall()
        _state.value = AndroidCallState.Active(session)
    }

    private fun acceptIncomingVideo(
        currentUser: UserDto,
        payload: IncomingCallPayload
    ) {
        viewModelScope.launch(Dispatchers.IO) {
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
                    roomName = roomName
                )

                _state.value = AndroidCallState.Active(session)

            } catch (e: Exception) {
                _state.value =
                    AndroidCallState.Failed(
                        e.message ?: "Failed to accept video call."
                    )
            }
        }
    }

    fun declineIncoming() {
        val ringing =
            _state.value as? AndroidCallState.Ringing ?: return

        val callId = ringing.payload.callId

        if (callId != null) {
            viewModelScope.launch(Dispatchers.IO) {
                runCatching {
                    callService.endCall(
                        callId = callId,
                        reason = "DECLINED"
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
        val current = _state.value

        val callId =
            when (current) {
                is AndroidCallState.Active -> current.session.callId
                is AndroidCallState.Connecting -> current.session.callId
                else -> null
            }

        voiceManager.endCall()
        videoManager.disconnect()

        if (callId != null) {
            viewModelScope.launch(Dispatchers.IO) {
                runCatching {
                    callService.endCall(
                        callId = callId,
                        reason = "ENDED"
                    )
                }
            }
        }

        _state.value = AndroidCallState.Ended()
    }

    fun clearEndedState() {
        _state.value = AndroidCallState.Idle
    }

    private fun observeSockets() {
        viewModelScope.launch {
            socketManager.incomingCalls.collect { raw ->
                val payload =
                    runCatching {
                        json.decodeFromString<IncomingCallPayload>(raw)
                    }.getOrNull()

                if (payload != null) {
                    _state.value = AndroidCallState.Ringing(payload)
                }
            }
        }

        viewModelScope.launch {
            socketManager.incomingVideoCalls.collect { raw ->
                val payload =
                    runCatching {
                        json.decodeFromString<IncomingCallPayload>(raw)
                    }.getOrNull()

                if (payload != null) {
                    _state.value =
                        AndroidCallState.Ringing(
                            payload.copy(mode = payload.mode ?: "VIDEO")
                        )
                }
            }
        }

        viewModelScope.launch {
            socketManager.callEnded.collect {
                voiceManager.endCall()
                videoManager.disconnect()
                _state.value = AndroidCallState.Ended()
            }
        }

        viewModelScope.launch {
            socketManager.videoCallEnded.collect {
                videoManager.disconnect()
                _state.value = AndroidCallState.Ended()
            }
        }
    }
}