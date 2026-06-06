package com.chatforia.android.voicemail

import kotlinx.serialization.Serializable

@Serializable
data class VoicemailDto(
    val id: String,
    val from: String? = null,
    val fromNumber: String? = null,
    val displayName: String? = null,
    val audioUrl: String? = null,
    val transcript: String? = null,
    val durationSec: Int? = null,
    val isRead: Boolean? = null,
    val createdAt: String? = null
)

@Serializable
data class VoicemailListResponse(
    val voicemails: List<VoicemailDto> = emptyList(),
    val items: List<VoicemailDto> = emptyList()
) {
    val resolvedItems: List<VoicemailDto>
        get() = if (voicemails.isNotEmpty()) voicemails else items
}

@Serializable
data class VoicemailReadRequest(
    val isRead: Boolean
)