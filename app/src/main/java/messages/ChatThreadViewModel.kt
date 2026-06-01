package com.chatforia.android.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.chatforia.android.socket.SocketManager
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class ChatThreadViewModel(
    private val repository: MessagesRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<MessageDto>>(emptyList())
    val messages: StateFlow<List<MessageDto>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun connectRealtime(
        roomId: Int,
        socketManager: SocketManager
    ) {
        socketManager.joinRoom(roomId)

        viewModelScope.launch {
            socketManager.messageUpserts.collect { messageJson ->
                try {
                    val incoming =
                        json.decodeFromString<MessageDto>(messageJson)

                    if (incoming.chatRoomId != null && incoming.chatRoomId != roomId) {
                        return@collect
                    }

                    _messages.value =
                        (_messages.value + incoming)
                            .distinctBy { message ->
                                if (message.id > 0) {
                                    "id-${message.id}"
                                } else {
                                    "local-${message.createdAt}-${message.sender.id}"
                                }
                            }
                            .sortedBy { it.createdAt }

                } catch (e: Exception) {
                    println("❌ Failed to decode realtime message: ${e.message}")
                }
            }
        }
    }

    fun loadMessages(roomId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                _messages.value =
                    repository.loadMessages(roomId)
                        .sortedBy { it.createdAt }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load messages."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendMessage(
        roomId: Int,
        text: String
    ) {
        viewModelScope.launch {
            val trimmed = text.trim()

            if (trimmed.isEmpty()) {
                return@launch
            }

            _isSending.value = true
            _error.value = null

            try {
                repository.sendMessage(
                    roomId = roomId,
                    text = trimmed
                )

                _messages.value =
                    repository.loadMessages(roomId)
                        .sortedBy { it.createdAt }

            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to send message."
            } finally {
                _isSending.value = false
            }
        }
    }
}