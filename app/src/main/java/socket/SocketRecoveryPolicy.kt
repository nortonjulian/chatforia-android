
package com.chatforia.android.socket

internal fun shouldRunScheduledSocketRecovery(
    hasToken: Boolean,
    networkValidated: Boolean,
    socketConnected: Boolean
): Boolean {
    return hasToken &&
        networkValidated &&
        !socketConnected
}


internal class SocketRecoveryPolicy(
    private val failureThreshold: Int = 3,
    private val recoveryCooldownMs: Long = 15_000,
    private val clock: () -> Long = {
        System.currentTimeMillis()
    }
) {
    private var networkLost = false
    private var consecutiveFailures = 0
    private var lastRecoveryAt: Long? = null

    @Synchronized
    fun markNetworkLost() {
        networkLost = true
        consecutiveFailures = 0
    }

    @Synchronized
    fun markConnected() {
        networkLost = false
        consecutiveFailures = 0
    }

    @Synchronized
    fun shouldRecoverForValidatedNetwork(): Boolean {
        if (!networkLost) return false

        networkLost = false
        return claimRecovery()
    }

    @Synchronized
    fun shouldRecoverAfterFailure(
        networkValidated: Boolean
    ): Boolean {
        if (!networkValidated) {
            consecutiveFailures = 0
            return false
        }

        consecutiveFailures += 1

        if (
            consecutiveFailures <
            failureThreshold
        ) {
            return false
        }

        consecutiveFailures = 0
        return claimRecovery()
    }

    @Synchronized
    fun reset() {
        networkLost = false
        consecutiveFailures = 0
        lastRecoveryAt = null
    }

    private fun claimRecovery(): Boolean {
        val now = clock()
        val last = lastRecoveryAt

        if (
            last != null &&
            now - last < recoveryCooldownMs
        ) {
            return false
        }

        lastRecoveryAt = now
        return true
    }
}
