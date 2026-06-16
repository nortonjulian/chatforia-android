package com.chatforia.android.socket

import kotlinx.coroutines.flow.SharedFlow

interface ChatRealtimeEvents {
    val messageUpserts: SharedFlow<String>
    val messageAcks: SharedFlow<String>
    val messageEdited: SharedFlow<String>
    val messageDeleted: SharedFlow<String>
    val messageExpired: SharedFlow<String>
    val messageReads: SharedFlow<String>
    val socketConnected: SharedFlow<Unit>
    val smsMessages: SharedFlow<String>

    fun joinRoom(roomId: Int)
}