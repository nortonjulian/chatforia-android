package com.chatforia.android.socket

/**
 * The Random Chat search belongs to the user's intent, not a particular
 * Socket.IO connection. A socket disconnect removes the server's queue entry,
 * so an outstanding search must be re-joined when the transport reconnects.
 *
 * The intent is cleared on cancel, match, leave, and account sign-out.
 * A matched session must never be automatically rematched.
 */
internal class RandomSearchIntent {
    @Volatile
    private var requested = false

    fun request() {
        requested = true
    }

    fun clear() {
        requested = false
    }

    fun isRequested(): Boolean = requested

    fun shouldJoin(connected: Boolean): Boolean =
        requested && connected
}
