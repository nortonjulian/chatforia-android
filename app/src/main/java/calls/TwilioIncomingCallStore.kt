package com.chatforia.android.calls

import com.twilio.voice.CallInvite

object TwilioIncomingCallStore {
    @Volatile
    private var pendingInvite: CallInvite? = null

    fun save(callInvite: CallInvite) {
        pendingInvite = callInvite
    }

    fun take(): CallInvite? {
        val invite = pendingInvite
        pendingInvite = null
        return invite
    }

    fun peek(): CallInvite? {
        return pendingInvite
    }

    fun clear() {
        pendingInvite = null
    }
}