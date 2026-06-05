package com.chatforia.android.messages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SmsParticipantDto(
    @SerialName("id")
    val rawId: Int? = null,
    val phone: String? = null,
    val createdAt: String? = null
) {
    val stableId: String
        get() =
            when {
                rawId != null -> "participant-$rawId"
                !phone.isNullOrBlank() -> "participant-$phone"
                else -> "participant-unknown"
            }
}