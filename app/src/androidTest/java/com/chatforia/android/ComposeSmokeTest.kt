package com.chatforia.android

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.chatforia.android.auth.LanguageSelectionView
import com.chatforia.android.auth.LoginScreen
import com.chatforia.android.auth.SettingsUiState
import com.chatforia.android.calls.AudioCallScreen
import com.chatforia.android.calls.CallSession
import com.chatforia.android.calls.VideoCallScreen
import com.chatforia.android.sounds.SoundSettingsView
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test

class ComposeSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun loginScreen_rendersWithoutCrashing() {
        composeRule.setContent {
            MaterialTheme {
                LoginScreen(
                    onLogin = { _, _ -> },
                    onGoogleLogin = {},
                    onAppleLogin = {},
                    onCreateAccount = {},
                    onResetEncryption = { _, _ -> },
                    onForgotPassword = {},
                    onResendVerification = {}
                )
            }
        }

        composeRule.onNodeWithText("Google").assertIsDisplayed()
        composeRule.onNodeWithText("Apple").assertIsDisplayed()
        composeRule.onNodeWithText("Log in").assertIsDisplayed()
        composeRule.onNodeWithText("Create an account").assertIsDisplayed()
    }

    @Test
    fun languageSelectionView_rendersWithoutCrashing() {
        composeRule.setContent {
            MaterialTheme {
                LanguageSelectionView(
                    selectedLanguage = "en",
                    onLanguageChange = {}
                )
            }
        }

        composeRule.onNodeWithText("English").assertIsDisplayed()
    }

    @Test
    fun accessibilitySettingsScreen_rendersWithoutCrashing() {
        composeRule.setContent {
            MaterialTheme {
                AccessibilitySettingsScreen(
                    state = SettingsUiState(),
                    onBack = {},
                    onUpdate = {},
                    onSave = {},
                    onUpgradeRequired = {}
                )
            }
        }

        composeRule
            .onNodeWithText("Save", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun audioCallScreen_rendersWithoutCrashing() {
        composeRule.setContent {
            MaterialTheme {
                AudioCallScreen(
                    session = CallSession(
                        callId = 123,
                        displayName = "Audio Friend",
                        isVideo = false
                    ),
                    onToggleMute = {},
                    onToggleSpeaker = {},
                    onEndCall = {}
                )
            }
        }

        composeRule.onNodeWithText("Audio Friend").assertIsDisplayed()
        composeRule.onNodeWithText("Calling…").assertIsDisplayed()
    }

    @Test
    fun videoCallScreen_rendersWithoutCrashing() {
        composeRule.setContent {
            MaterialTheme {
                VideoCallScreen(
                    session = CallSession(
                        callId = 456,
                        roomName = "room-456",
                        displayName = "Video Friend",
                        isVideo = true
                    ),
                    onToggleMute = {},
                    onToggleCamera = {},
                    onFlipCamera = {},
                    onEndCall = {}
                )
            }
        }

        composeRule.onNodeWithText("Video room: room-456").assertIsDisplayed()
        composeRule.onNodeWithText("Local video").assertIsDisplayed()
    }

    @Test
    fun soundSettingsView_rendersWithoutCrashing() {
        composeRule.setContent {
            MaterialTheme {
                SoundSettingsView(
                    currentPlan = "FREE",
                    state = SettingsUiState(
                        messageTone = "Default.mp3",
                        ringtone = "Classic.mp3",
                        soundVolume = 70
                    ),
                    onMessageToneChange = {},
                    onRingtoneChange = {},
                    onVolumeChange = {},
                    onUpgradeRequired = {}
                )
            }
        }

        composeRule.onNodeWithText("70%").assertIsDisplayed()
    }
}