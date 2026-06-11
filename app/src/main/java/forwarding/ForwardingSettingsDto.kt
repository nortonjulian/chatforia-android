package com.chatforia.android.forwarding

import kotlinx.serialization.Serializable

@Serializable
data class ForwardingSettingsDto(
    val forwardingEnabledSms: Boolean = false,
    val forwardSmsToPhone: Boolean = false,
    val forwardPhoneNumber: String = "",
    val forwardSmsToEmail: Boolean = false,
    val forwardEmail: String = "",
    val forwardingEnabledCalls: Boolean = false,
    val forwardToPhoneE164: String = "",
    val forwardQuietHoursStart: Int? = null,
    val forwardQuietHoursEnd: Int? = null
)