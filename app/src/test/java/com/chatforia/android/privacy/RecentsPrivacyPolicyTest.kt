package com.chatforia.android.privacy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentsPrivacyPolicyTest {
    @Test fun signedInWithBlurOnProtectsTaskPreview() {
        assertTrue(shouldProtectRecents(signedIn = true, blurWhenUnfocused = true))
    }

    @Test fun settingOffAllowsNormalTaskPreview() {
        assertFalse(shouldProtectRecents(signedIn = true, blurWhenUnfocused = false))
    }

    @Test fun loggedOutUserDoesNotRetainPreviousPrivacyFlag() {
        assertFalse(shouldProtectRecents(signedIn = false, blurWhenUnfocused = true))
    }
}
