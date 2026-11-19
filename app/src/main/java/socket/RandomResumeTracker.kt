package com.chatforia.android.socket

import java.util.UUID

/** Correlates negative resume responses without treating a room ID as authority. */
internal class RandomResumeTracker {
    private var pendingRequestId: String? = null
    private var activeRoomId: Int? = null

    @Synchronized
    fun begin(): String = UUID.randomUUID().toString().also { pendingRequestId = it }

    @Synchronized
    fun matched(roomId: Int) {
        pendingRequestId = null
        activeRoomId = roomId
    }

    @Synchronized
    fun acceptResumed(requestId: String?, roomId: Int): Boolean {
        if (requestId == null || requestId != pendingRequestId) return false
        matched(roomId)
        return true
    }

    @Synchronized
    fun acceptNone(requestId: String?): Int? {
        if (requestId == null || requestId != pendingRequestId) return null
        val roomId = activeRoomId ?: 0
        clear()
        return roomId
    }

    @Synchronized
    fun ended(roomId: Int): Boolean {
        if (roomId != activeRoomId) return false
        clear()
        return true
    }

    @Synchronized
    fun clear() {
        pendingRequestId = null
        activeRoomId = null
    }
}
