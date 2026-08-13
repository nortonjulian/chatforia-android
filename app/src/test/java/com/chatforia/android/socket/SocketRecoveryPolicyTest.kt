package com.chatforia.android.socket

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocketRecoveryPolicyTest {

    @Test
    fun validatedNetworkAfterLossClaimsOneRecovery() {
        val policy =
            SocketRecoveryPolicy(
                clock = { 1_000L }
            )

        policy.markNetworkLost()

        assertTrue(
            policy.shouldRecoverForValidatedNetwork()
        )

        assertFalse(
            policy.shouldRecoverForValidatedNetwork()
        )
    }

    @Test
    fun repeatedFailuresOnValidatedNetworkClaimRecovery() {
        var now = 1_000L

        val policy =
            SocketRecoveryPolicy(
                failureThreshold = 3,
                clock = { now }
            )

        assertFalse(
            policy.shouldRecoverAfterFailure(true)
        )

        assertFalse(
            policy.shouldRecoverAfterFailure(true)
        )

        assertTrue(
            policy.shouldRecoverAfterFailure(true)
        )

        now += 5_000L

        assertFalse(
            policy.shouldRecoverAfterFailure(true)
        )

        assertFalse(
            policy.shouldRecoverAfterFailure(true)
        )

        assertFalse(
            policy.shouldRecoverAfterFailure(true)
        )

        now += 15_000L

        assertFalse(
            policy.shouldRecoverAfterFailure(true)
        )

        assertFalse(
            policy.shouldRecoverAfterFailure(true)
        )

        assertTrue(
            policy.shouldRecoverAfterFailure(true)
        )
    }

    @Test
    fun failuresWhileOfflineDoNotAccumulate() {
        val policy =
            SocketRecoveryPolicy(
                failureThreshold = 2,
                clock = { 1_000L }
            )

        assertFalse(
            policy.shouldRecoverAfterFailure(false)
        )

        assertFalse(
            policy.shouldRecoverAfterFailure(true)
        )

        assertTrue(
            policy.shouldRecoverAfterFailure(true)
        )
    }

    @Test
    fun scheduledRecoverySkipsAlreadyConnectedSocket() {
        assertFalse(
            shouldRunScheduledSocketRecovery(
                hasToken = true,
                networkValidated = true,
                socketConnected = true
            )
        )
    }

    @Test
    fun scheduledRecoveryRunsForDisconnectedSocketOnValidatedNetwork() {
        assertTrue(
            shouldRunScheduledSocketRecovery(
                hasToken = true,
                networkValidated = true,
                socketConnected = false
            )
        )
    }


    @Test
    fun successfulConnectionResetsFailureCount() {
        val policy =
            SocketRecoveryPolicy(
                failureThreshold = 2,
                clock = { 1_000L }
            )

        assertFalse(
            policy.shouldRecoverAfterFailure(true)
        )

        policy.markConnected()

        assertFalse(
            policy.shouldRecoverAfterFailure(true)
        )

        assertTrue(
            policy.shouldRecoverAfterFailure(true)
        )
    }
}
