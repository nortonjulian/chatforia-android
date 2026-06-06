package com.chatforia.android.ria

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RiaChatUiState(
    val messages: List<RiaChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val aiDisabledReason: String? = null
)

class RiaChatViewModel(
    private val repository: RiaRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RiaChatUiState())
    val state: StateFlow<RiaChatUiState> = _state

    fun sendMessage(
        text: String,
        memoryEnabled: Boolean = true,
        filterProfanity: Boolean = false
    ) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val userMessage = RiaChatMessage(
            role = "user",
            content = trimmed
        )

        _state.value = _state.value.copy(
            messages = _state.value.messages + userMessage,
            isLoading = true,
            error = null,
            aiDisabledReason = null
        )

        viewModelScope.launch {
            try {
                val contextMessages =
                    _state.value.messages.map {
                        RiaContextMessageDto(
                            role = it.role,
                            content = it.content
                        )
                    }

                val reply = repository.chat(
                    messages = contextMessages,
                    memoryEnabled = memoryEnabled,
                    filterProfanity = filterProfanity
                )

                val assistantMessage = RiaChatMessage(
                    role = "assistant",
                    content = reply
                )

                _state.value = _state.value.copy(
                    messages = _state.value.messages + assistantMessage,
                    isLoading = false
                )
            } catch (e: Exception) {
                val message = e.message.orEmpty().lowercase()

                _state.value =
                    if (
                        message.contains("strict_e2ee") ||
                        message.contains("strict e2ee")
                    ) {
                        _state.value.copy(
                            isLoading = false,
                            aiDisabledReason =
                                "Ria is unavailable while Strict E2EE is enabled."
                        )
                    } else if (
                        message.contains("429") ||
                        message.contains("quota") ||
                        message.contains("billing")
                    ) {
                        _state.value.copy(
                            isLoading = false,
                            error =
                                "Ria isn’t available yet because AI billing hasn’t been set up."
                        )
                    } else {
                        _state.value.copy(
                            isLoading = false,
                            error =
                                e.message ?: "Ria could not respond."
                        )
                    }
            }
        }
    }

    fun clearConversation() {
        _state.value = RiaChatUiState()
    }
}