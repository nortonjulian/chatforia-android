package com.chatforia.android.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatforia.android.crypto.KeyStorage
import com.chatforia.android.crypto.MessageDecryptor
import com.chatforia.android.socket.SocketManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.UUID
import com.chatforia.android.chats.ConversationDto

class ChatThreadViewModel(
    private val repository: MessagesRepository,
    private val keyStorage: KeyStorage,
    private val messageDecryptor: MessageDecryptor = MessageDecryptor()
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
        socketManager: SocketManager,
        currentUserId: Int
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

                    mergeIncomingMessage(decryptForDisplay(incoming, currentUserId))
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

                    mergeIncomingMessage(decryptForDisplay(incoming, currentUserId))
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

    fun loadMessages(
        roomId: Int,
        currentUserId: Int
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val loaded =
                    repository.loadMessages(roomId)
                        .map { decryptForDisplay(it, currentUserId) }
                        .sortedWith(messageSorter())

                _messages.value = loaded

                repository.markReadBulk(roomId)

                val highestId =
                    loaded
                        .mapNotNull { if (it.id > 0) it.id else null }
                        .maxOrNull()

                if (highestId != null) {
                    val deltas =
                        repository.loadDeltas(roomId, highestId)
                            .map { decryptForDisplay(it, currentUserId) }

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
        conversation: ConversationDto,
        text: String,
        currentUserId: Int? = null,
        currentUsername: String? = null
    ) {
        viewModelScope.launch {
            val trimmed = text.trim()

            if (trimmed.isEmpty()) return@launch

            val clientMessageId = UUID.randomUUID().toString()

            val optimistic =
                MessageDto(
                    id = -kotlin.math.abs(clientMessageId.hashCode()),
                    rawContent = trimmed,
                    content = trimmed,
                    translatedForMe = null,
                    decryptedContent = trimmed,
                    createdAt = java.time.Instant.now().toString(),
                    sender = SenderDto(
                        id = currentUserId ?: 0,
                        username = currentUsername
                    ),
                    chatRoomId = conversation.id,
                    clientMessageId = clientMessageId,
                    optimistic = true,
                    failed = false
                )

            mergeIncomingMessage(optimistic)

            _isSending.value = true
            _error.value = null

            try {
                if (conversation.kind == "sms") {
                    val to =
                        conversation.phone?.trim()?.takeIf { it.isNotBlank() }
                            ?: throw Exception("Missing SMS phone number.")

                    repository.sendSms(
                        to = to,
                        text = trimmed
                    )

                    mergeIncomingMessage(
                        optimistic.copy(
                            optimistic = false,
                            failed = false
                        )
                    )
                } else {
                    val roomId =
                        conversation.id
                            ?: throw Exception("Missing chat room.")

                    val saved =
                        repository.sendMessage(
                            roomId = roomId,
                            text = trimmed,
                            clientMessageId = clientMessageId
                        )

                    if (saved != null) {
                        val display =
                            if (currentUserId != null) {
                                decryptForDisplay(saved, currentUserId)
                            } else {
                                saved
                            }

                        mergeIncomingMessage(display)
                    }
                }

            } catch (e: Exception) {
                markMessageFailed(clientMessageId)
                _error.value = e.message ?: "Failed to send message."
            } finally {
                _isSending.value = false
            }
        }
    }

    private fun decryptForDisplay(
        message: MessageDto,
        currentUserId: Int
    ): MessageDto {
        val privateKey = keyStorage.readPrivateKey()

        println("🔐 hasPrivateKey=${keyStorage.hasPrivateKey()}")

        println(
            "🔐 msg=${message.id} " +
                    "hasCipher=${!message.contentCiphertext.isNullOrBlank()} " +
                    "hasKey=${!message.encryptedKeyForMe.isNullOrBlank() || !message.encryptedKeys.isNullOrEmpty()}"
        )

        val decrypted =
            messageDecryptor.decryptMessageOrNull(
                message = message,
                currentUserPrivateKeyB64 = privateKey,
                currentUserId = currentUserId
            )

        println("🔐 decryptResult=${decrypted?.take(50)}")

        return if (!decrypted.isNullOrBlank()) {
            message.copy(decryptedContent = decrypted)
        } else {
            message
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

    private fun applyDeletedOrExpiredPayload(payloadJson: String, roomId: Int) {
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
                            content = "",
                            decryptedContent = ""
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
        val incomingId = if (incoming.id > 0) incoming.id else null
        val incomingClientId = incoming.clientMessageId

        val current = _messages.value.toMutableList()

        val index =
            current.indexOfFirst { existing ->
                val sameId = incomingId != null && existing.id == incomingId
                val sameClientId =
                    !incomingClientId.isNullOrBlank() &&
                            existing.clientMessageId == incomingClientId

                sameId || sameClientId
            }

        if (index >= 0) {
            val existing = current[index]

            current[index] =
                existing.copy(
                    id = if (incoming.id > 0) incoming.id else existing.id,
                    rawContent = incoming.rawContent ?: existing.rawContent,
                    content = incoming.content ?: existing.content,
                    translatedForMe = incoming.translatedForMe ?: existing.translatedForMe,
                    decryptedContent = incoming.decryptedContent ?: existing.decryptedContent,
                    contentCiphertext = incoming.contentCiphertext ?: existing.contentCiphertext,
                    encryptedKeyForMe = incoming.encryptedKeyForMe ?: existing.encryptedKeyForMe,
                    encryptedKeys = incoming.encryptedKeys ?: existing.encryptedKeys,
                    encryptionVersion = incoming.encryptionVersion ?: existing.encryptionVersion,
                    createdAt = incoming.createdAt.ifBlank { existing.createdAt },
                    expiresAt = incoming.expiresAt ?: existing.expiresAt,
                    editedAt = incoming.editedAt ?: existing.editedAt,
                    deletedAt = incoming.deletedAt ?: existing.deletedAt,
                    deletedForAll = incoming.deletedForAll ?: existing.deletedForAll,
                    deletedBySender = incoming.deletedBySender ?: existing.deletedBySender,
                    revision = incoming.revision ?: existing.revision,
                    sender = incoming.sender,
                    senderId = incoming.senderId ?: existing.senderId,
                    chatRoomId = incoming.chatRoomId ?: existing.chatRoomId,
                    clientMessageId = incoming.clientMessageId ?: existing.clientMessageId,
                    readBy = if (incoming.readBy.isNotEmpty()) incoming.readBy else existing.readBy,
                    attachments = if (incoming.attachments.isNotEmpty()) incoming.attachments else existing.attachments,
                    attachmentsInline = if (incoming.attachmentsInline.isNotEmpty()) incoming.attachmentsInline else existing.attachmentsInline,
                    optimistic = false,
                    failed = false
                )
        } else {
            current.add(incoming)
        }

        _messages.value = current.sortedWith(messageSorter())
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
        return compareBy<MessageDto> { it.createdAt }
            .thenBy { it.id }
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