package com.chatforia.android.messages

import kotlinx.serialization.Serializable

@Serializable
data class RoomParticipantsResponse(
    val ownerId: Int? = null,
    val participants: List<RoomParticipantDto> = emptyList()
)

@Serializable
data class RoomParticipantDto(
    val userId: Int,
    val role: String? = null,
    val user: RoomParticipantUserDto? = null
)

@Serializable
data class RoomParticipantUserDto(
    val id: Int,
    val username: String? = null,
    val publicKey: String? = null
)