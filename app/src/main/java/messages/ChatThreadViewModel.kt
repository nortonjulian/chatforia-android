package com.chatforia.android.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatforia.android.socket.SocketManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
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

    private var activeRealtimeRoomId: Int? = null

    fun connectRealtime(
        roomId: Int,
        socketManager: SocketManager
    ) {
        if (activeRealtimeRoomId == roomId) {
            socketManager.joinRoom(roomId)
            return
        }

        activeRealtimeRoomId = roomId

        socketManager.joinRoom(roomId)

        viewModelScope.launch {
            socketManager.messageUpserts.collect { messageJson ->
                try {
                    val incoming = json.decodeFromString<MessageDto>(messageJson)

                    if (incoming.chatRoomId != null && incoming.chatRoomId != roomId) {
                        return@collect
                    }

                    mergeIncomingMessage(incoming)
                } catch (e: Exception) {
                    println("❌ Failed to decode message:upsert: ${e.message}")
                }
            }
        }

        viewModelScope.launch {
            socketManager.messageAcks.collect { ackJson ->
                try {
                    val ack = json.decodeFromString<MessageAckDto>(ackJson)

                    if (ack.chatRoomId != null && ack.chatRoomId != roomId) {
                        return@collect
                    }

                    applyAck(ack)
                } catch (e: Exception) {
                    println("❌ Failed to decode message:ack: ${e.message}")
                }
            }
        }

        viewModelScope.launch {
            socketManager.messageEdited.collect { messageJson ->
                try {
                    val incoming = json.decodeFromString<MessageDto>(messageJson)

                    if (incoming.chatRoomId != null && incoming.chatRoomId != roomId) {
                        return@collect
                    }

                    mergeIncomingMessage(incoming)
                } catch (e: Exception) {
                    println("❌ Failed to decode message:edited: ${e.message}")
                }
            }
        }

        viewModelScope.launch {
            socketManager.messageDeleted.collect { payloadJson ->
                applyDeletedOrExpiredPayload(payloadJson, roomId)
            }
        }

        viewModelScope.launch {
            socketManager.messageExpired.collect { payloadJson ->
                applyDeletedOrExpiredPayload(payloadJson, roomId)
            }
        }
    }

    fun loadMessages(roomId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val loaded =
                    repository.loadMessages(roomId)
                        .sortedWith(messageSorter())

                _messages.value = loaded

                repository.markReadBulk(roomId)

                val highestId =
                    loaded
                        .mapNotNull { if (it.id > 0) it.id else null }
                        .maxOrNull()

                if (highestId != null) {
                    val deltas = repository.loadDeltas(roomId, highestId)

                    deltas.forEach { incoming ->
                        mergeIncomingMessage(incoming)
                    }
                }

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

            val clientMessageId = UUID.randomUUID().toString()

            val optimistic =
                MessageDto(
                    id = -kotlin.math.abs(clientMessageId.hashCode()),
                    rawContent = trimmed,
                    content = trimmed,
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

    private fun applyAck(ack: MessageAckDto) {
        val clientMessageId = ack.clientMessageId ?: return

        _messages.value =
            _messages.value.map { message ->
                if (message.clientMessageId == clientMessageId) {
                    message.copy(
                        id = ack.id ?: message.id,
                        chatRoomId = ack.chatRoomId ?: message.chatRoomId,
                        createdAt = ack.createdAt ?: message.createdAt,
                        optimistic = false,
                        failed = false
                    )
                } else {
                    message
                }
            }.sortedWith(messageSorter())
    }

    private fun applyDeletedOrExpiredPayload(
        payloadJson: String,
        roomId: Int
    ) {
        try {
            val payload = json.decodeFromString<MessageLifecyclePayload>(payloadJson)

            val payloadRoomId = payload.chatRoomId ?: payload.roomId
            if (payloadRoomId != null && payloadRoomId != roomId) return

            val messageId = payload.id ?: payload.messageId ?: return

            _messages.value =
                _messages.value.map { message ->
                    if (message.id == messageId) {
                        message.copy(
                            deletedForAll = true,
                            deletedAt = payload.deletedAt ?: message.deletedAt,
                            rawContent = "",
                            content = ""
                        )
                    } else {
                        message
                    }
                }

        } catch (e: Exception) {
            println("❌ Failed to decode message lifecycle payload: ${e.message}")
        }
    }

    private fun mergeIncomingMessage(incoming: MessageDto) {
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
            val existing = current[index]

            current[index] =
                existing.copy(
                    id =
                        if (incoming.id > 0) incoming.id else existing.id,
                    rawContent =
                        incoming.rawContent ?: existing.rawContent,
                    content =
                        incoming.content ?: existing.content,
                    translatedForMe =
                        incoming.translatedForMe ?: existing.translatedForMe,
                    decryptedContent =
                        incoming.decryptedContent ?: existing.decryptedContent,
                    contentCiphertext =
                        incoming.contentCiphertext ?: existing.contentCiphertext,
                    encryptedKeyForMe =
                        incoming.encryptedKeyForMe ?: existing.encryptedKeyForMe,
                    encryptedKeys =
                        incoming.encryptedKeys ?: existing.encryptedKeys,
                    encryptionVersion =
                        incoming.encryptionVersion ?: existing.encryptionVersion,
                    createdAt =
                        incoming.createdAt.ifBlank { existing.createdAt },
                    expiresAt =
                        incoming.expiresAt ?: existing.expiresAt,
                    editedAt =
                        incoming.editedAt ?: existing.editedAt,
                    deletedAt =
                        incoming.deletedAt ?: existing.deletedAt,
                    deletedForAll =
                        incoming.deletedForAll ?: existing.deletedForAll,
                    deletedBySender =
                        incoming.deletedBySender ?: existing.deletedBySender,
                    revision =
                        incoming.revision ?: existing.revision,
                    sender = incoming.sender,
                    senderId =
                        incoming.senderId ?: existing.senderId,
                    chatRoomId =
                        incoming.chatRoomId ?: existing.chatRoomId,
                    clientMessageId =
                        incoming.clientMessageId ?: existing.clientMessageId,
                    readBy =
                        if (incoming.readBy.isNotEmpty()) incoming.readBy else existing.readBy,
                    attachments =
                        if (incoming.attachments.isNotEmpty()) incoming.attachments else existing.attachments,
                    attachmentsInline =
                        if (incoming.attachmentsInline.isNotEmpty()) incoming.attachmentsInline else existing.attachmentsInline,
                    optimistic = false,
                    failed = false
                )
        } else {
            current.add(incoming)
        }

        _messages.value =
            current.sortedWith(messageSorter())
    }

    private fun markMessageFailed(clientMessageId: String) {
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

    private fun messageSorter(): Comparator<MessageDto> {
        return compareBy<MessageDto> { message ->
            message.createdAt
        }.thenBy { message ->
            message.id
        }
    }
}

@Serializable
data class MessageAckDto(
    val clientMessageId: String? = null,
    val id: Int? = null,
    val chatRoomId: Int? = null,
    val createdAt: String? = null
)

@Serializable
data class MessageLifecyclePayload(
    val id: Int? = null,
    val messageId: Int? = null,
    val chatRoomId: Int? = null,
    val roomId: Int? = null,
    val deletedAt: String? = null
)