package com.chatforia.android.messages

import com.chatforia.android.crypto.EncryptedMessagePayloadForUser

interface MessageQueueRepository {

    suspend fun loadRoomParticipants(
        roomId: Int
    ): List<RoomParticipantDto>

    suspend fun translateMessagePreview(
        roomId: Int,
        text: String,
        targetLangs: List<String>
    ): Map<String, String>

    suspend fun sendQueuedMessage(
        roomId: Int,
        clientMessageId: String,
        attachmentsInline: List<AttachmentDto>,
        encryptedPayloads: Map<String, EncryptedMessagePayloadForUser>?
    ): MessageDto?
}