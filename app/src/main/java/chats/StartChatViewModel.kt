package com.chatforia.android.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatforia.android.contacts.ContactDto
import com.chatforia.android.contacts.ContactsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StartChatViewModel(
    private val contactsRepository: ContactsRepository
) : ViewModel() {

    private val _state =
        MutableStateFlow(StartChatState())

    val state: StateFlow<StartChatState> =
        _state

    private var searchJob: Job? = null

    fun updateUsername(value: String) {
        _state.value =
            _state.value.copy(
                username = value,
                error = null
            )

        searchJob?.cancel()

        val query =
            value.trim()

        if (query.isBlank()) {
            _state.value =
                _state.value.copy(
                    contactResults = emptyList()
                )
            return
        }

        searchJob =
            viewModelScope.launch {
                delay(250)

                searchContacts(query)
            }
    }

    private suspend fun searchContacts(query: String) {
        try {
            val response =
                contactsRepository.fetchContacts(
                    query = query,
                    limit = 20
                )

            _state.value =
                _state.value.copy(
                    contactResults = response.items,
                    error = null
                )

        } catch (e: Exception) {
            _state.value =
                _state.value.copy(
                    contactResults = emptyList(),
                    error = e.message ?: "Failed to search contacts."
                )
        }
    }

    fun startChat() {
        val username =
            _state.value.username.trim()

        if (username.isBlank()) {
            _state.value =
                _state.value.copy(
                    error = "Enter a username."
                )
            return
        }

        viewModelScope.launch {
            _state.value =
                _state.value.copy(
                    isLoading = true,
                    error = null,
                    openedConversation = null
                )

            try {
                val user =
                    contactsRepository.lookupUser(username)

                openDirectChat(
                    userId = user.userId,
                    title = user.username.ifBlank { username }
                )

            } catch (e: Exception) {
                _state.value =
                    _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to start chat."
                    )
            }
        }
    }

    fun openContact(contact: ContactDto) {
        val userId =
            contact.user?.id ?: contact.userId

        val title =
            contact.alias?.trim()?.takeIf { it.isNotBlank() }
                ?: contact.user?.username?.trim()?.takeIf { it.isNotBlank() }
                ?: contact.externalName?.trim()?.takeIf { it.isNotBlank() }
                ?: contact.externalPhone?.trim()?.takeIf { it.isNotBlank() }
                ?: "New Chat"

        viewModelScope.launch {
            _state.value =
                _state.value.copy(
                    isLoading = true,
                    error = null,
                    openedConversation = null
                )

            try {
                if (userId != null) {
                    openDirectChat(
                        userId = userId,
                        title = title
                    )
                    return@launch
                }

                val phone =
                    contact.externalPhone?.trim()?.takeIf { it.isNotBlank() }
                        ?: throw Exception("This contact does not have a phone number.")

                val smsThread =
                    contactsRepository.startSmsThread(
                        phone = phone,
                        contactId = contact.id
                    )

                val resolvedTitle =
                    smsThread.displayName?.trim()?.takeIf { it.isNotBlank() }
                        ?: smsThread.contactName?.trim()?.takeIf { it.isNotBlank() }
                        ?: title

                _state.value =
                    _state.value.copy(
                        isLoading = false,
                        openedConversation =
                            ConversationDto(
                                kind = "sms",
                                id = smsThread.id,
                                title = resolvedTitle,
                                displayName = resolvedTitle,
                                updatedAt = smsThread.updatedAt,
                                isGroup = false,
                                phone = smsThread.contactPhone ?: phone,
                                unreadCount = 0
                            )
                    )

            } catch (e: Exception) {
                _state.value =
                    _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to open contact."
                    )
            }
        }
    }

    private suspend fun openDirectChat(
        userId: Int,
        title: String
    ) {
        val response =
            contactsRepository.openDirectChat(userId)

        val roomId =
            response.resolvedRoomId
                ?: throw Exception("Could not open chat.")

        _state.value =
            _state.value.copy(
                isLoading = false,
                openedConversation =
                    ConversationDto(
                        kind = "chat",
                        id = roomId,
                        title = title,
                        displayName = title,
                        isGroup = false,
                        unreadCount = 0
                    )
            )
    }

    fun clearOpenedConversation() {
        _state.value =
            _state.value.copy(
                openedConversation = null
            )
    }
}

data class StartChatState(
    val username: String = "",
    val contactResults: List<ContactDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val openedConversation: ConversationDto? = null
)