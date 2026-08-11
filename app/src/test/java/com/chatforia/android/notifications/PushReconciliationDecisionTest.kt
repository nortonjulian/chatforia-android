package com.chatforia.android.notifications

import com.chatforia.android.crypto.LinkedDeviceDto
import org.junit.Assert.assertEquals
import org.junit.Test

class PushReconciliationDecisionTest {

    @Test
    fun completeWhenBackendAndTwilioAreRegistered() {
        assertEquals(
            PushReconciliationDecision.COMPLETE,
            classifyPushReconciliation(
                PushRegistrationResult.Success(
                    twilioVoiceRegistered = true
                )
            )
        )
    }

    @Test
    fun retryWhenBackendSucceededButTwilioIsPending() {
        assertEquals(
            PushReconciliationDecision.RETRY,
            classifyPushReconciliation(
                PushRegistrationResult.Success(
                    twilioVoiceRegistered = false
                )
            )
        )
    }

    @Test
    fun retryWhenRegistrationFailed() {
        assertEquals(
            PushReconciliationDecision.RETRY,
            classifyPushReconciliation(
                PushRegistrationResult.Failed(
                    message = "temporary failure"
                )
            )
        )
    }

    @Test
    fun requireUserActionForDeviceReplacement() {
        assertEquals(
            PushReconciliationDecision.USER_ACTION_REQUIRED,
            classifyPushReconciliation(
                PushRegistrationResult.ReplacementRequired(
                    existingDevices =
                        listOf(
                            LinkedDeviceDto(
                                deviceId = "old-device",
                                name = "Old Android Device",
                                platform = "Android"
                            )
                        ),
                    message = "Choose a device."
                )
            )
        )
    }
}
