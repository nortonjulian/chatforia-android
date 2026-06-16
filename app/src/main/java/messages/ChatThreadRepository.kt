package com.chatforia.android.messages

import com.chatforia.android.crypto.EncryptedMessagePayloadForUser

interface ChatThreadRepository : MessageQueueRepository {

    suspend fun loadMessages(
        roomId: Int
    ): List<MessageDto>

    suspend fun markReadBulk(
        ids: List<Int>
    )

    suspend fun loadDeltas(
        roomId: Int,
        sinceId: Int
    ): List<MessageDto>

    suspend fun loadSmsThread(
        threadId: Int
    ): SmsThreadDto

    suspend fun sendSms(
        to: String,
        body: String? = null,
        mediaUrls: List<String> = emptyList()
    ): SendSmsResponse

    suspend fun deleteMessage(
        messageId: Int,
        deleteForEveryone: Boolean
    )

    suspend fun editMessage(
        messageId: Int,
        text: String,
        attachments: List<AttachmentDto> = emptyList(),
        encryptedPayloads: Map<String, EncryptedMessagePayloadForUser>? = null
    ): MessageDto?

    suspend fun reportMessage(
        messageId: Int,
        reason: String,
        details: String?,
        contextCount: Int,
        blockAfterReport: Boolean
    ): ReportMessageResponse
}