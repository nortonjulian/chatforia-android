package com.chatforia.android.messages

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant

class MessageStore {
    companion object {
        private val locallyRemovedMessageIds = mutableSetOf<Int>()
    }
    private val _messages = MutableStateFlow<List<MessageDto>>(emptyList())
    private val expiredMessageIds = mutableSetOf<Int>()
    val messages: StateFlow<List<MessageDto>> = _messages

    private val _deliveryStates =
        MutableStateFlow<Map<String, MessageDeliveryState>>(emptyMap())

    val deliveryStates: StateFlow<Map<String, MessageDeliveryState>> =
        _deliveryStates

    fun replaceAll(messages: List<MessageDto>) {
        _messages.value = activeMessages(messages)
            .filterNot { message ->
                message.id > 0 &&
                        locallyRemovedMessageIds.contains(message.id)
            }
            .dedupeAndSort()
    }

    fun replaceRoomMessages(
        roomId: Int,
        messages: List<MessageDto>
    ) {
        _messages.update { current ->
            val loaded = activeMessages(messages).filterNot { message ->
                message.id > 0 &&
                    locallyRemovedMessageIds.contains(message.id)
            }

            val serverIds = loaded
                .mapNotNull { it.id.takeIf { id -> id > 0 } }
                .toSet()

            val clientIds = loaded
                .mapNotNull { it.clientMessageId }
                .toSet()

            val retained = current.filter { message ->
                if (message.chatRoomId != roomId) {
                    false
                } else {
                    val clientId = message.clientMessageId
                    val matchesLoaded =
                        (message.id > 0 && message.id in serverIds) ||
                            (!clientId.isNullOrBlank() && clientId in clientIds)

                    val pending =
                        message.optimistic && message.id <= 0

                    val locallyConfirmed =
                        !clientId.isNullOrBlank() &&
                            _deliveryStates.value[clientId] ==
                                MessageDeliveryState.SENT

                    val locallyRemoved =
                        message.id > 0 &&
                            locallyRemovedMessageIds.contains(message.id)

                    !matchesLoaded &&
                        !locallyRemoved &&
                        (pending || locallyConfirmed)
                }
            }

            activeMessages(loaded + retained).dedupeAndSort()
        }
    }

    fun upsert(message: MessageDto) {
        if (message.isExpired()) {
            if (message.id > 0) expiredMessageIds.add(message.id)
            _messages.update { rows -> rows.filterNot { it.id == message.id } }
            return
        }
        if (
            message.id > 0 &&
            (locallyRemovedMessageIds.contains(message.id) || expiredMessageIds.contains(message.id))
        ) {
            return
        }

        _messages.update { current ->
            current
                .upsertMessage(message)
                .dedupeAndSort()
        }
    }

    fun upsertMany(messages: List<MessageDto>) {
        val filtered =
            activeMessages(messages).filterNot { message ->
                message.id > 0 &&
                        locallyRemovedMessageIds.contains(message.id)
            }

        if (filtered.isEmpty()) return

        _messages.update { current ->
            var next = current.filterNot { it.id > 0 && expiredMessageIds.contains(it.id) }

            filtered.forEach { message ->
                next = next.upsertMessage(message)
            }

            next.dedupeAndSort()
        }
    }

    fun markDeleted(messageId: Int, deletedAt: String? = null) {
        _messages.update { current ->
            current.map { message ->
                if (message.id == messageId) {
                    message.copy(
                        deletedForAll = true,
                        deletedAt = deletedAt ?: message.deletedAt,
                        rawContent = "",
                        content = "",
                        decryptedContent = "",
                        attachments = emptyList(),
                        attachmentsInline = emptyList()
                    )
                } else {
                    message
                }
            }
        }
    }

    fun remove(messageId: Int) {
        if (messageId > 0) {
            locallyRemovedMessageIds.add(messageId)
        }

        _messages.update { current ->
            current.filterNot { message ->
                message.id == messageId
            }
        }
    }

    fun expire(messageId: Int) {
        if (messageId > 0) expiredMessageIds.add(messageId)
        _messages.update { rows -> rows.filterNot { it.id == messageId } }
    }

    fun pruneExpired(now: Instant = Instant.now()) {
        _messages.update { rows -> activeMessages(rows, now) }
    }

    private fun activeMessages(
        rows: List<MessageDto>,
        now: Instant = Instant.now()
    ): List<MessageDto> = rows.filterNot { message ->
        val expired = message.isExpired(now)
        if (expired && message.id > 0) expiredMessageIds.add(message.id)
        expired || (message.id > 0 && expiredMessageIds.contains(message.id))
    }

    fun upsertAck(
        id: Int?,
        clientMessageId: String,
        chatRoomId: Int?,
        createdAt: String?
    ) {
        _messages.update { current ->
            current.map { message ->
                if (message.clientMessageId == clientMessageId) {
                    message.copy(
                        id = id ?: message.id,
                        chatRoomId = chatRoomId ?: message.chatRoomId,
                        createdAt = createdAt ?: message.createdAt,
                        optimistic = false,
                        failed = false
                    )
                } else {
                    message
                }
            }.dedupeAndSort()
        }

        setDeliveryState(
            clientMessageId = clientMessageId,
            state = MessageDeliveryState.SENT
        )
    }

    fun markFailed(clientMessageId: String) {
        setDeliveryState(clientMessageId, MessageDeliveryState.FAILED)

        _messages.update { current ->
            current.map { message ->
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
    }

    fun setDeliveryState(
        clientMessageId: String,
        state: MessageDeliveryState
    ) {
        if (clientMessageId.isBlank()) return

        _deliveryStates.update { current ->
            current + (clientMessageId to state)
        }
    }

    fun deliveryStateFor(clientMessageId: String?): MessageDeliveryState? {
        if (clientMessageId.isNullOrBlank()) return null
        return _deliveryStates.value[clientMessageId]
    }

    fun addReadBy(messageId: Int, reader: SenderDto) {
        _messages.update { current ->
            current.map { message ->
                if (message.id != messageId) {
                    message
                } else {
                    val alreadyRead = message.readBy.any { it.id == reader.id }

                    if (alreadyRead) {
                        message
                    } else {
                        message.copy(
                            readBy = message.readBy + reader
                        )
                    }
                }
            }
        }
    }

    private fun List<MessageDto>.upsertMessage(
        incoming: MessageDto
    ): List<MessageDto> {
        val incomingServerId = incoming.id.takeIf { it > 0 }
        val incomingClientId = incoming.clientMessageId

        val mutable = toMutableList()

        val index = mutable.indexOfFirst { existing ->
            val sameServerId =
                incomingServerId != null &&
                        existing.id > 0 &&
                        existing.id == incomingServerId

            val sameClientId =
                !incomingClientId.isNullOrBlank() &&
                        existing.clientMessageId == incomingClientId

            sameServerId || sameClientId
        }

        if (index >= 0) {
            val existing = mutable[index]
            mutable[index] = existing.mergeWith(incoming)
        } else {
            mutable.add(incoming)
        }

        return mutable
    }

    private fun MessageDto.mergeWith(
        incoming: MessageDto
    ): MessageDto {
        if (deletedForAll == true || incoming.deletedForAll == true) {
            return copy(
                deletedForAll = true,
                deletedAt = incoming.deletedAt ?: deletedAt,
                rawContent = "",
                content = "",
                decryptedContent = "",
                attachments = emptyList(),
                attachmentsInline = emptyList()
            )
        }

        return copy(
            id = if (incoming.id > 0) incoming.id else id,

            rawContent = incoming.rawContent ?: rawContent,
            content = incoming.content ?: content,
            translatedForMe = incoming.translatedForMe ?: translatedForMe,
            decryptedContent = incoming.decryptedContent ?: decryptedContent,

            contentCiphertext = incoming.contentCiphertext ?: contentCiphertext,
            encryptedKeyForMe = incoming.encryptedKeyForMe ?: encryptedKeyForMe,
            encryptedKeys = incoming.encryptedKeys ?: encryptedKeys,
            encryptedPayloadForMe = incoming.encryptedPayloadForMe ?: encryptedPayloadForMe,
            encryptionVersion = incoming.encryptionVersion ?: encryptionVersion,

            createdAt = incoming.createdAt.ifBlank { createdAt },
            expiresAt = incoming.expiresAt ?: expiresAt,
            editedAt = incoming.editedAt ?: editedAt,
            deletedAt = incoming.deletedAt ?: deletedAt,
            deletedForAll = incoming.deletedForAll ?: deletedForAll,
            deletedBySender = incoming.deletedBySender ?: deletedBySender,
            revision = incoming.revision ?: revision,

            sender = incoming.sender,
            senderId = incoming.senderId ?: senderId,
            chatRoomId = incoming.chatRoomId ?: chatRoomId,
            clientMessageId = incoming.clientMessageId ?: clientMessageId,

            readBy = if (incoming.readBy.isNotEmpty()) incoming.readBy else readBy,

            attachments = if (incoming.attachments.isNotEmpty()) {
                incoming.attachments
            } else {
                attachments
            },

            attachmentsInline = if (incoming.attachmentsInline.isNotEmpty()) {
                incoming.attachmentsInline
            } else {
                attachmentsInline
            },

            optimistic = incoming.optimistic,
            failed = incoming.failed
        )
    }

    private fun List<MessageDto>.dedupeAndSort(): List<MessageDto> {
        val seenServerIds = mutableSetOf<Int>()
        val seenClientIds = mutableSetOf<String>()

        return this
            .sortedWith(
                compareBy<MessageDto> { it.createdAt }
                    .thenBy { it.id }
            )
            .filter { message ->
                if (message.id > 0) {
                    seenServerIds.add(message.id)
                } else {
                    val clientId = message.clientMessageId

                    if (!clientId.isNullOrBlank()) {
                        seenClientIds.add(clientId)
                    } else {
                        true
                    }
                }
            }
    }
}
