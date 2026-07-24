package com.chatforia.android.calls

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class CallsResponseTest {

    @Test
    fun decodesNumericNextCursor() {
        val response = Json {
            ignoreUnknownKeys = true
        }.decodeFromString(
            CallsResponse.serializer(),
            """{"items":[],"nextCursor":279}"""
        )

        assertEquals(279, response.nextCursor)
    }
}
