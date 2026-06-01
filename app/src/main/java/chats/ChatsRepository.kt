package com.chatforia.android.chats

import com.chatforia.android.network.ApiClient
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatsRepository(
    private val apiClient: ApiClient
) {
    suspend fun loadConversations(): List<ConversationDto> {
        val response: ConversationsResponse =
            withContext(Dispatchers.IO) {
                apiClient.send(
                    ApiRequest(
                        path = "conversations",
                        method = HttpMethod.GET,
                        requiresAuth = true
                    )
                )
            }

        return response.conversations
            ?: response.items
            ?: emptyList()
    }
}