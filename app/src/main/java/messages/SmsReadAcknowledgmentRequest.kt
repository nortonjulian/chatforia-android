package com.chatforia.android.messages

import kotlinx.serialization.Serializable

@Serializable
data class SmsReadAcknowledgmentRequest(
    val messageId: Int
)
