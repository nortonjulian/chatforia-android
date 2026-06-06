package com.chatforia.android.calls

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CallDto(
    val id: Int,
    val mode: String? = null,
    val status: String? = null,
    val direction: String? = null,
    val callerId: Int? = null,
    val calleeId: Int? = null,
    val phoneNumber: String? = null,
    val displayName: String? = null,
    val roomName: String? = null,
    val durationSec: Int? = null,
    val startedAt: String? = null,
    val answeredAt: String? = null,
    val endedAt: String? = null,
    val createdAt: String? = null
)

@Serializable
data class CallsResponse(
    val items: List<CallDto> = emptyList(),
    val calls: List<CallDto> = emptyList(),
    val nextCursor: String? = null
) {
    val resolvedItems: List<CallDto>
        get() = if (items.isNotEmpty()) items else calls
}

@Serializable
data class CreateCallRequest(
    val calleeId: Int,
    val mode: String
)

@Serializable
data class StartExternalCallRequest(
    val phoneNumber: String,
    val mode: String = "AUDIO"
)

@Serializable
data class EndCallRequest(
    val callId: Int,
    val reason: String? = null,
    val durationSec: Int? = null
)

@Serializable
data class CreateCallResponse(
    val callId: Int? = null,
    val id: Int? = null
) {
    val resolvedCallId: Int
        get() = callId ?: id ?: error("Missing call id")
}

@Serializable
data class VideoStartRequest(
    val calleeId: Int,
    val chatRoomId: Int? = null
)

@Serializable
data class VideoStartResponse(
    val ok: Boolean? = null,
    val callId: Int,
    val roomName: String
)

@Serializable
data class VideoTokenRequest(
    val identity: String,
    val room: String
)

@Serializable
data class VideoTokenResponse(
    val token: String
)

@Serializable
data class VoiceTokenResponse(
    val token: String
)

@Serializable
data class IncomingCallPayload(
    val callId: Int? = null,
    val callerId: Int? = null,
    val callerName: String? = null,
    val fromNumber: String? = null,
    val mode: String? = null,
    val roomName: String? = null
)