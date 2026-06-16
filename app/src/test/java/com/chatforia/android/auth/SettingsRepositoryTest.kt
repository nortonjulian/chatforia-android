package com.chatforia.android.auth

import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.ApiTransport
import com.chatforia.android.network.HttpMethod
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun updateUsername_patchesUsersMeWithUsername() =
        runTest {
            val api = FakeApiTransport()

            api.enqueueResponse(
                userJson(
                    id = 1,
                    username = "new_username"
                )
            )

            val repository =
                SettingsRepository(
                    apiClient = api,
                    ioDispatcher = UnconfinedTestDispatcher(testScheduler)
                )

            val user =
                repository.updateUsername("new_username")

            assertEquals(1, user.id)
            assertEquals("new_username", user.username)

            val request = api.requests.single()

            assertEquals("users/me", request.path)
            assertEquals(HttpMethod.PATCH, request.method)
            assertTrue(request.requiresAuth)

            val body = parseBody(request)

            assertEquals("new_username", body["username"]?.jsonPrimitive?.content)
        }

    @Test
    fun updateSettings_patchesUsersMeWithAllSettings() =
        runTest {
            val api = FakeApiTransport()

            api.enqueueResponse(
                userJson(
                    id = 2,
                    username = "settings_user"
                )
            )

            val repository =
                SettingsRepository(
                    apiClient = api,
                    ioDispatcher = UnconfinedTestDispatcher(testScheduler)
                )

            val user =
                repository.updateSettings(
                    SettingsUpdateRequest(
                        preferredLanguage = "es",
                        autoTranslate = true,
                        showOriginalWithTranslation = true,
                        theme = "midnight",
                        allowExplicitContent = true,
                        showReadReceipts = true,
                        autoDeleteSeconds = 3600,
                        privacyBlurEnabled = true,
                        privacyBlurOnUnfocus = true,
                        privacyHoldToReveal = true,
                        notifyOnCopy = true,
                        ageBand = "ADULT_25_34",
                        wantsAgeFilter = false,
                        randomChatAllowedBands = listOf("ADULT_18_24", "ADULT_25_34"),
                        riaRemember = false,
                        voicemailEnabled = false,
                        voicemailAutoDeleteDays = 14,
                        voicemailForwardEmail = "voice@example.com",
                        voicemailGreetingText = "Leave a message.",
                        uiLanguage = "es",
                        messageTone = "Ping.mp3",
                        ringtone = "Ring.mp3",
                        enableSmartReplies = false,
                        maskAIProfanity = true,
                        soundVolume = 55
                    )
                )

            assertEquals(2, user.id)

            val request = api.requests.single()

            assertEquals("users/me", request.path)
            assertEquals(HttpMethod.PATCH, request.method)
            assertTrue(request.requiresAuth)

            val body = parseBody(request)

            assertEquals("es", body["preferredLanguage"]?.jsonPrimitive?.content)
            assertEquals(true, body["autoTranslate"]?.jsonPrimitive?.boolean)
            assertEquals(true, body["showOriginalWithTranslation"]?.jsonPrimitive?.boolean)
            assertEquals("midnight", body["theme"]?.jsonPrimitive?.content)
            assertEquals(true, body["allowExplicitContent"]?.jsonPrimitive?.boolean)
            assertEquals(true, body["showReadReceipts"]?.jsonPrimitive?.boolean)
            assertEquals(3600, body["autoDeleteSeconds"]?.jsonPrimitive?.int)

            assertEquals(true, body["privacyBlurEnabled"]?.jsonPrimitive?.boolean)
            assertEquals(true, body["privacyBlurOnUnfocus"]?.jsonPrimitive?.boolean)
            assertEquals(true, body["privacyHoldToReveal"]?.jsonPrimitive?.boolean)
            assertEquals(true, body["notifyOnCopy"]?.jsonPrimitive?.boolean)

            assertEquals("ADULT_25_34", body["ageBand"]?.jsonPrimitive?.content)
            assertEquals(false, body["wantsAgeFilter"]?.jsonPrimitive?.boolean)
            assertEquals(2, body["randomChatAllowedBands"]?.jsonArray?.size)
            assertEquals(false, body["riaRemember"]?.jsonPrimitive?.boolean)

            assertEquals(false, body["voicemailEnabled"]?.jsonPrimitive?.boolean)
            assertEquals(14, body["voicemailAutoDeleteDays"]?.jsonPrimitive?.int)
            assertEquals("voice@example.com", body["voicemailForwardEmail"]?.jsonPrimitive?.content)
            assertEquals("Leave a message.", body["voicemailGreetingText"]?.jsonPrimitive?.content)

            assertEquals("es", body["uiLanguage"]?.jsonPrimitive?.content)
            assertEquals("Ping.mp3", body["messageTone"]?.jsonPrimitive?.content)
            assertEquals("Ring.mp3", body["ringtone"]?.jsonPrimitive?.content)
            assertEquals(false, body["enableSmartReplies"]?.jsonPrimitive?.boolean)
            assertEquals(true, body["maskAIProfanity"]?.jsonPrimitive?.boolean)
            assertEquals(55, body["soundVolume"]?.jsonPrimitive?.int)
        }

    @Test
    fun updateAccessibility_patchesA11yThenFetchesMe() =
        runTest {
            val api = FakeApiTransport()

            api.enqueueResponse("{}")
            api.enqueueResponse(
                """
                {
                  "user": ${userJson(id = 3, username = "a11y_user")}
                }
                """.trimIndent()
            )

            val repository =
                SettingsRepository(
                    apiClient = api,
                    ioDispatcher = UnconfinedTestDispatcher(testScheduler)
                )

            val user =
                repository.updateAccessibility(
                    AccessibilitySettingsUpdateRequest(
                        a11yUiFont = "lg",
                        a11yVisualAlerts = true,
                        a11yVibrate = true,
                        a11yFlashOnCall = true,
                        a11yLiveCaptions = true,
                        a11yVoiceNoteSTT = true,
                        a11yCaptionFont = "xl",
                        a11yCaptionBg = "light"
                    )
                )

            assertEquals(3, user.id)

            assertEquals(2, api.requests.size)

            val patchRequest = api.requests[0]
            val getRequest = api.requests[1]

            assertEquals("users/me/a11y", patchRequest.path)
            assertEquals(HttpMethod.PATCH, patchRequest.method)
            assertTrue(patchRequest.requiresAuth)

            val body = parseBody(patchRequest)

            assertEquals("lg", body["a11yUiFont"]?.jsonPrimitive?.content)
            assertEquals(true, body["a11yVisualAlerts"]?.jsonPrimitive?.boolean)
            assertEquals(true, body["a11yVibrate"]?.jsonPrimitive?.boolean)
            assertEquals(true, body["a11yFlashOnCall"]?.jsonPrimitive?.boolean)
            assertEquals(true, body["a11yLiveCaptions"]?.jsonPrimitive?.boolean)
            assertEquals(true, body["a11yVoiceNoteSTT"]?.jsonPrimitive?.boolean)
            assertEquals("xl", body["a11yCaptionFont"]?.jsonPrimitive?.content)
            assertEquals("light", body["a11yCaptionBg"]?.jsonPrimitive?.content)

            assertEquals("auth/me", getRequest.path)
            assertEquals(HttpMethod.GET, getRequest.method)
            assertTrue(getRequest.requiresAuth)
        }

    @Test
    fun deleteAccount_sendsDeleteRequest() =
        runTest {
            val api = FakeApiTransport()
            api.enqueueResponse("{}")

            val repository =
                SettingsRepository(
                    apiClient = api,
                    ioDispatcher = UnconfinedTestDispatcher(testScheduler)
                )

            repository.deleteAccount()

            val request = api.requests.single()

            assertEquals("users/me", request.path)
            assertEquals(HttpMethod.DELETE, request.method)
            assertTrue(request.requiresAuth)
        }

    @Test
    fun removeAvatar_sendsDeleteAvatarRequest() =
        runTest {
            val api = FakeApiTransport()

            api.enqueueResponse(
                """
                {
                  "avatarUrl": null
                }
                """.trimIndent()
            )

            val repository =
                SettingsRepository(
                    apiClient = api,
                    ioDispatcher = UnconfinedTestDispatcher(testScheduler)
                )

            val response = repository.removeAvatar()

            assertEquals(null, response.avatarUrl)

            val request = api.requests.single()

            assertEquals("users/me/avatar", request.path)
            assertEquals(HttpMethod.DELETE, request.method)
            assertTrue(request.requiresAuth)
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
              "preferredLanguage": "en",
              "uiLanguage": "en",
              "theme": "dawn"
            }
        """.trimIndent()
    }

    private class FakeApiTransport : ApiTransport {
        val requests = mutableListOf<ApiRequest>()

        private val responses = ArrayDeque<String>()

        fun enqueueResponse(response: String) {
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