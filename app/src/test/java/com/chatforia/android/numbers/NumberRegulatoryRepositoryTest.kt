package com.chatforia.android.numbers

import com.chatforia.android.network.ApiException
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.ApiTransport
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class NumberRegulatoryRepositoryTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun initialize_postsOnlyE164WhenIdentityNotProvided() =
        runTest {
            val api = RegulatoryFakeApiTransport()

            api.enqueueResponse(
                """
                {
                  "initialized": true,
                  "reused": false,
                  "requirements": {
                    "end_user": [
                      {
                        "fields": ["first_name", "last_name"]
                      }
                    ]
                  }
                }
                """.trimIndent()
            )

            val repository = NumberRegulatoryRepository(api)

            val result =
                repository.initialize(
                    e164 = "+61255550123"
                )

            assertTrue(result.initialized)

            val request = api.requests.single()

            assertEquals(
                "numbers/regulatory/initialize",
                request.path
            )

            val body =
                json.parseToJsonElement(
                    request.bodyJson
                        ?: error("Expected initialize body.")
                ).jsonObject

            assertEquals(
                setOf("e164"),
                body.keys
            )

            assertEquals(
                "+61255550123",
                body["e164"]?.jsonPrimitive?.content
            )
        }

    @Test
    fun initialize_postsIdentityAttributesWithoutTrustedServerFields() =
        runTest {
            val api = RegulatoryFakeApiTransport()

            api.enqueueResponse(
                """
                {
                  "initialized": true,
                  "profile": {
                    "endUserSid": "IT123"
                  }
                }
                """.trimIndent()
            )

            val repository = NumberRegulatoryRepository(api)

            repository.initialize(
                e164 = "+61255550124",
                endUserAttributes =
                    mapOf(
                        "first_name" to "Julian",
                        "last_name" to "Norton"
                    )
            )

            val body =
                json.parseToJsonElement(
                    api.requests.single().bodyJson
                        ?: error("Expected initialize body.")
                ).jsonObject

            assertEquals(
                setOf("e164", "endUserAttributes"),
                body.keys
            )

            assertFalse(body.containsKey("provider"))
            assertFalse(body.containsKey("country"))
            assertFalse(body.containsKey("numberType"))
            assertFalse(body.containsKey("endUserType"))
        }

    @Test
    fun initialize_decodesStructured409Response() =
        runTest {
            val api = RegulatoryFakeApiTransport()

            api.errorToThrow =
                ApiException(
                    statusCode = 409,
                    responseBody =
                        """
                        {
                          "initialized": false,
                          "reason": "missing-end-user-fields",
                          "validation": {
                            "missingFields": ["first_name", "last_name"]
                          }
                        }
                        """.trimIndent()
                )

            val repository = NumberRegulatoryRepository(api)

            val result =
                repository.initialize(
                    e164 = "+61255550125"
                )

            assertFalse(result.initialized)

            assertEquals(
                "missing-end-user-fields",
                result.reason
            )

            assertEquals(
                listOf("first_name", "last_name"),
                result.validation?.missingFields
            )
        }

    @Test
    fun documentRequirements_decodesStructured409Response() =
        runTest {
            val api = RegulatoryFakeApiTransport()

            api.errorToThrow =
                ApiException(
                    statusCode = 409,
                    responseBody =
                        """
                        {
                          "resolved": false,
                          "reason": "missing-document-fields",
                          "requiredFields": [
                            "document_number",
                            "address"
                          ]
                        }
                        """.trimIndent()
                )

            val repository = NumberRegulatoryRepository(api)

            val result =
                repository.documentRequirements(
                    e164 = "+61255550126",
                    requirementName = "identity",
                    documentType = "passport"
                )

            assertFalse(result.resolved)

            assertEquals(
                listOf("document_number", "address"),
                result.requiredFields
            )
        }

    @Test
    fun uploadDocument_passesExactMultipartInputsAndDecodesSuccess() =
        runTest {
            val api = RegulatoryFakeApiTransport()
            val documents = FakeRegulatoryDocumentTransport()

            documents.response =
                """
                {
                  "provisioned": true,
                  "reused": false
                }
                """.trimIndent()

            val repository =
                NumberRegulatoryRepository(
                    api = api,
                    documentTransport = documents
                )

            val bytes = byteArrayOf(1, 2, 3, 4)

            val result =
                repository.uploadDocument(
                    e164 = "+61255550130",
                    requirementName = "identity",
                    documentType = "passport",
                    attributes =
                        mapOf(
                            "document_number" to "ABC123"
                        ),
                    filename = "passport.pdf",
                    mimeType = "application/pdf",
                    bytes = bytes
                )

            assertTrue(result.provisioned)
            assertFalse(result.reused)

            val upload =
                documents.uploads.single()

            assertEquals("+61255550130", upload.e164)
            assertEquals("identity", upload.requirementName)
            assertEquals("passport", upload.documentType)
            assertEquals("passport.pdf", upload.filename)
            assertEquals("application/pdf", upload.mimeType)
            assertTrue(upload.bytes.contentEquals(bytes))

            val attributes =
                json.parseToJsonElement(
                    upload.attributesJson
                ).jsonObject

            assertEquals(
                setOf("document_number"),
                attributes.keys
            )

            assertEquals(
                "ABC123",
                attributes["document_number"]
                    ?.jsonPrimitive
                    ?.content
            )
        }

    @Test
    fun uploadDocument_decodesStructured409Response() =
        runTest {
            val documents = FakeRegulatoryDocumentTransport()

            documents.errorToThrow =
                ApiException(
                    statusCode = 409,
                    responseBody =
                        """
                        {
                          "provisioned": false,
                          "reason": "missing-document-fields",
                          "requiredFields": ["document_number"]
                        }
                        """.trimIndent()
                )

            val repository =
                NumberRegulatoryRepository(
                    api = RegulatoryFakeApiTransport(),
                    documentTransport = documents
                )

            val result =
                repository.uploadDocument(
                    e164 = "+61255550131",
                    requirementName = "identity",
                    documentType = "passport",
                    attributes = emptyMap(),
                    filename = "passport.pdf",
                    mimeType = "application/pdf",
                    bytes = byteArrayOf(1)
                )

            assertFalse(result.provisioned)

            assertEquals(
                "missing-document-fields",
                result.reason
            )

            assertEquals(
                listOf("document_number"),
                result.requiredFields
            )
        }

    @Test
    fun uploadDocument_non409RethrowsOriginalApiException() =
        runTest {
            val documents = FakeRegulatoryDocumentTransport()

            val original =
                ApiException(
                    statusCode = 502,
                    responseBody =
                        """
                        {
                          "error": "REGULATORY_DOCUMENT_PROVISION_FAILED"
                        }
                        """.trimIndent()
                )

            documents.errorToThrow = original

            val repository =
                NumberRegulatoryRepository(
                    api = RegulatoryFakeApiTransport(),
                    documentTransport = documents
                )

            try {
                repository.uploadDocument(
                    e164 = "+61255550132",
                    requirementName = "identity",
                    documentType = "passport",
                    attributes = emptyMap(),
                    filename = "passport.pdf",
                    mimeType = "application/pdf",
                    bytes = byteArrayOf(1)
                )

                fail("Expected original ApiException.")
            } catch (error: ApiException) {
                assertTrue(error === original)
                assertEquals(502, error.statusCode)
            }
        }

    @Test
    fun supportingDocuments_normalizesArrayAndSingletonGroups() {
        val requirements =
            json.decodeFromString<RegulatoryRequirementsDto>(
                """
                {
                  "supporting_document": [
                    [
                      {
                        "requirement_name": "identity",
                        "type": "identity",
                        "accepted_documents": [
                          {
                            "name": "Passport",
                            "type": "passport"
                          }
                        ]
                      }
                    ],
                    {
                      "requirement_name": "address",
                      "type": "address",
                      "accepted_documents": [
                        {
                          "name": "Utility Bill",
                          "type": "utility_bill"
                        }
                      ]
                    }
                  ]
                }
                """.trimIndent()
            )

        val repository =
            NumberRegulatoryRepository(
                RegulatoryFakeApiTransport()
            )

        val documents =
            repository.supportingDocumentRequirements(
                requirements
            )

        assertEquals(2, documents.size)

        assertEquals(
            "identity",
            documents[0].requirementName
        )

        assertEquals(
            "passport",
            documents[0].acceptedDocuments.single().type
        )

        assertEquals(
            "address",
            documents[1].requirementName
        )

        assertEquals(
            "utility_bill",
            documents[1].acceptedDocuments.single().type
        )
    }

    @Test
    fun submit_doesNotConsumeReservationExpired409() =
        runTest {
            val api = RegulatoryFakeApiTransport()

            val original =
                ApiException(
                    statusCode = 409,
                    responseBody =
                        """
                        {
                          "error": "REGULATORY_RESERVATION_EXPIRED",
                          "decision": "REGULATORY_RESERVATION_EXPIRED"
                        }
                        """.trimIndent()
                )

            api.errorToThrow = original

            val repository = NumberRegulatoryRepository(api)

            try {
                repository.submit(
                    e164 = "+61255550127"
                )

                fail("Expected original ApiException.")
            } catch (error: ApiException) {
                assertTrue(error === original)
                assertEquals(409, error.statusCode)
                assertTrue(
                    error.responseBody.contains(
                        "REGULATORY_RESERVATION_EXPIRED"
                    )
                )
            }
        }
}

private data class RegulatoryDocumentUploadCall(
    val e164: String,
    val requirementName: String,
    val documentType: String,
    val attributesJson: String,
    val filename: String,
    val mimeType: String,
    val bytes: ByteArray
)

private class FakeRegulatoryDocumentTransport :
    RegulatoryDocumentTransport {

    val uploads =
        mutableListOf<RegulatoryDocumentUploadCall>()

    var response: String = "{}"

    var errorToThrow: ApiException? = null

    override fun uploadRegulatoryDocumentRaw(
        e164: String,
        requirementName: String,
        documentType: String,
        attributesJson: String,
        filename: String,
        mimeType: String,
        bytes: ByteArray
    ): String {
        uploads +=
            RegulatoryDocumentUploadCall(
                e164 = e164,
                requirementName = requirementName,
                documentType = documentType,
                attributesJson = attributesJson,
                filename = filename,
                mimeType = mimeType,
                bytes = bytes
            )

        errorToThrow?.let { throw it }

        return response
    }
}

private class RegulatoryFakeApiTransport : ApiTransport {

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
