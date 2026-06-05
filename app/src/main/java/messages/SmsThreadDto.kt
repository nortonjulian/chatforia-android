package com.chatforia.android.messages

import kotlinx.serialization.Serializable

@Serializable
data class SmsThreadDto(
    val id: Int,
    val userId: Int? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val archivedAt: String? = null,
    val contactPhone: String? = null,
    val displayName: String? = null,
    val contactName: String? = null,
    val participants: List<SmsParticipantDto> = emptyList(),
    val messages: List<SmsMessageDto> = emptyList()
) {
    val resolvedTitle: String
        get() =
            displayName?.trim()?.takeIf { it.isNotEmpty() }
                ?: contactName?.trim()?.takeIf { it.isNotEmpty() }
                ?: contactPhone?.trim()?.takeIf { it.isNotEmpty() }
                ?: "SMS #$id"

    val sortedMessages: List<SmsMessageDto>
        get() =
            messages.sortedWith(
                compareBy<SmsMessageDto> { it.createdAt }
                    .thenBy { it.id }
            )
}