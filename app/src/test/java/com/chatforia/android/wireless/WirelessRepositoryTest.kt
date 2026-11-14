package com.chatforia.android.wireless

import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.ApiTransport
import com.chatforia.android.network.HttpMethod
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class WirelessRepositoryTest {

    @Test
    fun getWirelessStatus_decodesActiveAndQueuedDataPacks() =
        runTest {
            val api = FakeApiTransport()

            api.enqueueResponse(
                """
                {
                  "mode": "INDIVIDUAL",
                  "state": "ACTIVE",
                  "low": false,
                  "exhausted": false,
                  "expired": false,
                  "source": {
                    "type": "PURCHASE",
                    "id": 42,
                    "addonKind": "chatforia_esim_local_3",
                    "totalDataMb": 3072,
                    "remainingDataMb": 2973,
                    "expiresAt": "2026-11-13T03:33:20.000Z",
                    "daysRemaining": 29
                  },
                  "queuedPacks": [
                    {
                      "id": 43,
                      "addonKind": "chatforia_esim_local_3",
                      "totalDataMb": 3072,
                      "remainingDataMb": 3072,
                      "expiresAt": "2026-11-13T19:27:49.000Z"
                    }
                  ],
                  "queuedDataMb": 3072,
                  "totalAvailableDataMb": 6045
                }
                """.trimIndent()
            )

            val repository = WirelessRepository(api)
            val result = repository.getWirelessStatus()

            assertEquals(42, result.source?.id)
            assertEquals(2973, result.source?.remainingDataMb)
            assertEquals(1, result.queuedPacks.size)
            assertEquals(43, result.queuedPacks.single().id)
            assertEquals(
                3072,
                result.queuedPacks.single().remainingDataMb
            )
            assertEquals(3072, result.queuedDataMb)
            assertEquals(6045, result.totalAvailableDataMb)

            val request = api.requests.single()

            assertEquals(
                "api/wireless/status",
                request.path
            )
            assertEquals(HttpMethod.GET, request.method)
            assertTrue(request.requiresAuth)
        }

    @Test
    fun getPurchasedPackages_decodesAllStatesAndServerReplacementEligibility() = runTest {
        val api = FakeApiTransport()
        api.enqueueResponse(
            """
            {
              "packages": [
                {"id":42,"state":"EXPIRED","providerStatus":"ACTIVE","totalDataMb":3072,"remainingDataMb":3072,"paid":true,"expiresAt":"2026-11-13T03:33:20.000Z"},
                {"id":43,"state":"AWAITING_ACTIVATION","providerStatus":"NOT_ACTIVE","totalDataMb":3072,"remainingDataMb":3072,"paid":true},
                {"id":44,"state":"AWAITING_PROVISIONING","totalDataMb":3072,"remainingDataMb":3072,"paid":true},
                {"id":45,"state":"ACTIVE","totalDataMb":5120,"remainingDataMb":4096,"paid":true},
                {"id":46,"state":"TERMINATED","totalDataMb":3072,"remainingDataMb":0,"paid":true}
              ],
              "replacementEligibility": {
                "eligible":false,
                "purchaseId":null,
                "reason":"Multiple or pending data packages may share this eSIM."
              }
            }
            """.trimIndent()
        )

        val result = WirelessRepository(api).getPurchasedPackages()

        assertEquals(listOf(42, 43, 44, 45, 46), result.packages.map { it.id })
        assertEquals("NOT_ACTIVE", result.packages[1].providerStatus)
        assertEquals(43, result.awaitingActivationPackages().single().id)
        assertEquals(3072, result.packages[0].remainingDataMb)
        assertNull(result.eligibleReplacementPurchaseId())
        assertTrue(result.replacementEligibility?.reason?.contains("Multiple") == true)
        assertEquals("api/wireless/purchases", api.requests.single().path)
        assertEquals(HttpMethod.GET, api.requests.single().method)
        assertTrue(api.requests.single().requiresAuth)
    }

    @Test
    fun statusHistoryKeepsQueuedPurchaseWhenCurrentPackageExpires() {
        val expired = WirelessStatusResponse(
            mode = "INDIVIDUAL",
            state = "EXPIRED",
            expired = true,
            source = WirelessStatusSourceDto(id = 42, remainingDataMb = 3072)
        )
        val history = WirelessPurchasesResponse(
            packages = listOf(
                WirelessPurchasedPackageDto(id = 42, state = "EXPIRED", remainingDataMb = 3072),
                WirelessPurchasedPackageDto(id = 43, state = "AWAITING_ACTIVATION", remainingDataMb = 3072)
            )
        )

        assertFalse(expired.hasUsableCurrentPack())
        assertFalse(expired.canPresentCurrentPack(history))
        assertEquals(43, history.awaitingActivationPackages().single().id)
        assertEquals(2, history.packages.size)
    }

    @Test
    fun replacementOnlyUsesExplicitBackendEligibility() {
        val active = WirelessStatusResponse(
            state = "OK",
            source = WirelessStatusSourceDto(id = 42, remainingDataMb = 2048)
        )
        val multiple = WirelessPurchasesResponse(
            replacementEligibility = EsimReplacementEligibilityDto(
                eligible = false,
                purchaseId = 42,
                reason = "Multiple packages cannot be transferred."
            )
        )
        val eligible = WirelessPurchasesResponse(
            replacementEligibility = EsimReplacementEligibilityDto(
                eligible = true,
                purchaseId = 42
            )
        )

        assertTrue(active.hasUsableCurrentPack())
        assertTrue(active.canPresentCurrentPack(null))
        assertFalse(
            active.canPresentCurrentPack(
                WirelessPurchasesResponse(
                    packages = listOf(WirelessPurchasedPackageDto(id = 42, state = "EXPIRED"))
                )
            )
        )
        assertTrue(
            active.canPresentCurrentPack(
                WirelessPurchasesResponse(
                    packages = listOf(WirelessPurchasedPackageDto(id = 42, state = "ACTIVE"))
                )
            )
        )
        assertNull(multiple.eligibleReplacementPurchaseId())
        assertEquals(42, eligible.eligibleReplacementPurchaseId())
        assertNull(WirelessPurchasesResponse().eligibleReplacementPurchaseId())
        assertNull(
            WirelessPurchasesResponse(
                replacementEligibility = EsimReplacementEligibilityDto(true, 0)
            ).eligibleReplacementPurchaseId()
        )
    }

    @Test
    fun replaceEsim_rejectsInvalidPurchaseIdBeforeNetworkCall() =
        runTest {
            val api = FakeApiTransport()
            val repository = WirelessRepository(api)

            try {
                repository.replaceEsim(
                    purchaseId = 0,
                    idempotencyKey = "replacement-request-1"
                )
                org.junit.Assert.fail(
                    "Expected IllegalArgumentException."
                )
            } catch (_: IllegalArgumentException) {
                // Expected.
            }

            assertTrue(api.requests.isEmpty())
        }

    @Test
    fun replaceEsim_rejectsBlankIdempotencyKeyBeforeNetworkCall() =
        runTest {
            val api = FakeApiTransport()
            val repository = WirelessRepository(api)

            try {
                repository.replaceEsim(
                    purchaseId = 42,
                    idempotencyKey = "   "
                )
                org.junit.Assert.fail(
                    "Expected IllegalArgumentException."
                )
            } catch (_: IllegalArgumentException) {
                // Expected.
            }

            assertTrue(api.requests.isEmpty())
        }

    @Test
    fun replaceEsim_rejectsOversizedIdempotencyKeyBeforeNetworkCall() =
        runTest {
            val api = FakeApiTransport()
            val repository = WirelessRepository(api)

            try {
                repository.replaceEsim(
                    purchaseId = 42,
                    idempotencyKey = "a".repeat(201)
                )
                org.junit.Assert.fail(
                    "Expected IllegalArgumentException."
                )
            } catch (_: IllegalArgumentException) {
                // Expected.
            }

            assertTrue(api.requests.isEmpty())
        }

    @Test
    fun replaceEsim_sendsIdempotencyKeyAndDecodesInstallation() =
        runTest {
            val api = FakeApiTransport()

            api.enqueueResponse(
                """
                {
                  "ok": true,
                  "replacement": {
                    "id": "replacement-1",
                    "purchaseId": 42,
                    "status": "COMPLETED",
                    "oldIccid": "old-iccid",
                    "newIccid": "new-iccid",
                    "remainingDataMb": 2973,
                    "expiresAt": "2026-11-13T03:33:20.000Z",
                    "createdAt": "2026-10-14T20:00:00.000Z",
                    "completedAt": "2026-10-14T20:00:05.000Z",
                    "installation": {
                      "subscriberId": 35,
                      "iccid": "new-iccid",
                      "providerProfileId": "new-profile",
                      "smdp": "example.smdp.address",
                      "activationCode": "example-code",
                      "lpaUri": "LPA:1${'$'}example",
                      "qrPayload": "LPA:1${'$'}example"
                    }
                  }
                }
                """.trimIndent()
            )

            val repository = WirelessRepository(api)

            val result =
                repository.replaceEsim(
                    purchaseId = 42,
                    idempotencyKey = "  replacement-request-1  "
                )

            assertTrue(result.ok)
            assertEquals(
                "replacement-1",
                result.replacement?.id
            )
            assertEquals(
                42,
                result.replacement?.purchaseId
            )
            assertEquals(
                35,
                result.replacement
                    ?.installation
                    ?.subscriberId
            )
            assertEquals(
                "new-profile",
                result.replacement
                    ?.installation
                    ?.providerProfileId
            )

            val request = api.requests.single()

            assertEquals(
                "api/wireless/esim/42/replace",
                request.path
            )
            assertEquals(HttpMethod.POST, request.method)
            assertTrue(request.requiresAuth)
            assertEquals(
                "replacement-request-1",
                request.headers["Idempotency-Key"]
            )
            assertNull(request.bodyJson)
        }
}

private class FakeApiTransport : ApiTransport {

    val requests = mutableListOf<ApiRequest>()

    private val responses = ArrayDeque<String>()

    fun enqueueResponse(response: String) {
        responses.addLast(response)
    }

    override fun sendRaw(request: ApiRequest): String {
        requests += request

        return responses.removeFirstOrNull()
            ?: error("No fake API response queued.")
    }
}
