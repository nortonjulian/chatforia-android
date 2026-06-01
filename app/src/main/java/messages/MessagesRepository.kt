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
    private data class SendMessageEnvelope(
        val message: MessageDto? = null,
        val item: MessageDto? = null,
        val shaped: MessageDto? = null
    ) {
        val resolved: MessageDto?
            get() = message ?: item ?: shaped
    }

    suspend fun sendMessage(
        roomId: Int,
        text: String,
        clientMessageId: String = UUID.randomUUID().toString()
    ): MessageDto? {
        val bodyJson =
            json.encodeToString(
                SendMessageRequest(
                    chatRoomId = roomId,
                    content = text,
                    clientMessageId = clientMessageId
                )
            )

        val responseText =
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

        return try {
            json.decodeFromString<SendMessageEnvelope>(responseText).resolved
        } catch (_: Exception) {
            try {
                json.decodeFromString<MessageDto>(responseText)
            } catch (_: Exception) {
                null
            }
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