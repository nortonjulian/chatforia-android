package com.chatforia.android.contacts

import kotlinx.serialization.Serializable

@Serializable
data class ContactDto(
    val id: Int,

    val alias: String? = null,
    val favorite: Boolean = false,

    val externalPhone: String? = null,
    val externalName: String? = null,

    val createdAt: String? = null,

    val userId: Int? = null,
    val user: ContactUserDto? = null
)

@Serializable
data class ContactUserDto(
    val id: Int,

    val username: String? = null,
    val avatarUrl: String? = null
)

@Serializable
data class DirectChatRoomResponse(
    val id: Int? = null,
    val chatRoomId: Int? = null,
    val roomId: Int? = null,
    val chatRoom: DirectChatRoomDto? = null
) {
    val resolvedRoomId: Int?
        get() = id ?: chatRoomId ?: roomId ?: chatRoom?.id
}

@Serializable
data class DirectChatRoomDto(
    val id: Int
)

@Serializable
data class UserLookupResponse(
    val userId: Int,
    val username: String
)

@Serializable
data class ContactsResponse(
    val items: List<ContactDto> = emptyList(),

    val nextCursor: Int? = null,

    val count: Int? = null
)