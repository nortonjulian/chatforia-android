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
import com.chatforia.android.messages.SendSmsRequest
import com.chatforia.android.messages.SendSmsResponse

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
        clientMessageId: String = UUID.randomUUID().toString(),
        attachmentsInline: List<AttachmentDto> = emptyList(),
        contentCiphertext: String? = null,
        encryptedKeys: Map<String, String>? = null,
        encryptionVersion: Int? = null
    ): MessageDto? {
        val bodyJson =
            json.encodeToString(
                SendMessageRequest(
                    chatRoomId = roomId,
                    content =
                        if (contentCiphertext.isNullOrBlank()) {
                            text.ifBlank { null }
                        } else {
                            null
                        },
                    contentCiphertext = contentCiphertext,
                    encryptedKeys = encryptedKeys,
                    encryptionVersion = encryptionVersion,
                    clientMessageId = clientMessageId,
                    attachmentsInline = attachmentsInline
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

    suspend fun loadRoomParticipants(
        roomId: Int
    ): List<RoomParticipantDto> {
        val response: RoomParticipantsResponse =
            withContext(Dispatchers.IO) {
                apiClient.send(
                    ApiRequest(
                        path = "chatrooms/$roomId/participants",
                        method = HttpMethod.GET,
                        requiresAuth = true
                    )
                )
            }

        return response.participants
    }

    suspend fun loadSmsThread(threadId: Int): SmsThreadDto {
        return withContext(Dispatchers.IO) {
            apiClient.send(
                ApiRequest(
                    path = "sms/threads/$threadId",
                    method = HttpMethod.GET,
                    requiresAuth = true
                )
            )
        }
    }

    suspend fun sendSms(
        to: String,
        body: String? = null,
        mediaUrls: List<String> = emptyList()
    ): SendSmsResponse {
        val bodyJson =
            json.encodeToString(
                SendSmsRequest(
                    to = to,
                    body = body,
                    mediaUrls = mediaUrls
                )
            )

        return withContext(Dispatchers.IO) {
            apiClient.send(
                ApiRequest(
                    path = "sms/send",
                    method = HttpMethod.POST,
                    bodyJson = bodyJson,
                    requiresAuth = true
                )
            )
        }
    }
    suspend fun markReadBulk(ids: List<Int>) {
        if (ids.isEmpty()) return

        val bodyJson = json.encodeToString(
            ReadBulkRequest(ids = ids)
        )

        withContext(Dispatchers.IO) {
            apiClient.sendRaw(
                ApiRequest(
                    path = "messages/read-bulk",
                    method = HttpMethod.POST,
                    bodyJson = bodyJson,
                    requiresAuth = true
                )
            )
        }
    }

    suspend fun loadDeltas(roomId: Int, sinceId: Int): List<MessageDto> {
        val response: MessagesResponse =
            withContext(Dispatchers.IO) {
                apiClient.send(
                    ApiRequest(
                        path = "messages/$roomId/deltas?sinceId=$sinceId",
                        method = HttpMethod.GET,
                        requiresAuth = true
                    )
                )
            }

        return response.items
    }

    suspend fun deleteMessage(
        messageId: Int,
        deleteForEveryone: Boolean
    ) {
        val scope = if (deleteForEveryone) "all" else "me"

        withContext(Dispatchers.IO) {
            apiClient.sendRaw(
                ApiRequest(
                    path = "messages/$messageId?scope=$scope",
                    method = HttpMethod.DELETE,
                    requiresAuth = true
                )
            )
        }
    }

    suspend fun editMessage(
        messageId: Int,
        text: String,
        attachments: List<AttachmentDto> = emptyList()
    ): MessageDto? {
        val bodyJson =
            json.encodeToString(
                EditMessageRequest(
                    content = text,
                    attachments = attachments
                )
            )

        val responseText =
            withContext(Dispatchers.IO) {
                apiClient.sendRaw(
                    ApiRequest(
                        path = "messages/$messageId",
                        method = HttpMethod.PATCH,
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

    suspend fun reportMessage(
        messageId: Int,
        reason: String,
        details: String?,
        contextCount: Int,
        blockAfterReport: Boolean
    ): ReportMessageResponse {
        val bodyJson =
            json.encodeToString(
                ReportMessageRequest(
                    messageId = messageId,
                    reason = reason,
                    details = details?.trim()?.takeIf { it.isNotBlank() },
                    contextCount = contextCount,
                    blockAfterReport = blockAfterReport
                )
            )

        return withContext(Dispatchers.IO) {
            apiClient.send(
                ApiRequest(
                    path = "reports",
                    method = HttpMethod.POST,
                    bodyJson = bodyJson,
                    requiresAuth = true
                )
            )
        }
    }
}

@Serializable
data class ReadBulkRequest(
    val ids: List<Int>
)

@Serializable
data class EditMessageRequest(
    val content: String,
    val attachments: List<AttachmentDto> = emptyList()
)

@Serializable
data class ReportMessageRequest(
    val messageId: Int,
    val reason: String,
    val details: String? = null,
    val contextCount: Int = 10,
    val blockAfterReport: Boolean = true
)

@Serializable
data class ReportMessageResponse(
    val success: Boolean
)