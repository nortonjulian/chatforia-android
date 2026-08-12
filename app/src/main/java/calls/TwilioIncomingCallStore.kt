package com.chatforia.android.calls

import com.twilio.voice.CallInvite

object TwilioIncomingCallStore {

    @Volatile
    private var pendingInvite: CallInvite? = null

    @Volatile
    private var locallyClaimedCallId: Int? = null

    fun save(callInvite: CallInvite) {
        /*
         * Do not clear local answer authority here. The Twilio invite
         * can arrive while the backend ACTIVE claim is in flight.
         *
         * Authority is call-ID scoped and is explicitly cleared when
         * a claim loses, fails, or the matching call terminates, so an
         * older call ID cannot protect a different invitation.
         */
        pendingInvite = callInvite
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
