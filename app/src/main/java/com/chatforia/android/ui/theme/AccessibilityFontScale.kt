package com.chatforia.android.ui.theme

/**
 * Multiplier applied on top of Android's own fontScale. Keeps layout dp unchanged.
 * Unknown/null saved preferences preserve the existing appearance.
 */
internal fun accessibilityFontScaleMultiplier(value: String?): Float =
    when (value?.lowercase()) {
        "sm" -> 0.9f
        "lg" -> 1.2f
        "xl" -> 1.4f
        else -> 1.0f
    }
