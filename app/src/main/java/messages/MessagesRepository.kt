package com.chatforia.android.messages

import com.chatforia.android.network.ApiClient
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class MessagesRepository(
    private val apiClient: ApiClient
) {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Serializable
    private data class SendMessageRequest(
        val chatRoomId: Int,
        val content: String,
        val clientMessageId: String
    )

    suspend fun sendMessage(
        roomId: Int,
        text: String
    ) {
        val bodyJson =
            json.encodeToString(
                SendMessageRequest(
                    chatRoomId = roomId,
                    content = text,
                    clientMessageId = UUID.randomUUID().toString()
                )
            )

        withContext(Dispatchers.IO) {
            apiClient.sendRaw(
                ApiRequest(
                    path = "messages",
                    method = HttpMethod.POST,
                    bodyJson = bodyJson,
                    requiresAuth = true
                )
            )
        }
    }

    suspend fun loadMessages(
        roomId: Int
    ): List<MessageDto> {

        val response: MessagesResponse =
            withContext(Dispatchers.IO) {

                apiClient.send(
                    ApiRequest(
                        path = "messages/$roomId",
                        method = HttpMethod.GET,
                        requiresAuth = true
                    )
                )
            }

        return response.items
    }
}