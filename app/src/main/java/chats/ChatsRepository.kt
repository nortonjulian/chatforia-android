package com.chatforia.android.chats

import com.chatforia.android.network.ApiClient
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatsRepository(
    private val apiClient: ApiClient
) {

    suspend fun deleteConversation(conversation: ConversationDto) {
        val id = conversation.id ?: return
        val kind = conversation.kind

        withContext(Dispatchers.IO) {
            apiClient.send<Unit>(
                ApiRequest(
                    path = "conversations/$kind/$id",
                    method = HttpMethod.DELETE,
                    requiresAuth = true
                )
            )
        }
    }
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