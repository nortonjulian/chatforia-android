package com.chatforia.android.billing

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class PlanEntitlementsRepositoryTest {

    @Test
    fun decodesBackendAllowanceShape() {
        val payload =
            """
            {
              "plan": "PREMIUM",
              "entitlements": {
                "riaActions": 500,
                "translationChars": 1000000,
                "hostedParticipantMinutes": 600,
                "smsMessages": 750,
                "pstnMinutes": 300,
                "forwardingMinutes": 300,
                "voicemailTranscriptionMinutes": 30,
                "cloudStorageBytes": 53687091200,
                "messageHistoryDays": null,
                "adsEnabled": false,
                "aiRewriteLevel": "FULL",
                "supportLevel": "PRIORITY"
              },
              "monthKey": "2026-10",
              "usage": {
                "riaActions": {
                  "used": 12,
                  "limit": 500,
                  "remaining": 488
                }
              }
            }
            """.trimIndent()

        val response =
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            }.decodeFromString<PlanEntitlementsResponse>(payload)

        assertEquals("PREMIUM", response.plan)
        assertEquals(500, response.entitlements.riaActions)
        assertEquals(
            53_687_091_200L,
            response.entitlements.cloudStorageBytes
        )
        assertNull(response.entitlements.messageHistoryDays)
        assertFalse(response.entitlements.adsEnabled)
        assertEquals("FULL", response.entitlements.aiRewriteLevel)
        assertEquals(12, response.usage["riaActions"]?.used)
        assertEquals(488, response.usage["riaActions"]?.remaining)
    }
}
