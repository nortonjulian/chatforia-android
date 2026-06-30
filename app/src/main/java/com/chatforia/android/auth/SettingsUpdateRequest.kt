package com.chatforia.android.auth

import kotlinx.serialization.Serializable

@Serializable
data class SettingsUpdateRequest(
    val preferredLanguage: String? = null,
    val autoTranslate: Boolean = false,
    val showOriginalWithTranslation: Boolean = false,
    val theme: String? = "Dawn",
    val allowExplicitContent: Boolean = false,
    val showReadReceipts: Boolean = false,
    val autoDeleteSeconds: Int = 0,
    val discoverability: String? = null,

    val privacyBlurEnabled: Boolean = false,
    val privacyBlurOnUnfocus: Boolean = false,
    val privacyHoldToReveal: Boolean = false,
    val notifyOnCopy: Boolean = false,

    val ageBand: String? = null,
    val wantsAgeFilter: Boolean = true,
    val randomChatAllowedBands: List<String> = emptyList(),
    val riaRemember: Boolean = true,

    val voicemailEnabled: Boolean = true,
    val voicemailAutoDeleteDays: Int? = null,
    val voicemailForwardEmail: String = "",
    val voicemailGreetingText: String = "",

    val uiLanguage: String? = null,

    val messageTone: String? = "Default.mp3",
    val ringtone: String? = "Classic.mp3",

    val enableSmartReplies: Boolean = true,
    val maskAIProfanity: Boolean? = false,

    val soundVolume: Int? = 70
)