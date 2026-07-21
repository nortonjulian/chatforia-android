package com.chatforia.android.network

class ApiException(
    val statusCode: Int,
    val responseBody: String
) : Exception(
    "HTTP $statusCode: $responseBody"
)
