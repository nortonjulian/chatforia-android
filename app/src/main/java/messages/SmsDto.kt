package com.chatforia.android.messages

import kotlinx.serialization.Serializable

@Serializable
data class SmsStartThreadRequest(
    val phone: String,
    val contactId: Int? = null
)

@Serializable
data class SendSmsRequest(
    val to: String,
    val body: String
)

@Serializable
data class SendSmsResponse(
    val ok: Boolean,
    val threadId: Int,
    val provider: String? = null,
    val messageSid: String? = null,
    val clientRef: String? = null
)
@Serializable
data class SmsStartThreadResponse(
    val id: Int,
    val contactPhone: String? = null,
    val displayName: String? = null,
    val contactName: String? = null,
    val updatedAt: String? = null
)