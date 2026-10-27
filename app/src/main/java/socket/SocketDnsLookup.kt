package com.chatforia.android.socket

import java.net.InetAddress
import java.net.UnknownHostException

/**
 * Retry a failed socket hostname lookup on Android's current validated network.
 *
 * This keeps the original hostname, TLS validation, and network DNS settings.
 * It never substitutes a hard-coded address or a third-party DNS provider.
 */
internal fun resolveSocketHost(
    hostname: String,
    systemLookup: (String) -> List<InetAddress>,
    validatedNetworkLookup: (String) -> List<InetAddress>?
): List<InetAddress> {
    try {
        return systemLookup(hostname)
    } catch (primaryFailure: UnknownHostException) {
        val recovered = try {
            validatedNetworkLookup(hostname)
        } catch (_: UnknownHostException) {
            null
        }

        if (!recovered.isNullOrEmpty()) return recovered

        throw primaryFailure
    }
}
