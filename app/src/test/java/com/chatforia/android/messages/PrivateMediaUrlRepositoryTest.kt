package com.chatforia.android.messages

import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.ApiTransport
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class PrivateMediaUrlRepositoryTest {
    @Test fun localPathRequiresAuthenticatedScopedGrant() = runTest {
        var seen: ApiRequest? = null
        val api = object : ApiTransport {
            override fun sendRaw(request: ApiRequest): String {
                seen = request
                return """{"url":"/private-media/file?token=test-capability","expiresSec":60}"""
            }
        }
        val url = PrivateMediaUrlRepository(api).authorize(42, "media/private.jpg")
        assertEquals("https://api.chatforia.com/private-media/file?token=test-capability", url)
        assertEquals("private-media/message/42?variant=file", seen?.path)
        assertTrue(seen?.requiresAuth == true)
    }

    @Test fun canonicalR2KeyRequiresParticipantGrant() = runTest {
        var seen: ApiRequest? = null
        val api = object : ApiTransport {
            override fun sendRaw(request: ApiRequest): String {
                seen = request
                return """{"url":"/private-media/file?token=scoped"}"""
            }
        }
        val key = "uploads/u123/2026/10/private.jpg"
        val url = PrivateMediaUrlRepository(api).authorize(42, key)
        assertEquals("https://api.chatforia.com/private-media/file?token=scoped", url)
        assertTrue(seen?.requiresAuth == true)
        assertFalse(isLocalPrivateMessageMedia("uploads/u123/../private.jpg"))
        assertFalse(isLocalPrivateMessageMedia("uploads/u123/%2e%2e/private.jpg"))
    }

    @Test fun cloudCdnMediaRequiresParticipantGrant() = runTest {
        var seen: ApiRequest? = null
        val api = object : ApiTransport {
            override fun sendRaw(request: ApiRequest): String {
                seen = request
                return """{"url":"/private-media/file?token=cdn-grant"}"""
            }
        }
        val url = PrivateMediaUrlRepository(api).authorize(
            42, "https://cdn.chatforia.com/uploads/u1/voice.webm"
        )
        assertEquals("https://api.chatforia.com/private-media/file?token=cdn-grant", url)
        assertEquals("private-media/message/42?variant=file", seen?.path)
        assertFalse(isLocalPrivateMessageMedia("https://cdn.chatforia.com.evil.test/uploads/u1/voice.webm"))
    }

    @Test fun externalMediaUnchangedWithoutNetworkCall() = runTest {
        val api = object : ApiTransport {
            override fun sendRaw(request: ApiRequest): String = error("unexpected request")
        }
        assertEquals(
            "https://cdn.example.com/voice.m4a",
            PrivateMediaUrlRepository(api).authorize(42, "https://cdn.example.com/voice.m4a")
        )
    }

    @Test fun localPathCannotFallbackToPublicUrlOrUntrustedResponse() = runTest {
        val api = object : ApiTransport {
            override fun sendRaw(request: ApiRequest): String =
                """{"url":"https://evil.example/media/private.jpg"}"""
        }
        assertNull(PrivateMediaUrlRepository(api).authorize(42, "media/private.jpg"))
        assertNull(PrivateMediaUrlRepository(api).authorize(0, "media/private.jpg"))
        assertFalse(isLocalPrivateMessageMedia("../media/private.jpg"))
    }

    @Test fun thumbnailRequestsUseDedicatedVariant() = runTest {
        var path = ""
        val api = object : ApiTransport {
            override fun sendRaw(request: ApiRequest): String {
                path = request.path
                return """{"url":"/private-media/file?token=thumb"}"""
            }
        }
        assertNotNull(PrivateMediaUrlRepository(api).authorize(42, "thumbs/preview.jpg", "thumb"))
        assertEquals("private-media/message/42?variant=thumb", path)
    }

    @Test fun repeatedRequestsReuseUnexpiredGrant() = runTest {
        var calls = 0
        val api = object : ApiTransport {
            override fun sendRaw(request: ApiRequest): String {
                calls++
                return """{"url":"/private-media/file?token=grant-1","expiresSec":60}"""
            }
        }

        val repo = PrivateMediaUrlRepository(api)

        val first = repo.authorize(
            284, "uploads/u68/photo.jpg", viewerId = 68
        )
        val second = repo.authorize(
            284, "uploads/u68/photo.jpg", viewerId = 68
        )

        assertEquals(first, second)
        assertEquals(1, calls)
    }

    @Test fun differentViewersNeverShareCachedGrant() = runTest {
        var calls = 0
        val api = object : ApiTransport {
            override fun sendRaw(request: ApiRequest): String {
                calls++
                return """{"url":"/private-media/file?token=grant-$calls","expiresSec":60}"""
            }
        }

        val repo = PrivateMediaUrlRepository(api)

        val first = repo.authorize(
            284, "uploads/u68/photo.jpg", viewerId = 68
        )
        val second = repo.authorize(
            284, "uploads/u68/photo.jpg", viewerId = 1
        )

        assertNotEquals(first, second)
        assertEquals(2, calls)
    }

    @Test fun expiredGrantIsRequestedAgain() = runTest {
        var calls = 0
        var clock = 1000L

        val api = object : ApiTransport {
            override fun sendRaw(request: ApiRequest): String {
                calls++
                return """{"url":"/private-media/file?token=grant-$calls","expiresSec":60}"""
            }
        }

        val repo = PrivateMediaUrlRepository(
            api = api,
            nowMillis = { clock }
        )

        val first = repo.authorize(
            284, "uploads/u68/photo.jpg", viewerId = 68
        )

        clock += 46000L

        val second = repo.authorize(
            284, "uploads/u68/photo.jpg", viewerId = 68
        )

        assertNotEquals(first, second)
        assertEquals(2, calls)
    }

    @Test fun clearedCacheRequiresNewAuthorization() = runTest {
        var calls = 0

        val api = object : ApiTransport {
            override fun sendRaw(request: ApiRequest): String {
                calls++
                return """{"url":"/private-media/file?token=grant-$calls","expiresSec":60}"""
            }
        }

        val repo = PrivateMediaUrlRepository(api)

        repo.authorize(284, "uploads/u68/photo.jpg", viewerId = 68)
        repo.clearCachedGrants()
        repo.authorize(284, "uploads/u68/photo.jpg", viewerId = 68)

        assertEquals(2, calls)
    }


    @Test fun concurrentRequestsShareSingleGrant() = runTest {
        val counter = java.util.concurrent.atomic.AtomicInteger()

        val api = object : ApiTransport {
            override fun sendRaw(request: ApiRequest): String {
                counter.incrementAndGet()
                Thread.sleep(50)
                return """{"url":"/private-media/file?token=shared","expiresSec":60}"""
            }
        }

        val repo = PrivateMediaUrlRepository(api)

        val results = (1..8).map {
            async {
                repo.authorize(
                    284,
                    "uploads/u68/photo.jpg",
                    viewerId = 68
                )
            }
        }.awaitAll()

        assertEquals(1, counter.get())
        assertEquals(1, results.toSet().size)
        assertNotNull(results.first())
    }

}
