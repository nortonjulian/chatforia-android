package com.chatforia.android.auth

import kotlinx.serialization.Serializable

@Serializable
data class AccessibilitySettingsUpdateRequest(
    val a11yUiFont: String? = null,
    val a11yVisualAlerts: Boolean? = null,
    val a11yVibrate: Boolean? = null,
    val a11yFlashOnCall: Boolean? = null,
    val a11yLiveCaptions: Boolean? = null,
    val a11yVoiceNoteSTT: Boolean? = null,
    val a11yCaptionFont: String? = null,
    val a11yCaptionBg: String? = null,
)