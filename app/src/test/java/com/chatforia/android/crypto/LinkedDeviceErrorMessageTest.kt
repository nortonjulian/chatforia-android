package com.chatforia.android.crypto

import com.chatforia.android.network.ApiException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkedDeviceErrorMessageTest {
    @Test
    fun revokedDeviceShowsHumanReadableTextWithoutJson() {
        val text = linkedDeviceErrorMessage(
            ApiException(409, "{\"code\":\"DEVICE_REVOKED\",\"error\":\"revoked\"}")
        )
        assertTrue(text.contains("removed from your Chatforia account"))
        assertFalse(text.contains("HTTP 409"))
        assertFalse(text.contains("DEVICE_REVOKED"))
    }

    @Test
    fun missingApprovalDoesNotShowRaw404() {
        val text = linkedDeviceErrorMessage(
            ApiException(404, "{\"error\":\"Device not found\"}")
        )
        assertTrue(text.contains("No active approval request"))
        assertFalse(text.contains("HTTP 404"))
    }
}
