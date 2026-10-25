package com.chatforia.android.ria

import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class GroupRiaConsentContractTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test fun canonicalRoomConsentReadsOnlyCurrentMember() {
        val on = json.decodeFromString<GroupRiaRoomConsent>(
            """{"allowForiaBot":false,"me":{"allowAIBot":true}}"""
        )
        assertTrue(on.me.allowAIBot)
        val off = json.decodeFromString<GroupRiaRoomConsent>(
            """{"me":{"allowAIBot":false}}"""
        )
        assertFalse(off.me.allowAIBot)
    }

    @Test fun updateRequestAndResponseUseBackendFields() {
        assertEquals("""{"allow":false}""", json.encodeToString(GroupRiaConsentUpdate(false)))
        assertFalse(json.decodeFromString<GroupRiaConsentResult>(
            """{"allowAIBot":false}"""
        ).allowAIBot)
    }
}
