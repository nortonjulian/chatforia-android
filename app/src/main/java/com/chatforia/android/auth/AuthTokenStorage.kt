package com.chatforia.android.auth

interface AuthTokenStorage {
    fun save(token: String)
    fun read(): String?
    fun clear()
}