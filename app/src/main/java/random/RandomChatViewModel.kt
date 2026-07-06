package com.chatforia.android.random

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatforia.android.socket.SocketManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class RandomChatViewModel(
    private val socketManager: SocketManager,
    private val currentUserId: Int
) : ViewModel() {

    private val _state = MutableStateFlow(RandomChatUiState())
    val state: StateFlow<RandomChatUiState> = _state

    init {
        observeSocket()
    }

    fun startSearch() {
        _state.value = RandomChatUiState(
            isSearching = true,
            error = null
        )

        socketManager.cancelRandomChat()

        viewModelScope.launch {
            kotlinx.coroutines.delay(200)
            socketManager.startRandomChat()
        }
    }

    fun cancelSearch() {
        socketManager.cancelRandomChat()
        _state.value = RandomChatUiState()
    }

    private fun observeSocket() {
        viewModelScope.launch {
            socketManager.randomChatWaiting.collect {
                _state.value = _state.value.copy(
                    isSearching = true,
                    error = null
                )
            }
        }

        viewModelScope.launch {
            socketManager.randomChatMatched.collect { raw ->
                try {
                    val json = JSONObject(raw)

                    val chatRoomId =
                        json.optInt("chatRoomId", 0)
                            .takeIf { it > 0 }
                            ?: json.optInt("roomId", 0)
                                .takeIf { it > 0 }
                            ?: throw Exception("Missing random chat room id.")

                    val randomChatRoomId =
                        json.optInt("randomChatRoomId", 0)
                            .takeIf { it > 0 }

                    val partnerAlias = json.optString("partnerAlias", "Someone")
                        .takeIf { it.isNotBlank() }
                        ?: "Someone"

                    val partnerDisplayName = json.optString("partnerDisplayName")
                        .takeIf { it.isNotBlank() }

                    val relationshipStatus = json.optString("relationshipStatus", "none")
                        .takeIf { it.isNotBlank() }
                        ?: "none"

                    val session = RandomSession(
                        roomId = chatRoomId,
                        randomChatRoomId = randomChatRoomId,
                        myAlias = json.optString("myAlias", "You"),
                        partnerAlias = partnerAlias,
                        partnerDisplayName = partnerDisplayName,
                        relationshipStatus = relationshipStatus
                    )

                    _state.value = RandomChatUiState(
                        isSearching = false,
                        session = session
                    )
                } catch (e: Exception) {
                    _state.value = RandomChatUiState(
                        isSearching = false,
                        error = e.message ?: "Could not start random chat."
                    )
                }
            }
        }

        viewModelScope.launch {
            socketManager.randomChatError.collect { raw ->
                val message =
                    try {
                        JSONObject(raw).optString(
                            "message",
                            "Could not find a random chat."
                        )
                    } catch (_: Exception) {
                        raw
                    }

                _state.value = RandomChatUiState(
                    isSearching = false,
                    error = message
                )
            }
        }

        viewModelScope.launch {
            socketManager.randomChatMessages.collect { raw ->
                try {
                    val json = JSONObject(raw)

                    val message = RandomChatMessage(
                        id = json.optString(
                            "createdAt",
                            System.currentTimeMillis().toString()
                        ),
                        text = json.optString("content"),
                        isMine = json.optInt("senderId") == currentUserId
                    )

                    _state.value = _state.value.copy(
                        messages = _state.value.messages + message
                    )
                } catch (_: Exception) {
                }
            }
        }

        viewModelScope.launch {
            socketManager.randomChatEnded.collect {
                _state.value = RandomChatUiState(
                    error = "Random chat ended."
                )
            }
        }

        viewModelScope.launch {
            socketManager.randomFriendAccepted.collect { raw ->
                val session = _state.value.session ?: return@collect

                val json = JSONObject(raw)

                val username = json.optString("username")
                    .takeIf { it.isNotBlank() }

                _state.value = _state.value.copy(
                    session = session.copy(
                        iRequestedFriend = true,
                        partnerRequestedFriend = true,
                        partnerDisplayName = username ?: session.partnerDisplayName,
                        relationshipStatus = "friends"
                    )
                )
            }
        }
    }

    fun sendMessage(text: String) {
        val session = _state.value.session ?: return

        socketManager.sendRandomMessage(
            roomId = session.roomId,
            text = text
        )
    }

    fun skip() {
        socketManager.skipRandomChat()
        _state.value = RandomChatUiState(isSearching = true)

        viewModelScope.launch {
            kotlinx.coroutines.delay(200)
            socketManager.startRandomChat()
        }
    }

    fun requestFriend() {
        val session = _state.value.session ?: return
        socketManager.requestRandomFriend(session.roomId)

        _state.value = _state.value.copy(
            session = session.copy(iRequestedFriend = true)
        )
    }

    fun endChat() {
        socketManager.cancelRandomChat()
        _state.value = RandomChatUiState()
    }

}

