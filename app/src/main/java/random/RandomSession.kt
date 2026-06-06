package com.chatforia.android.random

data class RandomSession(
    val roomId: Int,
    val myAlias: String,
    val partnerAlias: String,
    val iRequestedFriend: Boolean = false,
    val partnerRequestedFriend: Boolean = false
) {
    val isFriendUnlocked: Boolean
        get() = iRequestedFriend && partnerRequestedFriend
}