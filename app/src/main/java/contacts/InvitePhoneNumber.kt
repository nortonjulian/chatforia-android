package com.chatforia.android.contacts

/**
 * Accept the same ordinary US number shapes as Chatforia's existing SMS
 * composer, while preferring Android's locale-aware international formatter.
 *
 * [formattedE164] is the result from PhoneNumberUtils.formatNumberToE164,
 * passed in so the remaining normalization can be unit-tested without Android.
 */
internal fun normalizeInvitePhoneNumber(
    raw: String,
    formattedE164: String?
): String? {
    val value = raw.trim()
    if (value.isEmpty()) return null
    if (!value.all {
            it in '0'..'9' || it == '+' || it == '-' ||
                it == '(' || it == ')' || it == '.' || it.isWhitespace()
        }
    ) return null

    val validE164 = Regex("^\\+[1-9][0-9]{7,14}$")
    formattedE164?.takeIf { validE164.matches(it) }?.let { return it }

    val digits = value.filter { it in '0'..'9' }
    return when {
        value.startsWith("+") && validE164.matches("+$digits") -> "+$digits"
        digits.length == 10 && !value.startsWith("+") -> "+1$digits"
        digits.length == 11 && digits.startsWith("1") && !value.startsWith("+") ->
            "+$digits"
        else -> null
    }
}
