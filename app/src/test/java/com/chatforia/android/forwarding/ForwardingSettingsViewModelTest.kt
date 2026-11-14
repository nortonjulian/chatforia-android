package com.chatforia.android.forwarding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ForwardingSettingsViewModelTest {
    @Test fun loadSaveAndReloadUseOnlyTheMockedAccountSettings() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val store = FakeStore(ForwardingSettingsDto(
                forwardingEnabledSms = true,
                forwardSmsToPhone = true,
                forwardPhoneNumber = "+15551234567"
            ))
            val vm = ForwardingSettingsViewModel(store)
            vm.load()
            advanceUntilIdle()
            assertTrue(vm.state.value.forwardingEnabledSms)
            assertTrue(vm.state.value.hasChanges) // legacy NULL zone proposes this device's zone

            vm.update { it.copy(forwardPhoneNumber = "+1 (555) 987-6543", forwardQuietHoursStart = 22) }
            assertFalse(vm.state.value.canSave)
            vm.update { it.copy(forwardQuietHoursEnd = 6, forwardQuietHoursTimeZone = "America/Denver") }
            assertTrue(vm.state.value.canSave)
            vm.save()
            advanceUntilIdle()
            assertEquals("+15559876543", store.saved.single().forwardPhoneNumber)
            assertEquals(22, store.saved.single().forwardQuietHoursStart)
            assertEquals("America/Denver", store.saved.single().forwardQuietHoursTimeZone)
            assertFalse(vm.state.value.hasChanges)

            val reopened = ForwardingSettingsViewModel(store)
            reopened.load()
            advanceUntilIdle()
            assertEquals("+15559876543", reopened.state.value.forwardPhoneNumber)
            assertEquals(6, reopened.state.value.forwardQuietHoursEnd)
            assertEquals(store.saved.single().forwardQuietHoursTimeZone, reopened.state.value.forwardQuietHoursTimeZone)

            val secondAccount = ForwardingSettingsViewModel(FakeStore(ForwardingSettingsDto(
                forwardQuietHoursTimeZone = "Europe/London"
            )))
            secondAccount.load()
            advanceUntilIdle()
            assertFalse(secondAccount.state.value.forwardingEnabledSms)
            assertEquals("", secondAccount.state.value.forwardPhoneNumber)
            assertEquals("Europe/London", secondAccount.state.value.forwardQuietHoursTimeZone)
        } finally {
            Dispatchers.resetMain()
        }
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
