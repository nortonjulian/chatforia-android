package com.chatforia.android.random

data class RandomSession(
    val roomId: Int,
    val randomChatRoomId: Int? = null,
    val myAlias: String,
    val partnerAlias: String,
    val partnerDisplayName: String? = null,
    val relationshipStatus: String = "none",
    val iRequestedFriend: Boolean = false,
    val partnerRequestedFriend: Boolean = false
) {
    val isFriendUnlocked: Boolean
        get() = iRequestedFriend && partnerRequestedFriend

    val isAlreadyFriend: Boolean
        get() = relationshipStatus == "friends"

    val displayName: String
        get() {
            val alias = partnerAlias
                .takeIf { it.isNotBlank() }
                ?: "Someone"

            val realName = partnerDisplayName
                ?.takeIf { it.isNotBlank() }

            return if (isAlreadyFriend || isFriendUnlocked) {
                realName ?: alias
            } else {
                alias
            }
        }
}