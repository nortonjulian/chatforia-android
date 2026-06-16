package com.chatforia.android.messages

import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.ApiTransport
import com.chatforia.android.network.HttpMethod
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MessagesRepositoryTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun sendMessage_sendsPlainContentToMessagesEndpoint() {
        runTest {
            val api = FakeApiTransport()
            api.enqueueResponse(
                """
                {
                  "message": ${messageJson(
                    id = 101,
                    rawContent = "hello",
                    clientMessageId = "client-1"
                )}
                }
                """.trimIndent()
            )

            val repository = MessagesRepository(
                apiClient = api,
                ioDispatcher = UnconfinedTestDispatcher(testScheduler)
            )

            val result = repository.sendMessage(
                roomId = 10,
                text = "hello",
                clientMessageId = "client-1"
            )

            assertEquals(101, result?.id)
            assertEquals("client-1", result?.clientMessageId)

            val request = api.requests.single()

            assertEquals("messages", request.path)
            assertEquals(HttpMethod.POST, request.method)
            assertTrue(request.requiresAuth)

            val body = parseBody(request)

            println("REPORT REQUEST PATH = ${request.path}")
            println("REPORT REQUEST METHOD = ${request.method}")
            println("REPORT REQUEST BODY = $body")

            assertEquals(10, body["chatRoomId"]?.jsonPrimitive?.int)
            assertEquals("hello", body["content"]?.jsonPrimitive?.content)
            assertEquals("client-1", body["clientMessageId"]?.jsonPrimitive?.content)
        }
    }

    @Test
    fun sendMessage_withCiphertextOmitsPlainContent() {
        runTest {
            val api = FakeApiTransport()
            api.enqueueResponse(
                """
                {
                  "message": ${messageJson(
                    id = 102,
                    rawContent = "",
                    clientMessageId = "client-encrypted"
                )}
                }
                """.trimIndent()
            )

            val repository = MessagesRepository(
                apiClient = api,
                ioDispatcher = UnconfinedTestDispatcher(testScheduler)
            )

            repository.sendMessage(
                roomId = 10,
                text = "secret",
                clientMessageId = "client-encrypted",
                contentCiphertext = "ciphertext",
                encryptedKeys = mapOf("2" to "encrypted-key"),
                encryptionVersion = 1
            )

            val body = parseBody(api.requests.single())

            assertEquals(10, body["chatRoomId"]?.jsonPrimitive?.int)
            assertFalse(body.containsKey("content"))
            assertEquals("ciphertext", body["contentCiphertext"]?.jsonPrimitive?.content)
            assertEquals(1, body["encryptionVersion"]?.jsonPrimitive?.int)
            assertEquals("encrypted-key", body["encryptedKeys"]?.jsonObject?.get("2")?.jsonPrimitive?.content)
        }
    }

    @Test
    fun loadMessages_usesRoomMessagesEndpoint() {
        runTest {
            val api = FakeApiTransport()
            api.enqueueResponse(
                """
                {
                  "items": [
                    ${messageJson(id = 201, rawContent = "one", clientMessageId = "c1")}
                  ]
                }
                """.trimIndent()
            )

            val repository = MessagesRepository(
                apiClient = api,
                ioDispatcher = UnconfinedTestDispatcher(testScheduler)
            )

            val result = repository.loadMessages(roomId = 55)

            assertEquals(1, result.size)
            assertEquals(201, result.first().id)
            assertEquals("messages/55?limit=100", api.requests.single().path)
            assertEquals(HttpMethod.GET, api.requests.single().method)
        }
    }

    @Test
    fun loadDeltas_usesDeltasEndpoint() {
        runTest {
            val api = FakeApiTransport()
            api.enqueueResponse(
                """
                {
                  "items": [
                    ${messageJson(id = 301, rawContent = "delta", clientMessageId = "delta-1")}
                  ]
                }
                """.trimIndent()
            )

            val repository = MessagesRepository(
                apiClient = api,
                ioDispatcher = UnconfinedTestDispatcher(testScheduler)
            )

            val result = repository.loadDeltas(
                roomId = 77,
                sinceId = 300
            )

            assertEquals(1, result.size)
            assertEquals(301, result.first().id)
            assertEquals("messages/77/deltas?sinceId=300", api.requests.single().path)
            assertEquals(HttpMethod.GET, api.requests.single().method)
        }
    }

    @Test
    fun markReadBulk_doesNothingWhenIdsAreEmpty() {
        runTest {
            val api = FakeApiTransport()

            val repository = MessagesRepository(
                apiClient = api,
                ioDispatcher = UnconfinedTestDispatcher(testScheduler)
            )

            repository.markReadBulk(emptyList())

            assertTrue(api.requests.isEmpty())
        }
    }

    @Test
    fun markReadBulk_postsIds() {
        runTest {
            val api = FakeApiTransport()
            api.enqueueResponse("{}")

            val repository = MessagesRepository(
                apiClient = api,
                ioDispatcher = UnconfinedTestDispatcher(testScheduler)
            )

            repository.markReadBulk(listOf(1, 2, 3))

            val request = api.requests.single()

            assertEquals("messages/read-bulk", request.path)
            assertEquals(HttpMethod.POST, request.method)

            val body = parseBody(request)
            val ids = body["ids"]!!.jsonArray.map { it.jsonPrimitive.int }

            assertEquals(listOf(1, 2, 3), ids)
        }
    }

    @Test
    fun deleteMessage_usesAllScopeWhenDeleteForEveryoneIsTrue() {
        runTest {
            val api = FakeApiTransport()
            api.enqueueResponse("{}")

            val repository = MessagesRepository(
                apiClient = api,
                ioDispatcher = UnconfinedTestDispatcher(testScheduler)
            )

            repository.deleteMessage(
                messageId = 44,
                deleteForEveryone = true
            )

            val request = api.requests.single()

            assertEquals("messages/44?scope=all", request.path)
            assertEquals(HttpMethod.DELETE, request.method)
        }
    }

    @Test
    fun deleteMessage_usesMeScopeWhenDeleteForEveryoneIsFalse() {
        runTest {
            val api = FakeApiTransport()
            api.enqueueResponse("{}")

            val repository = MessagesRepository(
                apiClient = api,
                ioDispatcher = UnconfinedTestDispatcher(testScheduler)
            )

            repository.deleteMessage(
                messageId = 45,
                deleteForEveryone = false
            )

            val request = api.requests.single()

            assertEquals("messages/45?scope=me", request.path)
            assertEquals(HttpMethod.DELETE, request.method)
        }
    }

    @Test
    fun editMessage_usesEditEndpointAndPatchMethod() {
        runTest {
            val api = FakeApiTransport()
            api.enqueueResponse(
                """
                {
                  "message": ${messageJson(
                    id = 88,
                    rawContent = "edited",
                    clientMessageId = "edited-client"
                )}
                }
                """.trimIndent()
            )

            val repository = MessagesRepository(
                apiClient = api,
                ioDispatcher = UnconfinedTestDispatcher(testScheduler)
            )

            val result = repository.editMessage(
                messageId = 88,
                text = "edited"
            )

            assertEquals(88, result?.id)

            val request = api.requests.single()

            assertEquals("messages/88/edit", request.path)
            assertEquals(HttpMethod.PATCH, request.method)

            val body = parseBody(request)

            assertEquals("edited", body["newContent"]?.jsonPrimitive?.content)
        }
    }

    @Test
    fun sendSms_postsToSmsSendEndpoint() {
        runTest {
            val api = FakeApiTransport()
            api.enqueueResponse(
                """
                {
                  "ok": true,
                  "threadId": 9,
                  "provider": "twilio",
                  "messageSid": "SM123",
                  "clientRef": "client-ref"
                }
                """.trimIndent()
            )

            val repository = MessagesRepository(
                apiClient = api,
                ioDispatcher = UnconfinedTestDispatcher(testScheduler)
            )

            val result = repository.sendSms(
                to = "+15551234567",
                body = "hello sms",
                mediaUrls = listOf("https://example.com/photo.jpg")
            )

            assertTrue(result.ok)
            assertEquals(9, result.threadId)

            val request = api.requests.single()

            assertEquals("sms/send", request.path)
            assertEquals(HttpMethod.POST, request.method)

            val body = parseBody(request)

            assertEquals("+15551234567", body["to"]?.jsonPrimitive?.content)
            assertEquals("hello sms", body["body"]?.jsonPrimitive?.content)
            assertEquals(
                "https://example.com/photo.jpg",
                body["mediaUrls"]?.jsonArray?.first()?.jsonPrimitive?.content
            )
        }
    }

    @Test
    fun translateMessagePreview_returnsEmptyWithoutCallingApiWhenNoTargets() {
        runTest {
            val api = FakeApiTransport()

            val repository = MessagesRepository(
                apiClient = api,
                ioDispatcher = UnconfinedTestDispatcher(testScheduler)
            )

            val result = repository.translateMessagePreview(
                roomId = 10,
                text = "hello",
                targetLangs = emptyList()
            )

            assertTrue(result.isEmpty())
            assertTrue(api.requests.isEmpty())
        }
    }

    @Test
    fun translateMessagePreview_postsTargetsAndReturnsTranslations() {
        runTest {
            val api = FakeApiTransport()
            api.enqueueResponse(
                """
                {
                  "translations": {
                    "es": "hola",
                    "fr": "bonjour"
                  }
                }
                """.trimIndent()
            )

            val repository = MessagesRepository(
                apiClient = api,
                ioDispatcher = UnconfinedTestDispatcher(testScheduler)
            )

            val result = repository.translateMessagePreview(
                roomId = 10,
                text = "hello",
                targetLangs = listOf("es", "fr")
            )

            assertEquals("hola", result["es"])
            assertEquals("bonjour", result["fr"])

            val request = api.requests.single()

            assertEquals("translate/message-preview", request.path)
            assertEquals(HttpMethod.POST, request.method)

            val body = parseBody(request)

            assertEquals(10, body["chatRoomId"]?.jsonPrimitive?.int)
            assertEquals("hello", body["text"]?.jsonPrimitive?.content)
            assertEquals(
                listOf("es", "fr"),
                body["targetLangs"]!!.jsonArray.map { it.jsonPrimitive.content }
            )
        }
    }

    @Test
    fun reportMessage_postsReportBody() {
        runTest {
            val api = FakeApiTransport()
            api.enqueueResponse(
                """
                {
                  "success": true
                }
                """.trimIndent()
            )

            val repository = MessagesRepository(
                apiClient = api,
                ioDispatcher = UnconfinedTestDispatcher(testScheduler)
            )

            val result = repository.reportMessage(
                messageId = 500,
                reason = "spam",
                details = "   ",
                contextCount = 12,
                blockAfterReport = false
            )

            assertTrue(result.success)

            val request = api.requests.single()

            assertEquals("reports", request.path)
            assertEquals(HttpMethod.POST, request.method)

            val body = parseBody(request)

            assertEquals(500, body["messageId"]?.jsonPrimitive?.int)
            assertEquals("spam", body["reason"]?.jsonPrimitive?.content)
            assertEquals(12, body["contextCount"]?.jsonPrimitive?.int)
            assertEquals(false, body["blockAfterReport"]?.jsonPrimitive?.content.toBoolean())
            val detailsValue = body["details"]?.jsonPrimitive?.contentOrNull

            assertTrue(
                detailsValue == null || detailsValue.isBlank()
            )
        }
    }

    private fun parseBody(
        request: ApiRequest
    ) =
        json.parseToJsonElement(request.bodyJson!!).jsonObject

    private fun messageJson(
        id: Int,
        rawContent: String,
        clientMessageId: String
    ): String {
        return """
            {
              "id": $id,
              "rawContent": "$rawContent",
              "content": "$rawContent",
              "decryptedContent": "$rawContent",
              "createdAt": "2026-01-01T00:00:00Z",
              "sender": {
                "id": 1,
                "username": "sender"
              },
              "chatRoomId": 10,
              "clientMessageId": "$clientMessageId"
            }
        """.trimIndent()
    }

    private class FakeApiTransport : ApiTransport {
        val requests = mutableListOf<ApiRequest>()

        private val responses = ArrayDeque<String>()

        fun enqueueResponse(
            response: String
        ) {
            responses.addLast(response)
        }

        override fun sendRaw(
            request: ApiRequest
        ): String {
            requests.add(request)

            return if (responses.isNotEmpty()) {
                responses.removeFirst()
            } else {
                "{}"
            }
        }
    }
}