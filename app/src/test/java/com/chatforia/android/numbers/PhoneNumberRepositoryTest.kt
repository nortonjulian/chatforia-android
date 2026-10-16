package com.chatforia.android.numbers

import com.chatforia.android.network.ApiException
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.ApiTransport
import com.chatforia.android.network.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PhoneNumberRepositoryTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun leaseNumber_postsExactNumberAndPurchaseIntent() =
        runTest {
            val api = FakeApiTransport()

            api.enqueueResponse(
                """
                {
                  "ok": true,
                  "number": {
                    "e164": "+13035550123"
                  }
                }
                """.trimIndent()
            )

            val repository = PhoneNumberRepository(api)

            val result =
                repository.leaseNumber(
                    e164 = "+13035550123",
                    premium = true
                )

            assertTrue(result.ok)

            val request = api.requests.single()

            assertEquals("numbers/lease", request.path)
            assertEquals(HttpMethod.POST, request.method)
            assertTrue(request.requiresAuth)

            val body =
                json.parseToJsonElement(
                    request.bodyJson ?: error("Expected lease request body.")
                ).jsonObject

            assertEquals(
                "+13035550123",
                body["e164"]?.jsonPrimitive?.content
            )

            assertEquals(
                true,
                body["purchaseIntent"]?.jsonPrimitive?.boolean
            )
        }

    @Test
    fun leaseNumber_verificationRequiredThrowsRegulatoryException() =
        runTest {
            val api = FakeApiTransport()

            api.errorToThrow =
                ApiException(
                    statusCode = 409,
                    responseBody =
                        """
                        {
                          "error": "REGULATORY_COMPLIANCE_BLOCKED",
                          "decision": "VERIFICATION_REQUIRED",
                          "requiresVerification": true
                        }
                        """.trimIndent()
                )

            val repository = PhoneNumberRepository(api)

            try {
                repository.leaseNumber(
                    e164 = "+61255550123",
                    premium = false
                )

                fail("Expected NumberRegulatoryLeaseException.")
            } catch (error: NumberRegulatoryLeaseException) {
                assertEquals(
                    "VERIFICATION_REQUIRED",
                    error.response.decision
                )

                assertTrue(
                    error.response.requiresVerification == true
                )
            }
        }

    @Test
    fun leaseNumber_verificationPendingThrowsRegulatoryException() =
        runTest {
            val api = FakeApiTransport()

            api.errorToThrow =
                ApiException(
                    statusCode = 409,
                    responseBody =
                        """
                        {
                          "decision": "VERIFICATION_PENDING",
                          "requiresVerification": false
                        }
                        """.trimIndent()
                )

            val repository = PhoneNumberRepository(api)

            try {
                repository.leaseNumber(
                    e164 = "+61255550124",
                    premium = true
                )

                fail("Expected NumberRegulatoryLeaseException.")
            } catch (error: NumberRegulatoryLeaseException) {
                assertEquals(
                    "VERIFICATION_PENDING",
                    error.response.decision
                )

                assertFalse(
                    error.response.requiresVerification == true
                )
            }
        }

    @Test
    fun leaseNumber_nonActionable409RethrowsOriginalApiException() =
        runTest {
            val api = FakeApiTransport()

            val original =
                ApiException(
                    statusCode = 409,
                    responseBody =
                        """
                        {
                          "decision": "BLOCKED_UNKNOWN_NUMBER_TYPE",
                          "requiresVerification": false
                        }
                        """.trimIndent()
                )

            api.errorToThrow = original

            val repository = PhoneNumberRepository(api)

            try {
                repository.leaseNumber(
                    e164 = "+15555550125",
                    premium = false
                )

                fail("Expected original ApiException.")
            } catch (error: ApiException) {
                assertTrue(error === original)
                assertEquals(409, error.statusCode)
            }
        }

    @Test
    fun regulatoryVerificationState_retryResponsePreservesExactLeaseInputs() {
        val original =
            NumberRegulatoryVerificationState(
                e164 = "+442055550126",
                purchaseIntent = true,
                decision = "VERIFICATION_REQUIRED",
                requiresVerification = true
            )

        val updated =
            original.withLeaseResponse(
                NumberRegulatoryLeaseResponse(
                    decision = "VERIFICATION_PENDING",
                    requiresVerification = false
                )
            )

        assertEquals(original.e164, updated.e164)
        assertEquals(
            original.purchaseIntent,
            updated.purchaseIntent
        )
        assertEquals(
            "VERIFICATION_PENDING",
            updated.decision
        )
        assertFalse(updated.requiresVerification)
    }

    @Test
    fun regulatoryVerificationState_preservesExactLeaseInputs() {
        val state =
            NumberRegulatoryVerificationState(
                e164 = "+442055550126",
                purchaseIntent = true,
                decision = "VERIFICATION_REQUIRED",
                requiresVerification = true
            )

        assertEquals("+442055550126", state.e164)
        assertTrue(state.purchaseIntent)
        assertEquals(
            "VERIFICATION_REQUIRED",
            state.decision
        )
        assertTrue(state.requiresVerification)
    }
}

private class FakeApiTransport : ApiTransport {

    val requests = mutableListOf<ApiRequest>()

    private val responses = ArrayDeque<String>()

    var errorToThrow: ApiException? = null

    fun enqueueResponse(response: String) {
        responses.addLast(response)
    }

    override fun sendRaw(request: ApiRequest): String {
        requests += request

        errorToThrow?.let { throw it }

        return responses.removeFirstOrNull()
            ?: error("No fake API response queued.")
    }
}
