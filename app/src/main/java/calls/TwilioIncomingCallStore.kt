package com.chatforia.android.calls

import com.twilio.voice.CallInvite

object TwilioIncomingCallStore {

    @Volatile
    private var pendingInvite: CallInvite? = null

    @Volatile
    private var locallyClaimedCallId: Int? = null

    fun save(callInvite: CallInvite) {
        /*
         * A newly delivered invite belongs to a new answer attempt.
         * Never carry answer authority forward from an older call.
         */
        pendingInvite = callInvite
        locallyClaimedCallId = null
    }

    fun take(): CallInvite? {
        val invite = pendingInvite

        /*
         * Consuming the Twilio invite must NOT clear the local backend
         * claim. answered_elsewhere can arrive after invite.accept().
         */
        pendingInvite = null

        return invite
    }

    fun peek(): CallInvite? {
        return pendingInvite
    }

    fun markLocallyClaimed(
        callId: Int?
    ) {
        if (
            callId != null &&
            callId > 0
        ) {
            locallyClaimedCallId = callId
        }
    }

    fun isLocallyClaimed(
        callId: Int?
    ): Boolean {
        return (
            callId != null &&
            callId > 0 &&
            locallyClaimedCallId == callId
        )
    }

    fun clearLocalClaim(
        callId: Int? = null
    ) {
        if (
            callId == null ||
            locallyClaimedCallId == callId
        ) {
            locallyClaimedCallId = null
        }
    }

    fun clear() {
        pendingInvite = null
        locallyClaimedCallId = null
    }
}
