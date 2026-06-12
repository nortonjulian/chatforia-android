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
    private val queueStorage: MessageQueueStorage,
    private val messageDecryptor: MessageDecryptor = MessageDecryptor()
) : ViewModel() {

    private val messageStore = MessageStore()

    private val messageQueueManager =
        MessageQueueManager(
            repository = repository,
            messageStore = messageStore,
            queueStorage = queueStorage,
            scope = viewModelScope
        )

    val messages: StateFlow<List<MessageDto>>
        get() = messageStore.messages

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
    private var realtimeCollectorsStarted = false

    fun connectRealtime(
        roomId: Int,
        socketManager: SocketManager,
        currentUserId: Int
    ) {
        activeRealtimeRoomId = roomId
        socketManager.joinRoom(roomId)

        if (realtimeCollectorsStarted) return
        realtimeCollectorsStarted = true

        viewModelScope.launch {
            socketManager.socketConnected.collect {
                val activeRoomId = activeRealtimeRoomId ?: return@collect

                recoverMissingMessages(
                    roomId = activeRoomId,
                    currentUserId = currentUserId
                )
            }
        }

        viewModelScope.launch {
            socketManager.messageAcks.collect { ackJson ->
                try {
                    val ack = json.decodeFromString<MessageAckDto>(ackJson)

                    if (ack.chatRoomId != null && ack.chatRoomId != activeRealtimeRoomId) {
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

                    if (incoming.chatRoomId != null && incoming.chatRoomId != activeRealtimeRoomId) {
                        return@collect
                    }

                    messageStore.upsert(
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

                    if (incoming.chatRoomId != null && incoming.chatRoomId != activeRealtimeRoomId) {
                        return@collect
                    }

                    messageStore.upsert(
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
                val activeRoomId = activeRealtimeRoomId ?: return@collect

                applyDeletedOrExpiredPayload(
                    payloadJson = payloadJson,
                    roomId = activeRoomId
                )
            }
        }

        viewModelScope.launch {
            socketManager.messageExpired.collect { payloadJson ->
                val activeRoomId = activeRealtimeRoomId ?: return@collect

                applyDeletedOrExpiredPayload(
                    payloadJson = payloadJson,
                    roomId = activeRoomId
                )
            }
        }

        viewModelScope.launch {
            socketManager.messageReads.collect { payloadJson ->
                applyMessageReadPayload(payloadJson)
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

                messageStore.replaceAll(loaded)

                repository.markReadBulk(
                    ids = loaded
                        .filter { it.id > 0 }
                        .filter { it.sender.id != currentUserId }
                        .map { it.id }
                )

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
                        messageStore.upsert(incoming)
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
        mediaUrls: List<String>,
        text: String = "",
        currentUserId: Int? = null,
        currentUsername: String? = null
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
                mediaUrls = mediaUrls,
                text = text,
                currentUserId = currentUserId,
                currentUsername = currentUsername
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

    fun deleteMessage(
        message: MessageDto,
        deleteForEveryone: Boolean
    ) {
        viewModelScope.launch {
            try {
                repository.deleteMessage(
                    messageId = message.id,
                    deleteForEveryone = deleteForEveryone
                )

                if (deleteForEveryone) {
                    messageStore.markDeleted(
                        messageId = message.id,
                        deletedAt = Instant.now().toString()
                    )
                } else {
                    messageStore.remove(message.id)
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to delete message."
            }
        }
    }

    fun editMessage(
        message: MessageDto,
        text: String,
        gifUrl: String? = null
    ) {
        viewModelScope.launch {
            val trimmed = text.trim()

            val existingAttachments =
                message.attachments.ifEmpty { message.attachmentsInline }

            val updatedAttachments =
                if (!gifUrl.isNullOrBlank()) {
                    listOf(
                        AttachmentDto(
                            kind = "GIF",
                            url = gifUrl,
                            mimeType = "image/gif"
                        )
                    )
                } else {
                    existingAttachments
                }

            val updated = repository.editMessage(
                messageId = message.id,
                text = trimmed,
                attachments = updatedAttachments
            )

            val displayUpdated =
                updated?.copy(
                    rawContent = trimmed,
                    content = trimmed,
                    decryptedContent = trimmed,
                    attachments = updated.attachments.ifEmpty { updatedAttachments },
                    attachmentsInline = updated.attachmentsInline.ifEmpty { updatedAttachments },
                    editedAt = updated.editedAt ?: Instant.now().toString()
                ) ?: message.copy(
                    rawContent = trimmed,
                    content = trimmed,
                    decryptedContent = trimmed,
                    attachments = updatedAttachments,
                    attachmentsInline = updatedAttachments,
                    editedAt = Instant.now().toString()
                )

            messageStore.upsert(displayUpdated)
        }
    }

    fun reportMessage(
        message: MessageDto,
        reason: String,
        details: String,
        contextCount: Int,
        blockAfterReport: Boolean
    ) {
        viewModelScope.launch {
            try {
                repository.reportMessage(
                    messageId = message.id,
                    reason = reason,
                    details = details,
                    contextCount = contextCount,
                    blockAfterReport = blockAfterReport
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to submit report."
            }
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
        mediaUrls: List<String>,
        text: String = "",
        currentUserId: Int?,
        currentUsername: String?
    ) {
        val roomId = conversation.id
            ?: throw Exception("Missing chat room.")

        val clientMessageId = UUID.randomUUID().toString()

        val captionText = text.trim().takeIf { it.isNotBlank() }

        val attachments =
            mediaUrls.map { url ->
                AttachmentDto(
                    kind = inferAttachmentKind(url),
                    url = url,
                    mimeType = inferMimeType(url),
                    caption = captionText
                )
            }

        val optimistic = MessageDto(
            id = -abs(clientMessageId.hashCode()),
            rawContent = text.ifBlank { null },
            content = text.ifBlank { null },
            translatedForMe = null,
            decryptedContent = text.ifBlank { null },
            createdAt = Instant.now().toString(),
            sender = SenderDto(
                id = currentUserId ?: 0,
                username = currentUsername
            ),
            chatRoomId = roomId,
            clientMessageId = clientMessageId,
            attachmentsInline = attachments,
            optimistic = true,
            failed = false
        )


        messageStore.upsert(optimistic)

        messageQueueManager.enqueue(
            QueuedMessageJob(
                clientMessageId = clientMessageId,
                roomId = roomId,
                text = captionText,
                attachmentsInline = attachments
            )
        )

        _isSending.value = false
    }

    private suspend fun sendChatMessage(
        conversation: ConversationDto,
        text: String,
        currentUserId: Int?,
        currentUsername: String?
    ) {
        val roomId = conversation.id
            ?: throw Exception("Missing chat room.")

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
            chatRoomId = roomId,
            clientMessageId = clientMessageId,
            optimistic = true,
            failed = false
        )

        messageStore.upsert(optimistic)

        messageQueueManager.enqueue(
            QueuedMessageJob(
                clientMessageId = clientMessageId,
                roomId = roomId,
                text = text
            )
        )

        _isSending.value = false
    }

    private fun applyMessageReadPayload(payloadJson: String) {
        try {
            val payload = json.decodeFromString<MessageReadPayload>(payloadJson)

            val messageId = payload.messageId ?: return
            val reader = payload.reader ?: return

            messageStore.addReadBy(
                messageId = messageId,
                reader = reader
            )
        } catch (e: Exception) {
            println("❌ Failed to decode message_read: ${e.message}")
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

        messageStore.upsertAck(
            id = ack.id,
            clientMessageId = clientMessageId,
            chatRoomId = ack.chatRoomId,
            createdAt = ack.createdAt
        )
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

            messageStore.markDeleted(
                messageId = messageId,
                deletedAt = payload.deletedAt
            )
        } catch (e: Exception) {
            println("❌ Failed to decode message lifecycle payload: ${e.message}")
        }
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

    private suspend fun recoverMissingMessages(
        roomId: Int,
        currentUserId: Int
    ) {
        val highestId = messages.value
            .mapNotNull { message ->
                if (message.id > 0) message.id else null
            }
            .maxOrNull()
            ?: return

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
            messageStore.upsert(incoming)
        }

        if (deltas.isNotEmpty()) {
            repository.markReadBulk(
                ids = deltas
                    .filter { it.id > 0 }
                    .filter { it.sender.id != currentUserId }
                    .map { it.id }
            )
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

@Serializable
data class MessageReadPayload(
    val messageId: Int? = null,
    val reader: SenderDto? = null,
    val readAt: String? = null
)