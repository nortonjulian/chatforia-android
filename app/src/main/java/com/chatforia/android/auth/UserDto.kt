package com.chatforia.android.auth

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: Int,
    val email: String? = null,
    val username: String? = null,
    val phone: String? = null,
    val discoverability: String? = null,

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

    val riaRemember: Boolean? = null,
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

    val a11yUiFont: String? = null,
    val a11yVisualAlerts: Boolean? = null,
    val a11yVibrate: Boolean? = null,
    val a11yFlashOnCall: Boolean? = null,
    val a11yLiveCaptions: Boolean? = null,
    val a11yVoiceNoteSTT: Boolean? = null,
    val a11yCaptionFont: String? = null,
    val a11yCaptionBg: String? = null,

    val createdAt: String? = null,
    val updatedAt: String? = null
)

private fun UserDto.normalizedPlanName(): String =
    plan
        ?.trim()
        ?.uppercase()
        ?.replace("-", "_")
        ?.replace(" ", "_")
        ?: "FREE"

fun UserDto.hasPremiumAccess(): Boolean {
    if (isAdmin == true || role?.equals("ADMIN", ignoreCase = true) == true) {
        return true
    }

    if (isPremium == true) {
        return true
    }

    val premiumPlan =
        normalizedPlanName() in
            setOf(
                "PREMIUM",
                "PREMIUM_MONTHLY",
                "PREMIUM_ANNUAL",
                "PREMIUMMONTHLY",
                "PREMIUMANNUAL"
            )

    if (!premiumPlan) {
        return false
    }

    // Backend isPremium is authoritative when supplied.
    // Older payloads without it retain the legacy Premium fallback.
    return isPremium ?: true
}

fun UserDto.isPaidPlan(): Boolean {
    if (isAdmin == true || role?.equals("ADMIN", ignoreCase = true) == true) {
        return true
    }

    val normalizedPlan = normalizedPlanName()

    if (
        normalizedPlan == "PLUS" ||
        normalizedPlan == "PLUS_MONTHLY" ||
        normalizedPlan == "PLUSMONTHLY"
    ) {
        return true
    }

    return hasPremiumAccess()
}

fun UserDto.accessPlan(): String =
    when {
        hasPremiumAccess() -> "PREMIUM"
        normalizedPlanName() in
            setOf(
                "PLUS",
                "PLUS_MONTHLY",
                "PLUSMONTHLY"
            ) -> "PLUS"
        else -> "FREE"
    }