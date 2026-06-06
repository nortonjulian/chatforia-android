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

data class VideoCallUiState(
    val callId: Int? = null,
    val roomName: String? = null,
    val token: String? = null,
    val incomingVideoCall: IncomingCallPayload? = null,
    val isConnecting: Boolean = false,
    val isMuted: Boolean = false,
    val isCameraEnabled: Boolean = true,
    val error: String? = null
)

class VideoCallViewModel(
    private val repository: VideoCallRepository,
    private val socketManager: SocketManager
) : ViewModel() {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val _state = MutableStateFlow(VideoCallUiState())
    val state: StateFlow<VideoCallUiState> = _state

    init {
        observeVideoSocketEvents()
    }

    fun startVideoCall(
        currentUser: UserDto,
        calleeId: Int,
        chatRoomId: Int? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value =
                _state.value.copy(isConnecting = true, error = null)

            try {
                val start = repository.startVideo(calleeId, chatRoomId)

                val token =
                    repository.fetchVideoToken(
                        identity = currentUser.id.toString(),
                        roomName = start.roomName
                    )

                _state.value =
                    _state.value.copy(
                        callId = start.callId,
                        roomName = start.roomName,
                        token = token.token,
                        isConnecting = false
                    )
            } catch (e: Exception) {
                _state.value =
                    _state.value.copy(
                        isConnecting = false,
                        error = e.message ?: "Failed to start video call."
                    )
            }
        }
    }

    fun acceptIncomingVideoCall(currentUser: UserDto) {
        val incoming = _state.value.incomingVideoCall ?: return
        val roomName = incoming.roomName ?: return

        viewModelScope.launch(Dispatchers.IO) {
            _state.value =
                _state.value.copy(isConnecting = true, error = null)

            try {
                val token =
                    repository.fetchVideoToken(
                        identity = currentUser.id.toString(),
                        roomName = roomName
                    )

                _state.value =
                    _state.value.copy(
                        callId = incoming.callId,
                        roomName = roomName,
                        token = token.token,
                        incomingVideoCall = null,
                        isConnecting = false
                    )
            } catch (e: Exception) {
                _state.value =
                    _state.value.copy(
                        isConnecting = false,
                        error = e.message
                    )
            }
        }
    }

    fun toggleMute() {
        _state.value =
            _state.value.copy(isMuted = !_state.value.isMuted)
    }

    fun toggleCamera() {
        _state.value =
            _state.value.copy(isCameraEnabled = !_state.value.isCameraEnabled)
    }

    fun endVideoCall() {
        _state.value = VideoCallUiState()
    }

    private fun observeVideoSocketEvents() {
        viewModelScope.launch {
            socketManager.incomingVideoCalls.collect { raw ->
                val payload =
                    runCatching {
                        json.decodeFromString<IncomingCallPayload>(raw)
                    }.getOrNull()

                _state.value =
                    _state.value.copy(incomingVideoCall = payload)
            }
        }

        viewModelScope.launch {
            socketManager.videoCallEnded.collect {
                _state.value = VideoCallUiState()
            }
        }
    }
}