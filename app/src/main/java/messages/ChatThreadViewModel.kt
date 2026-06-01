package com.chatforia.android.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatforia.android.socket.SocketManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.UUID

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

                    mergeIncomingMessage(incoming)

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
                        .sortedWith(messageSorter())

            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load messages."

            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendMessage(
        roomId: Int,
        text: String,
        currentUserId: Int? = null,
        currentUsername: String? = null
    ) {
        viewModelScope.launch {
            val trimmed = text.trim()

            if (trimmed.isEmpty()) {
                return@launch
            }

            val clientMessageId =
                UUID.randomUUID().toString()

            val optimistic =
                MessageDto(
                    id = -kotlin.math.abs(clientMessageId.hashCode()),
                    rawContent = trimmed,
                    translatedForMe = null,
                    createdAt = java.time.Instant.now().toString(),
                    sender = SenderDto(
                        id = currentUserId ?: 0,
                        username = currentUsername
                    ),
                    chatRoomId = roomId,
                    clientMessageId = clientMessageId,
                    optimistic = true,
                    failed = false
                )

            mergeIncomingMessage(optimistic)

            _isSending.value = true
            _error.value = null

            try {
                val saved =
                    repository.sendMessage(
                        roomId = roomId,
                        text = trimmed,
                        clientMessageId = clientMessageId
                    )

                if (saved != null) {
                    mergeIncomingMessage(saved)
                }

            } catch (e: Exception) {
                markMessageFailed(clientMessageId)
                _error.value = e.message ?: "Failed to send message."

            } finally {
                _isSending.value = false
            }
        }
    }

    private fun mergeIncomingMessage(
        incoming: MessageDto
    ) {
        val incomingId =
            if (incoming.id > 0) incoming.id else null

        val incomingClientId =
            incoming.clientMessageId

        val current =
            _messages.value.toMutableList()

        val index =
            current.indexOfFirst { existing ->
                val sameId =
                    incomingId != null &&
                            existing.id == incomingId

                val sameClientId =
                    !incomingClientId.isNullOrBlank() &&
                            existing.clientMessageId == incomingClientId

                sameId || sameClientId
            }

        if (index >= 0) {
            current[index] =
                current[index].copy(
                    id =
                        if (incoming.id > 0) incoming.id else current[index].id,
                    rawContent =
                        incoming.rawContent ?: current[index].rawContent,
                    translatedForMe =
                        incoming.translatedForMe ?: current[index].translatedForMe,
                    createdAt =
                        incoming.createdAt.ifBlank { current[index].createdAt },
                    sender = incoming.sender,
                    chatRoomId =
                        incoming.chatRoomId ?: current[index].chatRoomId,
                    clientMessageId =
                        incoming.clientMessageId ?: current[index].clientMessageId,
                    optimistic = false,
                    failed = false
                )
        } else {
            current.add(incoming)
        }

        _messages.value =
            current.sortedWith(messageSorter())
    }

    private fun markMessageFailed(
        clientMessageId: String
    ) {
        _messages.value =
            _messages.value.map { message ->
                if (message.clientMessageId == clientMessageId) {
                    message.copy(
                        optimistic = false,
                        failed = true
                    )
                } else {
                    message
                }
            }
    }

    private fun messageSorter():
            Comparator<MessageDto> {
        return compareBy<MessageDto> { message ->
            message.createdAt
        }.thenBy { message ->
            message.id
        }
    }
}