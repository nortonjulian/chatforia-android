package com.chatforia.android.auth

import android.content.Context
import android.content.Intent
import android.net.Uri

class AppleAuthClient(
    private val context: Context
) {
    fun start() {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(
                "https://api.chatforia.com/auth/apple?next=chatforia%3A%2F%2Foauth%2Fapple"
            )
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
    }
}