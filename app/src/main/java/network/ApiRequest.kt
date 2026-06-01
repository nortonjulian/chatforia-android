package com.chatforia.android.network

data class ApiRequest(
    val path: String,
    val method: HttpMethod,
    val bodyJson: String? = null,
    val requiresAuth: Boolean = true
)