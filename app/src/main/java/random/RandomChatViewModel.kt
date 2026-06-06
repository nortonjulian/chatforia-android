package com.chatforia.android.random

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatforia.android.socket.SocketManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class RandomChatViewModel(
    private val socketManager: SocketManager
) : ViewModel() {

    private val _state = MutableStateFlow(RandomChatUiState())
    val state: StateFlow<RandomChatUiState> = _state

    init {
        observeSocket()
    }

    fun startSearch() {
        _state.value = RandomChatUiState(isSearching = true)
        socketManager.startRandomChat()
    }

    fun cancelSearch() {
        socketManager.cancelRandomChat()
        _state.value = RandomChatUiState()
    }

    private fun observeSocket() {
        viewModelScope.launch {
            socketManager.randomChatWaiting.collect {
                _state.value = _state.value.copy(
                    isSearching = true,
                    error = null
                )
            }
        }

        viewModelScope.launch {
            socketManager.randomChatMatched.collect { raw ->
                try {
                    val json = JSONObject(raw)

                    val session = RandomSession(
                        roomId = json.optInt("roomId"),
                        myAlias = json.optString("myAlias", "You"),
                        partnerAlias = json.optString("partnerAlias", "Someone")
                    )

                    _state.value = RandomChatUiState(
                        isSearching = false,
                        session = session
                    )
                } catch (e: Exception) {
                    _state.value = RandomChatUiState(
                        isSearching = false,
                        error = e.message ?: "Could not start random chat."
                    )
                }
            }
        }

        viewModelScope.launch {
            socketManager.randomChatError.collect { raw ->
                val message =
                    try {
                        JSONObject(raw).optString(
                            "message",
                            "Could not find a random chat."
                        )
                    } catch (_: Exception) {
                        raw
                    }

                _state.value = RandomChatUiState(
                    isSearching = false,
                    error = message
                )
            }
        }
    }
}