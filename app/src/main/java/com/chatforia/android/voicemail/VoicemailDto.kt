package com.chatforia.android.voicemail

import kotlinx.serialization.Serializable

@Serializable
enum class VoicemailTranscriptStatus {
    PENDING,
    COMPLETE,
    FAILED
}

@Serializable
data class VoicemailDto(
    val id: String,
    val callerUserId: Int? = null,
    val from: String? = null,
    val fromNumber: String? = null,
    val displayName: String? = null,
    val username: String? = null,
    val audioUrl: String? = null,
    val transcript: String? = null,
    val transcriptStatus: VoicemailTranscriptStatus =
        VoicemailTranscriptStatus.COMPLETE,
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
