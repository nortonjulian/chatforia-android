package com.chatforia.android.auth

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun load_mapsUserSettingsIntoUiState() {
        val repository = FakeUserSettingsRepository()
        val viewModel = SettingsViewModel(repository)

        viewModel.load(
            user(
                id = 1,
                preferredLanguage = "es",
                autoTranslate = true,
                showOriginalWithTranslation = true,
                theme = "dawn",
                plan = "FREE",
                allowExplicitContent = true,
                showReadReceipts = true,
                autoDeleteSeconds = 3600,
                privacyBlurEnabled = true,
                privacyBlurOnUnfocus = true,
                privacyHoldToReveal = true,
                notifyOnCopy = true,
                riaRemember = false,
                enableSmartReplies = false,
                maskAIProfanity = true,
                messageTone = "Default.mp3",
                ringtone = "Classic.mp3",
                soundVolume = 42.0,
                ageBand = "ADULT_25_34",
                wantsAgeFilter = false,
                randomChatAllowedBands = listOf("ADULT_18_24", "ADULT_25_34"),
                voicemailEnabled = false,
                voicemailAutoDeleteDays = 14,
                voicemailForwardEmail = "voice@example.com",
                voicemailGreetingText = "Leave a message.",
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

        val state = viewModel.state.value

        assertEquals("es", state.preferredLanguage)
        assertTrue(state.autoTranslate)
        assertTrue(state.showOriginalWithTranslation)
        assertEquals("dawn", state.theme)
        assertTrue(state.allowExplicitContent)
        assertTrue(state.showReadReceipts)
        assertEquals(3600, state.autoDeleteSeconds)

        assertTrue(state.privacyBlurEnabled)
        assertTrue(state.privacyBlurOnUnfocus)
        assertTrue(state.privacyHoldToReveal)
        assertTrue(state.notifyOnCopy)

        assertFalse(state.riaRemember)
        assertFalse(state.enableSmartReplies)
        assertTrue(state.maskAIProfanity)

        assertEquals("Default.mp3", state.messageTone)
        assertEquals("Classic.mp3", state.ringtone)
        assertEquals(42, state.soundVolume)

        assertEquals("ADULT_25_34", state.ageBand)
        assertFalse(state.wantsAgeFilter)
        assertEquals(listOf("ADULT_18_24", "ADULT_25_34"), state.randomChatAllowedBands)

        assertFalse(state.voicemailEnabled)
        assertEquals(14, state.voicemailAutoDeleteDays)
        assertEquals("voice@example.com", state.voicemailForwardEmail)
        assertEquals("Leave a message.", state.voicemailGreetingText)

        assertEquals("lg", state.a11yUiFont)
        assertTrue(state.a11yVisualAlerts)
        assertTrue(state.a11yVibrate)
        assertTrue(state.a11yFlashOnCall)
        assertTrue(state.a11yLiveCaptions)
        assertTrue(state.a11yVoiceNoteSTT)
        assertEquals("xl", state.a11yCaptionFont)
        assertEquals("light", state.a11yCaptionBg)
    }

    @Test
    fun load_usesFallbacksWhenUserFieldsAreNull() {
        val repository = FakeUserSettingsRepository()
        val viewModel = SettingsViewModel(repository)

        viewModel.load(
            UserDto(
                id = 2,
                email = "fallback@example.com",
                username = "fallback_user"
            )
        )

        val state = viewModel.state.value

        assertEquals("en", state.preferredLanguage)
        assertFalse(state.autoTranslate)
        assertFalse(state.showOriginalWithTranslation)
        assertEquals("dawn", state.theme)
        assertFalse(state.allowExplicitContent)
        assertFalse(state.showReadReceipts)
        assertEquals(0, state.autoDeleteSeconds)

        assertFalse(state.privacyBlurEnabled)
        assertFalse(state.privacyBlurOnUnfocus)
        assertFalse(state.privacyHoldToReveal)
        assertFalse(state.notifyOnCopy)

        assertTrue(state.riaRemember)
        assertTrue(state.enableSmartReplies)
        assertFalse(state.maskAIProfanity)

        assertEquals("Default.mp3", state.messageTone)
        assertEquals("Classic.mp3", state.ringtone)
        assertEquals(70, state.soundVolume)

        assertNull(state.ageBand)
        assertTrue(state.wantsAgeFilter)
        assertEquals(emptyList<String>(), state.randomChatAllowedBands)

        assertTrue(state.voicemailEnabled)
        assertNull(state.voicemailAutoDeleteDays)
        assertEquals("fallback@example.com", state.voicemailForwardEmail)
        assertEquals("", state.voicemailGreetingText)

        assertEquals("md", state.a11yUiFont)
        assertFalse(state.a11yVisualAlerts)
        assertFalse(state.a11yVibrate)
        assertFalse(state.a11yFlashOnCall)
        assertFalse(state.a11yLiveCaptions)
        assertFalse(state.a11yVoiceNoteSTT)
        assertEquals("lg", state.a11yCaptionFont)
        assertEquals("dark", state.a11yCaptionBg)
    }

    @Test
    fun update_changesStateImmediately() {
        val repository = FakeUserSettingsRepository()
        val viewModel = SettingsViewModel(repository)

        viewModel.update {
            it.copy(
                preferredLanguage = "fr",
                autoTranslate = true,
                riaRemember = false
            )
        }

        val state = viewModel.state.value

        assertEquals("fr", state.preferredLanguage)
        assertTrue(state.autoTranslate)
        assertFalse(state.riaRemember)
    }

    @Test
    fun save_sendsCurrentSettingsAndReportsSuccess() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeUserSettingsRepository()
            val viewModel = SettingsViewModel(repository)

            val updatedUser =
                user(
                    id = 3,
                    preferredLanguage = "de",
                    username = "saved_user"
                )

            repository.updateSettingsUser = updatedUser

            var callbackUser: UserDto? = null

            viewModel.update {
                it.copy(
                    preferredLanguage = "de",
                    autoTranslate = true,
                    showOriginalWithTranslation = true,
                    theme = "midnight",
                    allowExplicitContent = true,
                    showReadReceipts = true,
                    autoDeleteSeconds = 7200,
                    privacyBlurEnabled = true,
                    privacyBlurOnUnfocus = true,
                    privacyHoldToReveal = true,
                    notifyOnCopy = true,
                    riaRemember = false,
                    enableSmartReplies = false,
                    maskAIProfanity = true,
                    messageTone = "Ping.mp3",
                    ringtone = "Ring.mp3",
                    soundVolume = 55,
                    ageBand = "ADULT_35_49",
                    wantsAgeFilter = false,
                    randomChatAllowedBands = listOf("ADULT_25_34", "ADULT_35_49"),
                    voicemailEnabled = false,
                    voicemailAutoDeleteDays = 30,
                    voicemailForwardEmail = "voice@example.com",
                    voicemailGreetingText = "Custom greeting"
                )
            }

            viewModel.save { user ->
                callbackUser = user
            }

            advanceUntilIdle()

            val request = repository.updateSettingsRequests.single()

            assertEquals("de", request.preferredLanguage)
            assertTrue(request.autoTranslate)
            assertTrue(request.showOriginalWithTranslation)
            assertEquals("midnight", request.theme)
            assertTrue(request.allowExplicitContent)
            assertTrue(request.showReadReceipts)
            assertEquals(7200, request.autoDeleteSeconds)

            assertTrue(request.privacyBlurEnabled)
            assertTrue(request.privacyBlurOnUnfocus)
            assertTrue(request.privacyHoldToReveal)
            assertTrue(request.notifyOnCopy)

            assertFalse(request.riaRemember)
            assertFalse(request.enableSmartReplies)
            assertEquals(true, request.maskAIProfanity)

            assertEquals("Ping.mp3", request.messageTone)
            assertEquals("Ring.mp3", request.ringtone)
            assertEquals(55, request.soundVolume)

            assertEquals("ADULT_35_49", request.ageBand)
            assertFalse(request.wantsAgeFilter)
            assertEquals(listOf("ADULT_25_34", "ADULT_35_49"), request.randomChatAllowedBands)

            assertFalse(request.voicemailEnabled)
            assertEquals(30, request.voicemailAutoDeleteDays)
            assertEquals("voice@example.com", request.voicemailForwardEmail)
            assertEquals("Custom greeting", request.voicemailGreetingText)

            assertEquals("de", request.uiLanguage)

            assertEquals(updatedUser, callbackUser)

            val state = viewModel.state.value

            assertFalse(state.isSaving)
            assertEquals("Settings saved.", state.success)
            assertNull(state.error)
        }

    @Test
    fun save_failureSetsErrorAndDoesNotCallCallback() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeUserSettingsRepository()
            repository.updateSettingsShouldThrow = true

            val viewModel = SettingsViewModel(repository)

            var callbackCalled = false

            viewModel.save {
                callbackCalled = true
            }

            advanceUntilIdle()

            val state = viewModel.state.value

            assertFalse(state.isSaving)
            assertEquals("Settings boom", state.error)
            assertNull(state.success)
            assertFalse(callbackCalled)
        }

    @Test
    fun saveAccessibility_sendsAccessibilitySettingsReloadsUserAndReportsSuccess() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeUserSettingsRepository()
            val viewModel = SettingsViewModel(repository)

            val updatedUser =
                user(
                    id = 4,
                    preferredLanguage = "en",
                    username = "a11y_user",
                    a11yUiFont = "xl",
                    a11yVisualAlerts = true,
                    a11yVibrate = true,
                    a11yFlashOnCall = true,
                    a11yLiveCaptions = true,
                    a11yVoiceNoteSTT = true,
                    a11yCaptionFont = "xl",
                    a11yCaptionBg = "light"
                )

            repository.updateAccessibilityUser = updatedUser

            var callbackUser: UserDto? = null

            viewModel.update {
                it.copy(
                    a11yUiFont = "xl",
                    a11yVisualAlerts = true,
                    a11yVibrate = true,
                    a11yFlashOnCall = true,
                    a11yLiveCaptions = true,
                    a11yVoiceNoteSTT = true,
                    a11yCaptionFont = "xl",
                    a11yCaptionBg = "light"
                )
            }

            viewModel.saveAccessibility { user ->
                callbackUser = user
            }

            advanceUntilIdle()

            val request = repository.updateAccessibilityRequests.single()

            assertEquals("xl", request.a11yUiFont)
            assertEquals(true, request.a11yVisualAlerts)
            assertEquals(true, request.a11yVibrate)
            assertEquals(true, request.a11yFlashOnCall)
            assertEquals(true, request.a11yLiveCaptions)
            assertEquals(true, request.a11yVoiceNoteSTT)
            assertEquals("xl", request.a11yCaptionFont)
            assertEquals("light", request.a11yCaptionBg)

            assertEquals(updatedUser, callbackUser)

            val state = viewModel.state.value

            assertFalse(state.isSaving)
            assertEquals("Accessibility settings saved.", state.success)
            assertNull(state.error)

            assertEquals("xl", state.a11yUiFont)
            assertTrue(state.a11yVisualAlerts)
            assertTrue(state.a11yVibrate)
            assertTrue(state.a11yFlashOnCall)
            assertTrue(state.a11yLiveCaptions)
            assertTrue(state.a11yVoiceNoteSTT)
            assertEquals("xl", state.a11yCaptionFont)
            assertEquals("light", state.a11yCaptionBg)
        }

    @Test
    fun saveAccessibility_failureSetsErrorAndDoesNotCallCallback() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeUserSettingsRepository()
            repository.updateAccessibilityShouldThrow = true

            val viewModel = SettingsViewModel(repository)

            var callbackCalled = false

            viewModel.saveAccessibility {
                callbackCalled = true
            }

            advanceUntilIdle()

            val state = viewModel.state.value

            assertFalse(state.isSaving)
            assertEquals("Accessibility boom", state.error)
            assertNull(state.success)
            assertFalse(callbackCalled)
        }

    private fun user(
        id: Int,
        username: String = "user_$id",
        preferredLanguage: String? = "en",
        autoTranslate: Boolean? = null,
        showOriginalWithTranslation: Boolean? = null,
        theme: String? = "dawn",
        plan: String? = "FREE",
        allowExplicitContent: Boolean? = null,
        showReadReceipts: Boolean? = null,
        autoDeleteSeconds: Int? = null,
        privacyBlurEnabled: Boolean? = null,
        privacyBlurOnUnfocus: Boolean? = null,
        privacyHoldToReveal: Boolean? = null,
        notifyOnCopy: Boolean? = null,
        riaRemember: Boolean? = null,
        enableSmartReplies: Boolean? = null,
        maskAIProfanity: Boolean? = null,
        messageTone: String? = null,
        ringtone: String? = null,
        soundVolume: Double? = null,
        ageBand: String? = null,
        wantsAgeFilter: Boolean? = null,
        randomChatAllowedBands: List<String>? = null,
        voicemailEnabled: Boolean? = null,
        voicemailAutoDeleteDays: Int? = null,
        voicemailForwardEmail: String? = null,
        voicemailGreetingText: String? = null,
        a11yUiFont: String? = null,
        a11yVisualAlerts: Boolean? = null,
        a11yVibrate: Boolean? = null,
        a11yFlashOnCall: Boolean? = null,
        a11yLiveCaptions: Boolean? = null,
        a11yVoiceNoteSTT: Boolean? = null,
        a11yCaptionFont: String? = null,
        a11yCaptionBg: String? = null
    ): UserDto {
        return UserDto(
            id = id,
            email = "$username@example.com",
            username = username,
            preferredLanguage = preferredLanguage,
            uiLanguage = preferredLanguage,
            autoTranslate = autoTranslate,
            showOriginalWithTranslation = showOriginalWithTranslation,
            theme = theme,
            plan = plan,
            allowExplicitContent = allowExplicitContent,
            showReadReceipts = showReadReceipts,
            autoDeleteSeconds = autoDeleteSeconds,
            privacyBlurEnabled = privacyBlurEnabled,
            privacyBlurOnUnfocus = privacyBlurOnUnfocus,
            privacyHoldToReveal = privacyHoldToReveal,
            notifyOnCopy = notifyOnCopy,
            riaRemember = riaRemember,
            enableSmartReplies = enableSmartReplies,
            maskAIProfanity = maskAIProfanity,
            messageTone = messageTone,
            ringtone = ringtone,
            soundVolume = soundVolume,
            ageBand = ageBand,
            wantsAgeFilter = wantsAgeFilter,
            randomChatAllowedBands = randomChatAllowedBands,
            voicemailEnabled = voicemailEnabled,
            voicemailAutoDeleteDays = voicemailAutoDeleteDays,
            voicemailForwardEmail = voicemailForwardEmail,
            voicemailGreetingText = voicemailGreetingText,
            a11yUiFont = a11yUiFont,
            a11yVisualAlerts = a11yVisualAlerts,
            a11yVibrate = a11yVibrate,
            a11yFlashOnCall = a11yFlashOnCall,
            a11yLiveCaptions = a11yLiveCaptions,
            a11yVoiceNoteSTT = a11yVoiceNoteSTT,
            a11yCaptionFont = a11yCaptionFont,
            a11yCaptionBg = a11yCaptionBg
        )
    }

    private class FakeUserSettingsRepository : UserSettingsRepository {
        val updateSettingsRequests = mutableListOf<SettingsUpdateRequest>()
        val updateAccessibilityRequests =
            mutableListOf<AccessibilitySettingsUpdateRequest>()

        var updateSettingsUser =
            UserDto(
                id = 100,
                email = "updated@example.com",
                username = "updated_user",
                preferredLanguage = "en",
                uiLanguage = "en",
                theme = "dawn"
            )

        var updateAccessibilityUser =
            UserDto(
                id = 101,
                email = "a11y@example.com",
                username = "a11y_user",
                preferredLanguage = "en",
                uiLanguage = "en",
                theme = "dawn"
            )

        var updateSettingsShouldThrow = false
        var updateAccessibilityShouldThrow = false
        var deleteAccountCalled = false
        var removeAvatarCalled = false

        override suspend fun updateUsername(
            username: String
        ): UserDto {
            return updateSettingsUser.copy(username = username)
        }

        override suspend fun updateSettings(
            request: SettingsUpdateRequest
        ): UserDto {
            if (updateSettingsShouldThrow) {
                throw Exception("Settings boom")
            }

            updateSettingsRequests.add(request)
            return updateSettingsUser
        }

        override suspend fun updateAccessibility(
            request: AccessibilitySettingsUpdateRequest
        ): UserDto {
            if (updateAccessibilityShouldThrow) {
                throw Exception("Accessibility boom")
            }

            updateAccessibilityRequests.add(request)
            return updateAccessibilityUser
        }

        override suspend fun deleteAccount() {
            deleteAccountCalled = true
        }

        override suspend fun removeAvatar(): AvatarResponse {
            removeAvatarCalled = true
            return AvatarResponse(avatarUrl = null)
        }
    }
}