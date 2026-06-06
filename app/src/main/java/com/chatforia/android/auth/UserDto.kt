package com.chatforia.android.auth

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: Int,
    val email: String? = null,
    val username: String? = null,
    val phone: String? = null,

    val publicKey: String? = null,
    val privateKey: String? = null,

    val plan: String? = null,
    val role: String? = null,
    val isPremium: Boolean? = null,
    val isAdmin: Boolean? = null,

    val preferredLanguage: String? = null,
    val uiLanguage: String? = null,
    val autoTranslate: Boolean? = null,
    val showOriginalWithTranslation: Boolean? = null,
    val theme: String? = null,
    val avatarUrl: String? = null,

    val emailVerifiedAt: String? = null,
    val onboardingCompletedAt: String? = null,

    val allowExplicitContent: Boolean? = null,
    val showReadReceipts: Boolean? = null,
    val autoDeleteSeconds: Int? = null,

    val privacyBlurEnabled: Boolean? = null,
    val privacyBlurOnUnfocus: Boolean? = null,
    val privacyHoldToReveal: Boolean? = null,
    val notifyOnCopy: Boolean? = null,

    val ageBand: String? = null,
    val wantsAgeFilter: Boolean? = null,
    val randomChatAllowedBands: List<String>? = null,

    val foriaRemember: Boolean? = null,
    val enableSmartReplies: Boolean? = null,
    val smartRepliesEnabled: Boolean? = null,
    val maskAIProfanity: Boolean? = null,
    val profanityMaskEnabled: Boolean? = null,

    val tone: String? = null,
    val messageTone: String? = null,
    val messageSound: String? = null,
    val ringtone: String? = null,
    val soundVolume: Double? = null,

    val voicemailEnabled: Boolean? = null,
    val voicemailAutoDeleteDays: Int? = null,
    val voicemailForwardEmail: String? = null,
    val voicemailGreetingText: String? = null,
    val voicemailGreeting: String? = null,

    val createdAt: String? = null,
    val updatedAt: String? = null
)