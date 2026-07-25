package com.chatforia.android.notifications

internal object IncomingCallFreshness {
    const val MAX_INCOMING_CALL_AGE_MS = 30_000L

    fun isExpired(
        sentTimeMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (sentTimeMs <= 0L) return false

        val ageMs = nowMs - sentTimeMs

        if (ageMs < 0L) return false

        return ageMs > MAX_INCOMING_CALL_AGE_MS
    }
}
