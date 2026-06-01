package com.chatforia.android.chats

import kotlinx.serialization.Serializable

@Serializable
data class ConversationDto(
    val kind: String,
    val id: Int? = null,
    val title: String,
    val displayName: String? = null,
    val updatedAt: String? = null,
    val isGroup: Boolean? = null,
    val phone: String? = null,
    val unreadCount: Int? = null,
    val avatarUsers: List<ConversationAvatarUserDto>? = null,
    val last: ConversationLastDto? = null
) {
    val uniqueId: String
        get() =
            if (id != null) {
                "$kind-$id"
            } else {
                "$kind-draft-${phone ?: title}"
            }
}

@Serializable
data class ConversationAvatarUserDto(
    val id: Int,
    val username: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null
)

@Serializable
data class ConversationLastDto(
    val text: String? = null,
    val messageId: Int? = null,
    val at: String? = null,
    val hasMedia: Boolean? = null,
    val mediaCount: Int? = null,
    val mediaKinds: List<String>? = null,
    val thumbUrl: String? = null,
    val senderName: String? = null
)

@Serializable
data class ConversationsResponse(
    val items: List<ConversationDto>? = null,
    val conversations: List<ConversationDto>? = null
)