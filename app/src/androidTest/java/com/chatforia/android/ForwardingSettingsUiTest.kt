package com.chatforia.android

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.chatforia.android.forwarding.ForwardingSettingsDto
import com.chatforia.android.forwarding.ForwardingSettingsStore
import com.chatforia.android.forwarding.ForwardingSettingsView
import com.chatforia.android.forwarding.ForwardingSettingsViewModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ForwardingSettingsUiTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun accountSwitchAndDisabledSaveStayInTheSyntheticScreen() {
        val storeA = FakeStore(ForwardingSettingsDto(
            forwardingEnabledSms = true, forwardSmsToPhone = true,
            forwardPhoneNumber = "+15551110000", forwardQuietHoursStart = 22,
            forwardQuietHoursEnd = 6
        ))
        val storeB = FakeStore(ForwardingSettingsDto(
            forwardingEnabledCalls = true, forwardToPhoneE164 = "+15552220000"
        ))
        val model = mutableStateOf(ForwardingSettingsViewModel(storeA))
        rule.setContent { MaterialTheme { ForwardingSettingsView(model.value, onBack = {}) } }
        rule.waitUntil(10_000) { !model.value.state.value.isLoading && model.value.state.value.initial.forwardPhoneNumber == "+15551110000" }
        rule.onNodeWithText("+15551110000").assertExists()

        rule.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))[0].performClick()
        rule.runOnIdle { assertFalse(model.value.state.value.forwardingEnabledSms) }
        rule.onNodeWithText(rule.activity.getString(R.string.common_save)).performScrollTo().performClick()
        rule.waitUntil(10_000) { !model.value.state.value.isSaving && storeA.saved.isNotEmpty() }
        rule.runOnIdle {
            assertFalse(storeA.saved.single().forwardingEnabledSms)
            assertTrue(storeA.saved.single().forwardSmsToPhone)
        }

        rule.runOnUiThread { model.value = ForwardingSettingsViewModel(storeB) }
        rule.waitUntil(10_000) { !model.value.state.value.isLoading && model.value.state.value.initial.forwardToPhoneE164 == "+15552220000" }
        rule.onNodeWithText("+15551110000").assertDoesNotExist()
        rule.onNodeWithText("+15552220000").assertExists()
    }

    private class FakeStore(var prefs: ForwardingSettingsDto) : ForwardingSettingsStore {
        val saved = mutableListOf<ForwardingSettingsDto>()
        override suspend fun fetchSettings() = prefs
        override suspend fun saveSettings(request: ForwardingSettingsDto): ForwardingSettingsDto {
            saved += request
            prefs = request
            return request
        }
    }
}
