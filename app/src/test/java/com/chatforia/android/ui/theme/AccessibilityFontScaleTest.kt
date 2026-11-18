package com.chatforia.android.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class AccessibilityFontScaleTest {
    @Test fun usesNormalScaleForMediumOrUnknown() {
        assertEquals(1.0f, accessibilityFontScaleMultiplier("md"), 0.0001f)
        assertEquals(1.0f, accessibilityFontScaleMultiplier(null), 0.0001f)
        assertEquals(1.0f, accessibilityFontScaleMultiplier("unknown"), 0.0001f)
    }

    @Test fun offersDistinctSmallLargeAndExtraLargeSizes() {
        assertEquals(0.9f, accessibilityFontScaleMultiplier("sm"), 0.0001f)
        assertEquals(1.2f, accessibilityFontScaleMultiplier("lg"), 0.0001f)
        assertEquals(1.4f, accessibilityFontScaleMultiplier("xl"), 0.0001f)
    }
}
