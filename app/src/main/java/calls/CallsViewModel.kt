package com.chatforia.android.calls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatforia.android.socket.SocketManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

data class CallsUiState(
    val calls: List<CallDto> = emptyList(),
    val incomingCall: IncomingCallPayload? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class CallsViewModel(
    private val callHistoryRepository: CallHistoryRepository,
    private val callService: CallService,
    private val socketManager: SocketManager
) : ViewModel() {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val _state = MutableStateFlow(CallsUiState())
    val state: StateFlow<CallsUiState> = _state

    init {
        observeSocketEvents()
    }

    fun loadCalls() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                val calls = callHistoryRepository.fetchCalls()

                _state.value =
                    _state.value.copy(
                        calls = calls,
                        isLoading = false
                    )
            } catch (e: Exception) {
                _state.value =
                    _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load calls."
                    )
            }
        }
    }

    fun startAudioCall(calleeId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                callService.createAppCall(calleeId = calleeId, video = false)
                loadCalls()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun startPhoneCall(phoneNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                callService.startExternalCall(phoneNumber)
                loadCalls()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun endCall(callId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                callService.endCall(callId)
                loadCalls()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun dismissIncomingCall() {
        _state.value = _state.value.copy(incomingCall = null)
    }

    private fun observeSocketEvents() {
        viewModelScope.launch {
            socketManager.incomingCalls.collect { raw ->
                val payload =
                    runCatching {
                        json.decodeFromString<IncomingCallPayload>(raw)
                    }.getOrNull()

                _state.value =
                    _state.value.copy(incomingCall = payload)
            }
        }

        viewModelScope.launch {
            socketManager.callEnded.collect {
                _state.value = _state.value.copy(incomingCall = null)
                loadCalls()
            }
        }
    }
}