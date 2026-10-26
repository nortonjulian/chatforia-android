package com.chatforia.android.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatforia.android.messages.MessageDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant
import com.chatforia.android.crypto.KeyStorage
import com.chatforia.android.crypto.MessageDecryptor

class ChatsViewModel(
    private val repository: ChatsRepository,
    private val keyStorage: KeyStorage,
    private val messageDecryptor: MessageDecryptor = MessageDecryptor()
) : ViewModel() {

    private val _conversations =
        MutableStateFlow<List<ConversationDto>>(emptyList())

    val conversations:
            StateFlow<List<ConversationDto>>
            = _conversations

    private val _isLoading =
        MutableStateFlow(false)

    val isLoading:
            StateFlow<Boolean>
            = _isLoading

    private val _error =
        MutableStateFlow<String?>(null)

    val error:
            StateFlow<String?>
            = _error

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun loadConversations() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val loaded =
                    repository.loadConversations()

                _conversations.value =
                    sortConversations(
                        hydrateEncryptedPreviews(loaded)
                    )

            } catch (e: Exception) {
                _error.value =
                    e.message ?: "Failed to load chats."

            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshConversations() {
        loadConversations()
    }

    /**
     * Reconcile only authoritative unread badges after a realtime event.
     * A full loadConversations() here can overwrite newer decrypted previews,
     * re-sort chats, and race with other asynchronous list refreshes.
     */
    suspend fun reconcileUnreadCounts() {
        try {
            val authoritative = repository.loadConversations()
            _conversations.value = mergeConversationUnreadCounts(
                current = _conversations.value,
                authoritative = authoritative
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("⚠️ Could not reconcile unread counts: ${e.message}")
        }
    }

    fun deleteConversation(conversation: ConversationDto) {
        val before = _conversations.value

        _conversations.value =
            before.filterNot { it.uniqueId == conversation.uniqueId }

        viewModelScope.launch {
            try {
                repository.deleteConversation(conversation)
                loadConversations()
            } catch (e: Exception) {
                _conversations.value = before
                _error.value = e.message ?: "Failed to delete conversation."
            }
        }
    }


    private val _isDeletingConversations = MutableStateFlow(false)
    val isDeletingConversations: StateFlow<Boolean> = _isDeletingConversations

    suspend fun deleteConversations(
        selected: List<ConversationDto>
    ): Set<String> {
        if (_isDeletingConversations.value) {
            return selected.map { it.uniqueId }.toSet()
        }

        _isDeletingConversations.value = true
        _error.value = null
        val failed = mutableSetOf<String>()

        try {
            for (conversation in selected.distinctBy { it.uniqueId }) {
                try {
                    repository.deleteConversation(conversation)
                    _conversations.value = _conversations.value.filterNot {
                        it.uniqueId == conversation.uniqueId
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    failed.add(conversation.uniqueId)
                }
            }
            return failed
        } finally {
            _isDeletingConversations.value = false
        }
    }

    fun applyRealtimeMessageJson(messageJson: String) {
        viewModelScope.launch {
            try {
                val message =
                    json.decodeFromString<MessageDto>(messageJson)

                // A delete-for-everyone is also emitted as a message:upsert
                // tombstone. It is not a new message and must never retain
                // or replace the deleted text in the Chats list preview.
                if (message.deletedForAll == true) {
                    val roomId = message.chatRoomId
                    if (roomId != null && shouldRefreshDeletedPreview(
                            _conversations.value, roomId, message.id
                        )
                    ) {
                        refreshConversations()
                    }
                    return@launch
                }

                applyRealtimeMessage(message)

            } catch (e: Exception) {
                println("❌ Failed to apply realtime conversation preview: ${e.message}")
                refreshConversations()
            }
        }
    }

    fun applyRealtimeDeleteJson(payloadJson: String) {
        viewModelScope.launch {
            try {
                // Production sends { item: { id, chatRoomId, ... } }.
                // Delete-for-me also includes a top-level roomId.
                val target = parseConversationDeleteTarget(payloadJson)
                    ?: return@launch

                if (shouldRefreshDeletedPreview(
                        _conversations.value, target.roomId, target.messageId
                    )
                ) {
                    // Canonical /conversations already excludes deleted
                    // messages, so this restores the previous surviving one.
                    refreshConversations()
                }
            } catch (e: Exception) {
                println("❌ Failed to apply realtime conversation delete: ${e.message}")
                refreshConversations()
            }
        }
    }

    private suspend fun hydrateEncryptedPreviews(
        conversations: List<ConversationDto>
    ): List<ConversationDto> {
        val userId = currentUserId
            ?: return conversations

        val privateKey = keyStorage.readPrivateKey()

        return conversations.map { conversation ->
            if (!conversation.needsPreviewHydration()) {
                return@map conversation
            }

            val roomId = conversation.id
                ?: return@map conversation

            val latestMessage =
                try {
                    repository
                        .loadRecentMessages(roomId = roomId, limit = 10)
                        .maxWithOrNull(
                            compareBy<MessageDto> { it.createdAt }
                                .thenBy { it.id }
                        )
                } catch (e: Exception) {
                    null
                } ?: return@map conversation

            val decryptedText =
                messageDecryptor.decryptMessageOrNull(
                    message = latestMessage,
                    currentUserPrivateKeyB64 = privateKey,
                    currentUserId = userId
                )

            val previewText =
                decryptedText
                    ?.takeIf { it.isNotBlank() }
                    ?: latestMessage.decryptedContent
                        ?.takeIf { it.isNotBlank() }
                    ?: latestMessage.translatedForMe
                        ?.takeIf { it.isNotBlank() }
                    ?: latestMessage.rawContent
                        ?.takeIf { it.isNotBlank() }
                    ?: latestMessage.content
                        ?.takeIf { it.isNotBlank() }
                    ?: if (latestMessage.attachments.isNotEmpty() || latestMessage.attachmentsInline.isNotEmpty()) {
                        "[media]"
                    } else {
                        conversation.last?.text
                    }

            if (previewText.isNullOrBlank()) {
                return@map conversation
            }

            conversation.copy(
                updatedAt = latestMessage.createdAt,
                last = ConversationLastDto(
                    text = previewText,
                    messageId = latestMessage.id.takeIf { it > 0 }
                        ?: conversation.last?.messageId,
                    at = latestMessage.createdAt,
                    hasMedia =
                        latestMessage.attachments.isNotEmpty() ||
                                latestMessage.attachmentsInline.isNotEmpty() ||
                                conversation.last?.hasMedia == true,
                    mediaCount = conversation.last?.mediaCount,
                    mediaKinds = conversation.last?.mediaKinds,
                    thumbUrl = conversation.last?.thumbUrl,
                    senderName =
                        latestMessage.sender.username
                            ?: conversation.last?.senderName
                )
            )
        }
    }

    private fun ConversationDto.needsPreviewHydration(): Boolean {
        if (!kind.equals("chat", ignoreCase = true)) {
            return false
        }

        val text =
            last?.text
                ?.trim()
                .orEmpty()

        return text.isBlank() ||
                text.equals("Message", ignoreCase = true) ||
                text.equals("Tap to open", ignoreCase = true) ||
                text.equals("[encrypted message]", ignoreCase = true)
    }

    private fun MessageDto.hasEncryptedBody(): Boolean {
        return !encryptedPayloadForMe?.contentCiphertext.isNullOrBlank() ||
                !encryptedPayloadForMe?.encryptedKey.isNullOrBlank() ||
                !contentCiphertext.isNullOrBlank() ||
                !encryptedKeyForMe.isNullOrBlank() ||
                !encryptedKeys.isNullOrEmpty()
    }
    private fun applyRealtimeMessage(message: MessageDto) {
        val roomId =
            message.chatRoomId ?: return

        val decryptedText =
            currentUserId?.let { userId ->
                messageDecryptor.decryptMessageOrNull(
                    message = message,
                    currentUserPrivateKeyB64 = keyStorage.readPrivateKey(),
                    currentUserId = userId
                )
            }

        val current =
            _conversations.value.toMutableList()

        val index =
            current.indexOfFirst { conversation ->
                conversation.id == roomId
            }

        if (index == -1) {
            refreshConversations()
            return
        }

        val conversation =
            current[index]

        val previewText =
            decryptedText
                ?.takeIf { it.isNotBlank() }
                ?: message.decryptedContent
                    ?.takeIf { it.isNotBlank() }
                ?: message.translatedForMe
                    ?.takeIf { it.isNotBlank() }
                ?: message.rawContent
                    ?.takeIf { it.isNotBlank() }
                ?: message.content
                    ?.takeIf { it.isNotBlank() }
                ?: if (message.hasEncryptedBody()) {
                    "[encrypted message]"
                } else if (message.attachments.isNotEmpty() || message.attachmentsInline.isNotEmpty()) {
                    "[media]"
                } else {
                    conversation.last?.text ?: "[unsupported message]"
                }

        val timestamp =
            message.createdAt
                .takeIf { it.isNotBlank() }
                ?: Instant.now().toString()

        val updated =
            conversation.copy(
                updatedAt = timestamp,
                last = ConversationLastDto(
                    text = previewText,
                    messageId =
                        if (message.id > 0) {
                            message.id
                        } else {
                            conversation.last?.messageId
                        },
                    at = timestamp,
                    hasMedia = conversation.last?.hasMedia,
                    mediaCount = conversation.last?.mediaCount,
                    mediaKinds = conversation.last?.mediaKinds,
                    thumbUrl = conversation.last?.thumbUrl,
                    senderName =
                        message.sender.username
                            ?: conversation.last?.senderName
                )
            )

        current[index] = updated

        _conversations.value =
            sortConversations(current)
    }

    private var currentUserId: Int? = null

    fun configureCurrentUser(id: Int?) {
        currentUserId = id
    }

    private fun sortConversations(
        items: List<ConversationDto>
    ): List<ConversationDto> {
        return items.sortedWith(
            compareByDescending<ConversationDto> { conversation ->
                conversation.last?.at
                    ?: conversation.updatedAt
                    ?: ""
            }.thenByDescending { conversation ->
                conversation.id ?: 0
            }
        )
    }
}
/**
 * Update unread badges without altering the current live previews,
 * selection state, message ordering, or local decrypted text.
 */
internal fun mergeConversationUnreadCounts(
    current: List<ConversationDto>,
    authoritative: List<ConversationDto>
): List<ConversationDto> {
    val countsByConversation = authoritative.associate {
        it.uniqueId to (it.unreadCount ?: 0)
    }
    return current.map { local ->
        val count = countsByConversation[local.uniqueId]
        if (count == null) local else local.copy(unreadCount = count)
    }
}

/**
 * Accept both the production event envelope { item: { id, chatRoomId } }
 * and legacy flat delete events. Do not infer a message ID from a room ID.
 */
internal data class ConversationDeleteTarget(val roomId: Int, val messageId: Int)

internal fun parseConversationDeleteTarget(payloadJson: String): ConversationDeleteTarget? {
    val outer = Json.parseToJsonElement(payloadJson) as? JsonObject ?: return null
    val item = (outer["item"] as? JsonObject)
        ?: (outer["message"] as? JsonObject)
        ?: outer

    fun integer(value: kotlinx.serialization.json.JsonElement?): Int? =
        (value as? JsonPrimitive)?.content?.toIntOrNull()

    val roomId = integer(item["chatRoomId"])
        ?: integer(item["roomId"])
        ?: integer(outer["chatRoomId"])
        ?: integer(outer["roomId"])
        ?: return null
    val messageId = integer(item["id"])
        ?: integer(item["messageId"])
        ?: integer(outer["id"])
        ?: integer(outer["messageId"])
        ?: return null

    return ConversationDeleteTarget(roomId, messageId)
}

/** A deleted older message must not replace a newer conversation preview. */
internal fun shouldRefreshDeletedPreview(
    conversations: List<ConversationDto>,
    roomId: Int,
    messageId: Int
): Boolean = conversations.any {
    it.kind.equals("chat", ignoreCase = true) &&
        it.id == roomId &&
        it.last?.messageId == messageId
}
