package com.chatforia.android.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatforia.android.messages.MessageDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
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

    fun applyRealtimeMessageJson(messageJson: String) {
        viewModelScope.launch {
            try {
                val message =
                    json.decodeFromString<MessageDto>(messageJson)

                applyRealtimeMessage(message)

            } catch (e: Exception) {
                println("❌ Failed to apply realtime conversation preview: ${e.message}")
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