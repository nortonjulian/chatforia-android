package com.chatforia.android.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MessagesViewModel(
    private val repository: MessagesRepository
) : ViewModel() {

    private val _messages =
        MutableStateFlow<List<MessageDto>>(
            emptyList()
        )

    val messages =
        _messages.asStateFlow()

    fun loadMessages(
        roomId: Int
    ) {

        viewModelScope.launch {

            _messages.value =
                repository.loadMessages(roomId)
        }
    }

    fun sendMessage(
        roomId: Int,
        text: String
    ) {

        viewModelScope.launch {

            repository.sendMessage(
                roomId,
                text
            )

            loadMessages(roomId)
        }
    }
}