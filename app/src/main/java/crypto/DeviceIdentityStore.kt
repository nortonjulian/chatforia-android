package com.chatforia.android.crypto

interface DeviceIdentityStore {
    fun getOrCreateDeviceId(): String

    fun getOrCreateKeyPair(): Pair<String, String>
}