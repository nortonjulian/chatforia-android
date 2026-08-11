package com.chatforia.android.chats

import android.telephony.PhoneNumberUtils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatforia.android.contacts.ContactDto
import com.chatforia.android.contacts.ContactsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class StartChatViewModel(
    private val contactsRepository: ContactsRepository
) : ViewModel() {

    private val _state =
        MutableStateFlow(StartChatState())

    val state: StateFlow<StartChatState> =
        _state

    private var searchJob: Job? = null

    fun updateUsername(value: String) {
        val normalizedPhoneNumber =
            normalizePhoneNumber(value)

        _state.value =
            _state.value.copy(
                username = value,
                normalizedPhoneNumber = normalizedPhoneNumber,
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
        val current = _state.value
        val query = current.username.trim()

        if (query.isBlank()) {
            _state.value =
                current.copy(
                    error = "Enter a username or phone number."
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
                val phone = current.normalizedPhoneNumber

                if (phone != null) {
                    openSmsThread(
                        phone = phone,
                        fallbackTitle = phone
                    )
                    return@launch
                }

                val user =
                    contactsRepository.lookupUser(query)

                openDirectChat(
                    userId = user.userId,
                    title = user.username.ifBlank { query }
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

                openSmsThread(
                    phone = phone,
                    contactId = contact.id,
                    fallbackTitle = title
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

    fun openDirectChatFromCall(
        userId: Int,
        title: String
    ) {
        viewModelScope.launch {
            _state.value =
                _state.value.copy(
                    isLoading = true,
                    error = null,
                    openedConversation = null
                )

            try {
                openDirectChat(
                    userId = userId,
                    title = title
                )
            } catch (e: Exception) {
                _state.value =
                    _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to open chat."
                    )
            }
        }
    }

    private suspend fun openSmsThread(
        phone: String,
        contactId: Int? = null,
        fallbackTitle: String = phone
    ) {
        val smsThread =
            contactsRepository.startSmsThread(
                phone = phone,
                contactId = contactId
            )

        val resolvedPhone =
            smsThread.contactPhone
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: phone

        val resolvedTitle =
            smsThread.displayName
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: smsThread.contactName
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                ?: fallbackTitle

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
                        phone = resolvedPhone,
                        unreadCount = 0
                    )
            )
    }

    private fun normalizePhoneNumber(value: String): String? {
        val raw = value.trim()

        if (raw.isBlank()) {
            return null
        }

        val containsOnlyPhoneCharacters =
            raw.all { character ->
                character.isDigit() ||
                    character == '+' ||
                    character == '-' ||
                    character == '(' ||
                    character == ')' ||
                    character == '.' ||
                    character.isWhitespace()
            }

        if (!containsOnlyPhoneCharacters) {
            return null
        }

        val normalized =
            PhoneNumberUtils.normalizeNumber(raw)

        val digitCount =
            normalized.count { it.isDigit() }

        if (digitCount !in 7..15) {
            return null
        }

        PhoneNumberUtils.formatNumberToE164(
            raw,
            Locale.getDefault().country
        )?.let {
            return it
        }

        if (normalized.startsWith("+") && digitCount in 8..15) {
            return normalized
        }

        if (digitCount == 10) {
            return "+1$normalized"
        }

        return null
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


    fun setGroupMode(enabled: Boolean) {
        _state.value =
            _state.value.copy(
                isGroupMode = enabled,
                selectedContacts =
                    if (enabled) {
                        _state.value.selectedContacts
                    } else {
                        emptyList()
                    },
                groupName =
                    if (enabled) {
                        _state.value.groupName
                    } else {
                        ""
                    },
                error = null
            )
    }

    fun updateGroupName(value: String) {
        _state.value =
            _state.value.copy(
                groupName = value
            )
    }

    fun handleContactTap(contact: ContactDto) {
        if (_state.value.isGroupMode) {
            toggleSelectedContact(contact)
        } else {
            openContact(contact)
        }
    }

    fun toggleSelectedContact(contact: ContactDto) {
        val userId =
            contact.user?.id ?: contact.userId

        if (userId == null) {
            _state.value =
                _state.value.copy(
                    error = "Only Chatforia users can be added to group chats."
                )
            return
        }

        val current = _state.value.selectedContacts

        val alreadySelected =
            current.any {
                (it.user?.id ?: it.userId) == userId
            }

        val next =
            if (alreadySelected) {
                current.filterNot {
                    (it.user?.id ?: it.userId) == userId
                }
            } else {
                current + contact
            }

        _state.value =
            _state.value.copy(
                selectedContacts = next,
                error = null
            )
    }

    fun createGroupChat() {
        val current = _state.value

        val userIds =
            current.selectedContacts
                .mapNotNull { it.user?.id ?: it.userId }
                .distinct()

        if (userIds.size < 2) {
            _state.value =
                current.copy(
                    error = "Select at least 2 Chatforia users for a group chat."
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
                val response =
                    contactsRepository.createGroupChat(
                        userIds = userIds,
                        name = current.groupName
                    )

                val roomId =
                    response.resolvedRoomId
                        ?: throw Exception("Could not create group chat.")

                val fallbackTitle =
                    current.selectedContacts
                        .map {
                            it.alias?.trim()?.takeIf { value -> value.isNotBlank() }
                                ?: it.user?.username?.trim()?.takeIf { value -> value.isNotBlank() }
                                ?: "Chatforia user"
                        }
                        .joinToString(", ")

                val title =
                    current.groupName.trim().takeIf { it.isNotBlank() }
                        ?: fallbackTitle.takeIf { it.isNotBlank() }
                        ?: "Group chat"

                _state.value =
                    _state.value.copy(
                        isLoading = false,
                        openedConversation =
                            ConversationDto(
                                kind = "chat",
                                id = roomId,
                                title = title,
                                displayName = title,
                                isGroup = true,
                                unreadCount = 0
                            )
                    )

            } catch (e: Exception) {
                _state.value =
                    _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to create group chat."
                    )
            }
        }
    }
}

data class StartChatState(
    val username: String = "",
    val normalizedPhoneNumber: String? = null,
    val contactResults: List<ContactDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val openedConversation: ConversationDto? = null,

    val isGroupMode: Boolean = false,
    val selectedContacts: List<ContactDto> = emptyList(),
    val groupName: String = ""
)