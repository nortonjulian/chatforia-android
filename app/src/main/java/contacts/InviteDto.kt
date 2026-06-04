package com.chatforia.android.contacts

import kotlinx.serialization.Serializable

@Serializable
data class CreateInviteRequest(
    val targetPhone: String? = null,
    val targetEmail: String? = null,
    val channel: String = "share_link"
)

@Serializable
data class CreateInviteResponse(
    val ok: Boolean,
    val invite: PeopleInviteDto,
    val url: String
)

@Serializable
data class PeopleInviteDto(
    val id: Int? = null,
    val code: String,
    val inviterUserId: Int? = null,
    val targetPhone: String? = null,
    val targetEmail: String? = null,
    val channel: String? = null,
    val status: String = "pending",
    val acceptedByUserId: Int? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val expiresAt: String? = null
)