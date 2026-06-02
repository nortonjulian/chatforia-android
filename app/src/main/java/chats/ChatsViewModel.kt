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

class ChatsViewModel(
    private val repository: ChatsRepository
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
                _conversations.value =
                    sortConversations(
                        repository.loadConversations()
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

    private fun applyRealtimeMessage(message: MessageDto) {
        val roomId =
            message.chatRoomId ?: return

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
            message.decryptedContent
                ?.takeIf { it.isNotBlank() }
                ?: message.translatedForMe
                    ?.takeIf { it.isNotBlank() }
                ?: message.rawContent
                    ?.takeIf { it.isNotBlank() }
                ?: message.content
                    ?.takeIf { it.isNotBlank() }
                ?: if (!message.contentCiphertext.isNullOrBlank()) {
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