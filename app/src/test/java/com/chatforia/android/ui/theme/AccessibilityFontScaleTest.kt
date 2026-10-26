package com.chatforia.android.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class AccessibilityFontScaleTest {
    @Test fun usesNormalScaleForMediumOrUnknown() {
        assertEquals(1.0f, accessibilityFontScaleMultiplier("md"))
        assertEquals(1.0f, accessibilityFontScaleMultiplier(null))
        assertEquals(1.0f, accessibilityFontScaleMultiplier("unknown"))
    }

    @Test fun offersDistinctSmallLargeAndExtraLargeSizes() {
        assertEquals(0.9f, accessibilityFontScaleMultiplier("sm"))
        assertEquals(1.2f, accessibilityFontScaleMultiplier("lg"))
        assertEquals(1.4f, accessibilityFontScaleMultiplier("xl"))
    }
}
