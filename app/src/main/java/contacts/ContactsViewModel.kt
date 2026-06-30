package com.chatforia.android.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.chatforia.android.chats.ConversationDto

class ContactsViewModel(
    private val repository: ContactsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ContactsState())
    val state: StateFlow<ContactsState> = _state

    fun loadContacts(query: String? = null) {
        viewModelScope.launch {
            _state.value =
                _state.value.copy(
                    isLoading = true,
                    error = null,
                    importMessage = null,
                    searchText = query ?: _state.value.searchText
                )

            try {
                val response =
                    repository.fetchContacts(
                        query = _state.value.searchText
                    )

                _state.value =
                    _state.value.copy(
                        isLoading = false,
                        contacts = response.items,
                        nextCursor = response.nextCursor,
                        error = null
                    )

            } catch (e: Exception) {
                _state.value =
                    _state.value.copy(
                        isLoading = false,
                        contacts = emptyList(),
                        error = e.message ?: "Failed to load contacts."
                    )
            }
        }
    }

    fun updateSearchText(value: String) {
        _state.value =
            _state.value.copy(
                searchText = value
            )

        loadContacts(value)
    }

    fun deleteContact(contact: ContactDto) {
        viewModelScope.launch {
            try {
                repository.deleteContact(contact.id)

                _state.value =
                    _state.value.copy(
                        contacts =
                            _state.value.contacts.filterNot {
                                it.id == contact.id
                            },
                        importMessage = null
                    )

            } catch (e: Exception) {
                _state.value =
                    _state.value.copy(
                        error = e.message ?: "Failed to delete contact."
                    )
            }
        }
    }

    fun displayName(contact: ContactDto): String {
        return contact.alias?.trim()?.takeIf { it.isNotBlank() }
            ?: contact.user?.username?.trim()?.takeIf { it.isNotBlank() }
            ?: contact.externalName?.trim()?.takeIf { it.isNotBlank() }
            ?: contact.externalPhone?.trim()?.takeIf { it.isNotBlank() }
            ?: "Unknown contact"
    }

    fun subtitle(contact: ContactDto): String {
        val username =
            contact.user?.username?.trim()?.takeIf { it.isNotBlank() }

        val displayName = displayName(contact)

        if (username != null && username != displayName) {
            return "@$username"
        }

        val phone =
            contact.externalPhone?.trim()?.takeIf { it.isNotBlank() }

        if (phone != null) {
            return phone
        }

        return "Tap to view contact"
    }

    fun openDirectChat(contact: ContactDto) {
        viewModelScope.launch {
            val userId =
                contact.user?.id ?: contact.userId

            if (userId == null) {
                _state.value =
                    _state.value.copy(
                        error = "This contact has not joined Chatforia yet."
                    )
                return@launch
            }

            _state.value =
                _state.value.copy(
                    isOpeningChat = true,
                    error = null,
                    openedConversation = null
                )

            try {
                val response =
                    repository.openDirectChat(userId)

                val roomId =
                    response.resolvedRoomId
                        ?: throw Exception("Could not open chat.")

                val title = displayName(contact)

                val conversation =
                    ConversationDto(
                        kind = "chat",
                        id = roomId,
                        title = title,
                        displayName = title,
                        isGroup = false,
                        unreadCount = 0
                    )

                _state.value =
                    _state.value.copy(
                        isOpeningChat = false,
                        openedConversation = conversation
                    )

            } catch (e: Exception) {
                _state.value =
                    _state.value.copy(
                        isOpeningChat = false,
                        error = e.message ?: "Failed to open chat."
                    )
            }
        }
    }

    fun saveUserContact(
        userId: Int,
        alias: String? = null
    ) {
        viewModelScope.launch {
            try {
                repository.saveUserContact(
                    userId = userId,
                    alias = alias
                )

                loadContacts()

            } catch (e: Exception) {
                _state.value =
                    _state.value.copy(
                        error = e.message ?: "Failed to save contact."
                    )
            }
        }
    }

    fun saveUsernameContact(
        username: String,
        alias: String? = null,
        favorite: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                val user =
                    repository.lookupUser(
                        username.trim()
                    )

                repository.saveUserContact(
                    userId = user.userId,
                    alias = alias,
                    favorite = favorite
                )

                loadContacts()

            } catch (e: Exception) {
                _state.value =
                    _state.value.copy(
                        error = e.message ?: "Failed to save contact."
                    )
            }
        }
    }

    fun importPhoneContacts(
        phoneContacts: List<PhoneContactDto>
    ) {
        viewModelScope.launch {
            _state.value =
                _state.value.copy(
                    isImportingContacts = true,
                    error = null,
                    importMessage = null
                )

            try {
                if (phoneContacts.isEmpty()) {
                    _state.value =
                        _state.value.copy(
                            isImportingContacts = false,
                            importMessage = "No phone contacts found."
                        )
                    return@launch
                }
                val response =
                    repository.importContacts(phoneContacts)

                val count = response.importedCount

                _state.value =
                    _state.value.copy(
                        isImportingContacts = false,
                        contacts = response.items,
                        importMessage =
                            if (count == 1) {
                                "Imported 1 contact."
                            } else {
                                "Imported $count contacts."
                            }
                    )

            } catch (e: Exception) {
                _state.value =
                    _state.value.copy(
                        isImportingContacts = false,
                        error = e.message ?: "Failed to import contacts."
                    )
            }
        }
    }

    fun saveExternalContact(
        phone: String,
        name: String? = null,
        alias: String? = null,
        favorite: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                repository.saveExternalContact(
                    phone = phone,
                    externalName = name,
                    alias = alias,
                    favorite = favorite
                )

                loadContacts()

            } catch (e: Exception) {
                _state.value =
                    _state.value.copy(
                        error = e.message ?: "Failed to save contact."
                    )
            }
        }
    }

    fun updateContact(
        contact: ContactDto,
        alias: String? = null,
        externalName: String? = null,
        favorite: Boolean
    ) {
        viewModelScope.launch {
            try {
                val updated =
                    repository.updateContact(
                        contact = contact,
                        alias = alias,
                        externalName = externalName,
                        favorite = favorite
                    )

                _state.value =
                    _state.value.copy(
                        contacts =
                            _state.value.contacts.map {
                                if (it.id == updated.id) updated else it
                            },
                        error = null,
                        importMessage = "Contact updated."
                    )

                loadContacts()

            } catch (e: Exception) {
                _state.value =
                    _state.value.copy(
                        error = e.message ?: "Failed to update contact."
                    )
            }
        }
    }

    fun clearOpenedConversation() {
        _state.value =
            _state.value.copy(
                openedConversation = null
            )
    }
}

data class ContactsState(
    val contacts: List<ContactDto> = emptyList(),
    val searchText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val nextCursor: Int? = null,
    val isImportingContacts: Boolean = false,
    val importMessage: String? = null,
    val openedConversation: ConversationDto? = null,
    val isOpeningChat: Boolean = false
)