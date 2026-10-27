package com.chatforia.android.socket

import java.net.InetAddress
import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SocketDnsLookupTest {
    private val address = InetAddress.getByAddress(byteArrayOf(127, 0, 0, 1))

    @Test fun successfulSystemLookupDoesNotUseFallback() {
        var fallbackCalled = false
        val result = resolveSocketHost("api.chatforia.com",
            systemLookup = { listOf(address) },
            validatedNetworkLookup = {
                fallbackCalled = true
                null
            }
        )
        assertEquals(listOf(address), result)
        assertEquals(false, fallbackCalled)
    }

    @Test fun unknownHostUsesValidatedNetworkLookup() {
        val result = resolveSocketHost("api.chatforia.com",
            systemLookup = { throw UnknownHostException("system") },
            validatedNetworkLookup = { listOf(address) }
        )
        assertEquals(listOf(address), result)
    }

    @Test fun fallbackUnavailablePropagatesOriginalFailure() {
        val original = UnknownHostException("system")
        val thrown = assertThrows(UnknownHostException::class.java) {
            resolveSocketHost("api.chatforia.com",
                systemLookup = { throw original },
                validatedNetworkLookup = { null }
            )
        }
        assertEquals(original, thrown)
    }

    @Test fun secondLookupFailurePropagatesOriginalFailure() {
        val original = UnknownHostException("system")
        val thrown = assertThrows(UnknownHostException::class.java) {
            resolveSocketHost("api.chatforia.com",
                systemLookup = { throw original },
                validatedNetworkLookup = { throw UnknownHostException("network") }
            )
        }
        assertEquals(original, thrown)
    }
}
