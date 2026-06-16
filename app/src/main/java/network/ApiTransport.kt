package com.chatforia.android.network

interface ApiTransport {
    fun sendRaw(
        request: ApiRequest
    ): String
}