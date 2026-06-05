package com.chatforia.android.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatforia.android.chats.ConversationDto
import com.chatforia.android.crypto.KeyStorage
import com.chatforia.android.crypto.MessageDecryptor
import com.chatforia.android.socket.SocketManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID
import kotlin.math.abs

class ChatThreadViewModel(
    private val repository: MessagesRepository,
    private val keyStorage: KeyStorage,
    private val messageDecryptor: MessageDecryptor = MessageDecryptor()
) : ViewModel() {

    private val _messages = MutableStateFlow<List<MessageDto>>(emptyList())
    val messages: StateFlow<List<MessageDto>> = _messages

    private val _smsMessages = MutableStateFlow<List<SmsMessageDto>>(emptyList())
    val smsMessages: StateFlow<List<SmsMessageDto>> = _smsMessages

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
    private var smsRealtimeConnected = false

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
            socketManager.messageUpserts.collect { messageJson ->
                try {
                    val incoming = json.decodeFromString<MessageDto>(messageJson)

                    if (incoming.chatRoomId != null && incoming.chatRoomId != roomId) {
                        return@collect
                    }

                    mergeIncomingMessage(
                        decryptForDisplay(
                            message = incoming,
                            currentUserId = currentUserId
                        )
                    )
                } catch (e: Exception) {
                    println("❌ Failed to decode message:new: ${e.message}")
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

                    mergeIncomingMessage(
                        decryptForDisplay(
                            message = incoming,
                            currentUserId = currentUserId
                        )
                    )
                } catch (e: Exception) {
                    println("❌ Failed to decode message:edited: ${e.message}")
                }
            }
        }

        viewModelScope.launch {
            socketManager.messageDeleted.collect { payloadJson ->
                applyDeletedOrExpiredPayload(
                    payloadJson = payloadJson,
                    roomId = roomId
                )
            }
        }

        viewModelScope.launch {
            socketManager.messageExpired.collect { payloadJson ->
                applyDeletedOrExpiredPayload(
                    payloadJson = payloadJson,
                    roomId = roomId
                )
            }
        }
    }

    fun connectSmsRealtime(socketManager: SocketManager) {
        if (smsRealtimeConnected) return

        smsRealtimeConnected = true

        viewModelScope.launch {
            socketManager.smsMessages.collect { smsJson ->
                try {
                    val incoming = json.decodeFromString<SmsMessageDto>(smsJson)
                    mergeIncomingSms(incoming)
                } catch (e: Exception) {
                    println("❌ Failed to decode sms:message:new ${e.message}")
                }
            }
        }
    }

    fun loadConversation(
        conversation: ConversationDto,
        currentUserId: Int
    ) {
        val id = conversation.id ?: return

        if (conversation.kind == "sms") {
            loadSmsThread(id)
        } else {
            loadMessages(
                roomId = id,
                currentUserId = currentUserId
            )
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
                val loaded = repository
                    .loadMessages(roomId)
                    .map { message ->
                        decryptForDisplay(
                            message = message,
                            currentUserId = currentUserId
                        )
                    }
                    .sortedWith(messageSorter())

                _messages.value = loaded

                repository.markReadBulk(roomId)

                val highestId = loaded
                    .mapNotNull { message ->
                        if (message.id > 0) message.id else null
                    }
                    .maxOrNull()

                if (highestId != null) {
                    val deltas = repository
                        .loadDeltas(
                            roomId = roomId,
                            sinceId = highestId
                        )
                        .map { message ->
                            decryptForDisplay(
                                message = message,
                                currentUserId = currentUserId
                            )
                        }

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

    fun loadSmsThread(threadId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val thread = repository.loadSmsThread(threadId)

                _smsMessages.value = thread.sortedMessages
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load SMS thread."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendMedia(
        conversation: ConversationDto,
        mediaUrls: List<String>
    ) {
        viewModelScope.launch {
            if (mediaUrls.isEmpty()) return@launch

            _isSending.value = true
            _error.value = null

            if (conversation.kind == "sms") {
                sendSmsMedia(
                    conversation = conversation,
                    mediaUrls = mediaUrls
                )
                return@launch
            }

            sendChatMedia(
                conversation = conversation,
                mediaUrls = mediaUrls
            )
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

            _isSending.value = true
            _error.value = null

            if (conversation.kind == "sms") {
                sendSmsMessage(
                    conversation = conversation,
                    text = trimmed
                )
                return@launch
            }

            sendChatMessage(
                conversation = conversation,
                text = trimmed,
                currentUserId = currentUserId,
                currentUsername = currentUsername
            )
        }
    }

    private suspend fun sendSmsMessage(
        conversation: ConversationDto,
        text: String
    ) {
        try {
            val to = conversation.phone
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: throw Exception("Missing SMS phone number.")

            val optimisticSms = SmsMessageDto.optimisticOutgoing(
                threadId = conversation.id ?: -1,
                to = to,
                body = text
            )

            mergeIncomingSms(optimisticSms)

            repository.sendSms(
                to = to,
                body = text,
                mediaUrls = emptyList()
            )

            mergeIncomingSms(
                optimisticSms.copy(
                    optimistic = false,
                    failed = false
                )
            )
        } catch (e: Exception) {
            _smsMessages.value = _smsMessages.value.map { message ->
                if (message.optimistic) {
                    message.copy(
                        optimistic = false,
                        failed = true
                    )
                } else {
                    message
                }
            }

            _error.value = e.message ?: "Failed to send SMS."
        } finally {
            _isSending.value = false
        }
    }

    private suspend fun sendSmsMedia(
        conversation: ConversationDto,
        mediaUrls: List<String>
    ) {
        try {

            val to =
                conversation.phone
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: throw Exception("Missing SMS phone number.")

            val optimistic =
                SmsMessageDto.optimisticOutgoing(
                    threadId = conversation.id ?: -1,
                    to = to,
                    body = null,
                    mediaUrls = mediaUrls
                )

            mergeIncomingSms(
                optimistic
            )

            repository.sendSms(
                to = to,
                body = null,
                mediaUrls = mediaUrls
            )

        } catch (e: Exception) {

            _error.value =
                e.message ?: "Failed to send media."

        } finally {

            _isSending.value = false
        }
    }

    private suspend fun sendChatMedia(
        conversation: ConversationDto,
        mediaUrls: List<String>
    ) {
        val clientMessageId = UUID.randomUUID().toString()

        val attachments =
            mediaUrls.map { url ->
                AttachmentDto(
                    kind = inferAttachmentKind(url),
                    url = url,
                    mimeType = inferMimeType(url)
                )
            }

        val optimistic = MessageDto(
            id = -abs(clientMessageId.hashCode()),
            rawContent = "",
            content = "",
            translatedForMe = null,
            decryptedContent = "",
            createdAt = Instant.now().toString(),
            sender = SenderDto(id = 0, username = null),
            chatRoomId = conversation.id,
            clientMessageId = clientMessageId,
            attachmentsInline = attachments,
            optimistic = true,
            failed = false
        )

        mergeIncomingMessage(optimistic)

        try {
            val roomId = conversation.id
                ?: throw Exception("Missing chat room.")

            val saved = repository.sendMessage(
                roomId = roomId,
                text = "",
                clientMessageId = clientMessageId,
                attachmentsInline = attachments
            )

            if (saved != null) {
                mergeIncomingMessage(saved)
            }
        } catch (e: Exception) {
            markMessageFailed(clientMessageId)
            _error.value = e.message ?: "Failed to send media."
        } finally {
            _isSending.value = false
        }
    }

    private suspend fun sendChatMessage(
        conversation: ConversationDto,
        text: String,
        currentUserId: Int?,
        currentUsername: String?
    ) {
        val clientMessageId = UUID.randomUUID().toString()

        val optimistic = MessageDto(
            id = -abs(clientMessageId.hashCode()),
            rawContent = text,
            content = text,
            translatedForMe = null,
            decryptedContent = text,
            createdAt = Instant.now().toString(),
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

        try {
            val roomId = conversation.id
                ?: throw Exception("Missing chat room.")

            val saved = repository.sendMessage(
                roomId = roomId,
                text = text,
                clientMessageId = clientMessageId
            )

            if (saved != null) {
                val display = if (currentUserId != null) {
                    decryptForDisplay(
                        message = saved,
                        currentUserId = currentUserId
                    )
                } else {
                    saved
                }

                mergeIncomingMessage(display)
            }
        } catch (e: Exception) {
            markMessageFailed(clientMessageId)
            _error.value = e.message ?: "Failed to send message."
        } finally {
            _isSending.value = false
        }
    }

    private fun decryptForDisplay(
        message: MessageDto,
        currentUserId: Int
    ): MessageDto {
        val privateKey = keyStorage.readPrivateKey()

        val decrypted = messageDecryptor.decryptMessageOrNull(
            message = message,
            currentUserPrivateKeyB64 = privateKey,
            currentUserId = currentUserId
        )

        return if (!decrypted.isNullOrBlank()) {
            message.copy(decryptedContent = decrypted)
        } else {
            message
        }
    }

    private fun applyAck(ack: MessageAckDto) {
        val clientMessageId = ack.clientMessageId ?: return

        _messages.value = _messages.value
            .map { message ->
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
            }
            .sortedWith(messageSorter())
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

            _messages.value = _messages.value.map { message ->
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
        val incomingId = incoming.id.takeIf { it > 0 }
        val incomingClientId = incoming.clientMessageId

        val current = _messages.value.toMutableList()

        val index = current.indexOfFirst { existing ->
            val sameId = incomingId != null && existing.id == incomingId

            val sameClientId =
                !incomingClientId.isNullOrBlank() &&
                        existing.clientMessageId == incomingClientId

            sameId || sameClientId
        }

        if (index >= 0) {
            val existing = current[index]

            current[index] = existing.copy(
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
                attachments = if (incoming.attachments.isNotEmpty()) {
                    incoming.attachments
                } else {
                    existing.attachments
                },
                attachmentsInline = if (incoming.attachmentsInline.isNotEmpty()) {
                    incoming.attachmentsInline
                } else {
                    existing.attachmentsInline
                },
                optimistic = false,
                failed = false
            )
        } else {
            current.add(incoming)
        }

        _messages.value = current.sortedWith(messageSorter())
    }

    private fun mergeIncomingSms(incoming: SmsMessageDto) {
        val incomingId = incoming.id.takeIf { it > 0 }

        val current = _smsMessages.value.toMutableList()

        val index = current.indexOfFirst { existing ->
            incomingId != null && existing.id == incomingId
        }

        if (index >= 0) {
            val existing = current[index]

            current[index] = existing.copy(
                id = if (incoming.id > 0) incoming.id else existing.id,
                threadId = incoming.threadId ?: existing.threadId,
                direction = incoming.direction,
                fromNumber = incoming.fromNumber ?: existing.fromNumber,
                toNumber = incoming.toNumber ?: existing.toNumber,
                body = incoming.body ?: existing.body,
                provider = incoming.provider ?: existing.provider,
                providerMessageId = incoming.providerMessageId ?: existing.providerMessageId,
                media = if (incoming.media.isNotEmpty()) incoming.media else existing.media,
                createdAt = incoming.createdAt.ifBlank { existing.createdAt },
                editedAt = incoming.editedAt ?: existing.editedAt,
                optimistic = false,
                failed = false
            )
        } else {
            current.add(incoming)
        }

        _smsMessages.value = current.sortedWith(smsMessageSorter())
    }

    private fun inferAttachmentKind(url: String): String {
        val lower = url.lowercase()

        return when {
            lower.contains(".gif") -> "GIF"
            lower.contains(".mp4") ||
                    lower.contains(".mov") ||
                    lower.contains(".webm") -> "VIDEO"
            lower.contains(".mp3") ||
                    lower.contains(".m4a") ||
                    lower.contains(".wav") ||
                    lower.contains(".aac") -> "AUDIO"
            else -> "IMAGE"
        }
    }

    private fun inferMimeType(url: String): String? {
        val lower = url.lowercase()

        return when {
            lower.contains(".gif") -> "image/gif"
            lower.contains(".jpg") || lower.contains(".jpeg") -> "image/jpeg"
            lower.contains(".png") -> "image/png"
            lower.contains(".webp") -> "image/webp"
            lower.contains(".mp4") -> "video/mp4"
            lower.contains(".mov") -> "video/quicktime"
            lower.contains(".webm") -> "video/webm"
            lower.contains(".mp3") -> "audio/mpeg"
            lower.contains(".m4a") -> "audio/m4a"
            lower.contains(".wav") -> "audio/wav"
            lower.contains(".aac") -> "audio/aac"
            else -> null
        }
    }

    private fun markMessageFailed(clientMessageId: String) {
        _messages.value = _messages.value.map { message ->
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

    private fun smsMessageSorter(): Comparator<SmsMessageDto> {
        return compareBy<SmsMessageDto> { it.createdAt }
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