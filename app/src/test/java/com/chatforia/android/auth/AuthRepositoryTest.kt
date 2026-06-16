package com.chatforia.android.auth

import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.ApiTransport
import com.chatforia.android.network.HttpMethod
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun login_trimsIdentifierPostsLoginAndSavesToken() =
        runTest {
            val api = FakeApiTransport()
            val tokenStorage = FakeAuthTokenStorage()

            api.enqueueResponse(
                """
                {
                  "message": "ok",
                  "token": "login-token",
                  "user": ${userJson(id = 1, username = "julian")}
                }
                """.trimIndent()
            )

            val repository =
                AuthRepository(
                    apiClient = api,
                    tokenStorage = tokenStorage,
                    ioDispatcher = UnconfinedTestDispatcher(testScheduler)
                )

            val user =
                repository.login(
                    identifier = "  julian@example.com  ",
                    password = "password123"
                )

            assertEquals(1, user.id)
            assertEquals("julian", user.username)
            assertEquals("login-token", tokenStorage.token)

            val request = api.requests.single()

            assertEquals("auth/login", request.path)
            assertEquals(HttpMethod.POST, request.method)
            assertFalse(request.requiresAuth)

            val body = parseBody(request)

            assertEquals("julian@example.com", body["identifier"]?.jsonPrimitive?.content)
            assertEquals("password123", body["password"]?.jsonPrimitive?.content)
        }

    @Test
    fun loginWithGoogle_postsGoogleEndpointAndSavesToken() =
        runTest {
            val api = FakeApiTransport()
            val tokenStorage = FakeAuthTokenStorage()

            api.enqueueResponse(
                """
                {
                  "message": "ok",
                  "token": "google-token",
                  "user": ${userJson(id = 2, username = "google_user")}
                }
                """.trimIndent()
            )

            val repository =
                AuthRepository(
                    apiClient = api,
                    tokenStorage = tokenStorage,
                    ioDispatcher = UnconfinedTestDispatcher(testScheduler)
                )

            val user =
                repository.loginWithGoogle(
                    idToken = "google-id-token"
                )

            assertEquals(2, user.id)
            assertEquals("google-token", tokenStorage.token)

            val request = api.requests.single()

            assertEquals("auth/oauth/google/android", request.path)
            assertEquals(HttpMethod.POST, request.method)
            assertFalse(request.requiresAuth)

            val body = parseBody(request)

            assertEquals("google-id-token", body["idToken"]?.jsonPrimitive?.content)
        }

    @Test
    fun fetchMe_getsCurrentUser() =
        runTest {
            val api = FakeApiTransport()
            val tokenStorage = FakeAuthTokenStorage()

            api.enqueueResponse(
                """
                {
                  "user": ${userJson(id = 3, username = "me_user")}
                }
                """.trimIndent()
            )

            val repository =
                AuthRepository(
                    apiClient = api,
                    tokenStorage = tokenStorage,
                    ioDispatcher = UnconfinedTestDispatcher(testScheduler)
                )

            val user = repository.fetchMe()

            assertEquals(3, user.id)
            assertEquals("me_user", user.username)

            val request = api.requests.single()

            assertEquals("auth/me", request.path)
            assertEquals(HttpMethod.GET, request.method)
            assertTrue(request.requiresAuth)
        }

    @Test
    fun bootstrap_returnsNullWhenNoTokenAndDoesNotCallApi() =
        runTest {
            val api = FakeApiTransport()
            val tokenStorage = FakeAuthTokenStorage()

            val repository =
                AuthRepository(
                    apiClient = api,
                    tokenStorage = tokenStorage,
                    ioDispatcher = UnconfinedTestDispatcher(testScheduler)
                )

            val result = repository.bootstrap()

            assertEquals(null, result)
            assertTrue(api.requests.isEmpty())
            assertFalse(tokenStorage.clearCalled)
        }

    @Test
    fun bootstrap_fetchesMeWhenTokenExists() =
        runTest {
            val api = FakeApiTransport()
            val tokenStorage = FakeAuthTokenStorage()
            tokenStorage.save("existing-token")

            api.enqueueResponse(
                """
                {
                  "user": ${userJson(id = 4, username = "boot_user")}
                }
                """.trimIndent()
            )

            val repository =
                AuthRepository(
                    apiClient = api,
                    tokenStorage = tokenStorage,
                    ioDispatcher = UnconfinedTestDispatcher(testScheduler)
                )

            val result = repository.bootstrap()

            assertEquals(4, result?.id)
            assertEquals("auth/me", api.requests.single().path)
            assertFalse(tokenStorage.clearCalled)
        }

    @Test
    fun bootstrap_clearsTokenWhenFetchMeFails() =
        runTest {
            val api = FakeApiTransport()
            val tokenStorage = FakeAuthTokenStorage()
            tokenStorage.save("bad-token")

            api.shouldThrow = true

            val repository =
                AuthRepository(
                    apiClient = api,
                    tokenStorage = tokenStorage,
                    ioDispatcher = UnconfinedTestDispatcher(testScheduler)
                )

            val result = repository.bootstrap()

            assertEquals(null, result)
            assertEquals(null, tokenStorage.token)
            assertTrue(tokenStorage.clearCalled)
        }

    @Test
    fun rotateEncryptionKey_postsPublicKey() =
        runTest {
            val api = FakeApiTransport()
            val tokenStorage = FakeAuthTokenStorage()
            api.enqueueResponse("{}")

            val repository =
                AuthRepository(
                    apiClient = api,
                    tokenStorage = tokenStorage,
                    ioDispatcher = UnconfinedTestDispatcher(testScheduler)
                )

            repository.rotateEncryptionKey("public-key-123")

            val request = api.requests.single()

            assertEquals("auth/keys/rotate", request.path)
            assertEquals(HttpMethod.POST, request.method)
            assertTrue(request.requiresAuth)

            val body = parseBody(request)

            assertEquals("public-key-123", body["publicKey"]?.jsonPrimitive?.content)
        }

    @Test
    fun resendVerificationEmail_trimsEmailAndPostsRequest() =
        runTest {
            val api = FakeApiTransport()
            val tokenStorage = FakeAuthTokenStorage()
            api.enqueueResponse("{}")

            val repository =
                AuthRepository(
                    apiClient = api,
                    tokenStorage = tokenStorage,
                    ioDispatcher = UnconfinedTestDispatcher(testScheduler)
                )

            repository.resendVerificationEmail("  user@example.com  ")

            val request = api.requests.single()

            assertEquals("auth/resend-email", request.path)
            assertEquals(HttpMethod.POST, request.method)
            assertFalse(request.requiresAuth)

            val body = parseBody(request)

            assertEquals("user@example.com", body["email"]?.jsonPrimitive?.content)
        }

    @Test
    fun forgotPassword_trimsIdentifierAndPostsRequest() =
        runTest {
            val api = FakeApiTransport()
            val tokenStorage = FakeAuthTokenStorage()
            api.enqueueResponse("{}")

            val repository =
                AuthRepository(
                    apiClient = api,
                    tokenStorage = tokenStorage,
                    ioDispatcher = UnconfinedTestDispatcher(testScheduler)
                )

            repository.forgotPassword("  julian@example.com  ")

            val request = api.requests.single()

            assertEquals("auth/forgot-password", request.path)
            assertEquals(HttpMethod.POST, request.method)
            assertFalse(request.requiresAuth)

            val body = parseBody(request)

            assertEquals("julian@example.com", body["identifier"]?.jsonPrimitive?.content)
        }

    @Test
    fun register_trimsFieldsAndIncludesSmsConsentWhenPhoneExists() =
        runTest {
            val api = FakeApiTransport()
            val tokenStorage = FakeAuthTokenStorage()

            api.enqueueResponse(
                """
                {
                  "message": "registered",
                  "token": "register-token",
                  "user": ${userJson(id = 5, username = "new_user")}
                }
                """.trimIndent()
            )

            val repository =
                AuthRepository(
                    apiClient = api,
                    tokenStorage = tokenStorage,
                    ioDispatcher = UnconfinedTestDispatcher(testScheduler)
                )

            val response =
                repository.register(
                    username = "  new_user  ",
                    email = "  new@example.com  ",
                    password = "password123",
                    phone = "  +15551234567  ",
                    smsConsent = true
                )

            assertEquals("register-token", response.token)
            assertEquals(5, response.resolvedUser?.id)

            val request = api.requests.single()

            assertEquals("auth/register", request.path)
            assertEquals(HttpMethod.POST, request.method)
            assertFalse(request.requiresAuth)

            val body = parseBody(request)

            assertEquals("new_user", body["username"]?.jsonPrimitive?.content)
            assertEquals("new@example.com", body["email"]?.jsonPrimitive?.content)
            assertEquals("password123", body["password"]?.jsonPrimitive?.content)
            assertEquals("+15551234567", body["phone"]?.jsonPrimitive?.content)
            assertEquals(true, body["smsConsent"]?.jsonPrimitive?.content.toBoolean())
        }

    @Test
    fun register_omitsPhoneAndSmsConsentWhenPhoneBlank() =
        runTest {
            val api = FakeApiTransport()
            val tokenStorage = FakeAuthTokenStorage()

            api.enqueueResponse(
                """
                {
                  "message": "registered",
                  "id": 6,
                  "username": "no_phone",
                  "email": "no_phone@example.com"
                }
                """.trimIndent()
            )

            val repository =
                AuthRepository(
                    apiClient = api,
                    tokenStorage = tokenStorage,
                    ioDispatcher = UnconfinedTestDispatcher(testScheduler)
                )

            val response =
                repository.register(
                    username = "no_phone",
                    email = "no_phone@example.com",
                    password = "password123",
                    phone = "   ",
                    smsConsent = true
                )

            assertEquals(6, response.resolvedUser?.id)
            assertEquals("no_phone", response.resolvedUser?.username)

            val body = parseBody(api.requests.single())

            assertFalse(body.containsKey("phone"))
            assertFalse(body.containsKey("smsConsent"))
        }

    @Test
    fun saveExternalToken_savesToken() {
        val api = FakeApiTransport()
        val tokenStorage = FakeAuthTokenStorage()

        val repository =
            AuthRepository(
                apiClient = api,
                tokenStorage = tokenStorage
            )

        repository.saveExternalToken("external-token")

        assertEquals("external-token", tokenStorage.token)
    }

    @Test
    fun logout_clearsToken() {
        val api = FakeApiTransport()
        val tokenStorage = FakeAuthTokenStorage()
        tokenStorage.save("token-to-clear")

        val repository =
            AuthRepository(
                apiClient = api,
                tokenStorage = tokenStorage
            )

        repository.logout()

        assertEquals(null, tokenStorage.token)
        assertTrue(tokenStorage.clearCalled)
    }

    private fun parseBody(
        request: ApiRequest
    ) =
        json.parseToJsonElement(request.bodyJson!!).jsonObject

    private fun userJson(
        id: Int,
        username: String
    ): String {
        return """
            {
              "id": $id,
              "email": "$username@example.com",
              "username": "$username",
              "publicKey": "public-key-$id",
              "preferredLanguage": "en",
              "uiLanguage": "en",
              "theme": "dawn"
            }
        """.trimIndent()
    }

    private class FakeAuthTokenStorage : AuthTokenStorage {
        var token: String? = null
        var clearCalled = false

        override fun save(token: String) {
            this.token = token
        }

        override fun read(): String? {
            return token
        }

        override fun clear() {
            token = null
            clearCalled = true
        }
    }

    private class FakeApiTransport : ApiTransport {
        val requests = mutableListOf<ApiRequest>()

        private val responses = ArrayDeque<String>()

        var shouldThrow = false

        fun enqueueResponse(response: String) {
            responses.addLast(response)
        }

        override fun sendRaw(
            request: ApiRequest
        ): String {
            requests.add(request)

            if (shouldThrow) {
                throw Exception("API boom")
            }

            return if (responses.isNotEmpty()) {
                responses.removeFirst()
            } else {
                "{}"
            }
        }
    }
}