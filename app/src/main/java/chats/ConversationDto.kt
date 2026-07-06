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
    val last: ConversationLastDto? = null,

    val isRandomChat: Boolean? = null,
    val randomChatRoomId: Int? = null,
    val randomChat: RandomChatDto? = null
) {
    val uniqueId: String
        get() =
            if (id != null) {
                "$kind-$id"
            } else {
                "$kind-draft-${phone ?: title}"
            }

    val isTemporaryRandomChat: Boolean
        get() =
            kind.equals("chat", ignoreCase = true) &&
                    (
                            isRandomChat == true ||
                                    randomChatRoomId != null ||
                                    randomChat != null
                            ) &&
                    randomChat?.endedAt == null &&
                    randomChat?.unlockedAt == null

    val randomAwareTitle: String
        get() =
            randomChat?.partnerAlias?.takeIf { it.isNotBlank() }
                ?: displayName?.takeIf { it.isNotBlank() }
                ?: title
}

@Serializable
data class RandomChatDto(
    val id: Int? = null,
    val chatRoomId: Int? = null,
    val myAlias: String? = null,
    val partnerAlias: String? = null,
    val aliasByUser: Map<String, String>? = null,
    val unlockedAt: String? = null,
    val endedAt: String? = null
)

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