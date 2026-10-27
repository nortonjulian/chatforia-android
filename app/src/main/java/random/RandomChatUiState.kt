package com.chatforia.android.random

data class RandomChatUiState(
    val isSearching: Boolean = false,
    val error: String? = null,
    val session: RandomSession? = null,
    val endedRoomId: Int? = null,
    val messages: List<RandomChatMessage> = emptyList()
)