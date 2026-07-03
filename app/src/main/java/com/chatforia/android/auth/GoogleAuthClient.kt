package com.chatforia.android.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.chatforia.android.network.Environment
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class GoogleAuthClient(
    private val context: Context
) {
    private val credentialManager =
        CredentialManager.create(context)

    suspend fun getIdToken(): String {
        Log.d("ChatforiaGoogleAuth", "1. Building Google sign-in request")

        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
            Environment.GOOGLE_WEB_CLIENT_ID
        ).build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        Log.d("ChatforiaGoogleAuth", "2. Calling credentialManager.getCredential")

        val result = credentialManager.getCredential(
            context = context,
            request = request
        )

        Log.d("ChatforiaGoogleAuth", "3. Credential result returned: ${result.credential::class.java.name}")

        val credential = result.credential

        if (credential is CustomCredential) {
            Log.d("ChatforiaGoogleAuth", "4. Custom credential type: ${credential.type}")
        }

        if (
            credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)

            Log.d(
                "ChatforiaGoogleAuth",
                "5. Got Google ID token. Length=${googleCredential.idToken.length}"
            )

            return googleCredential.idToken
        }

        throw IllegalStateException("Unexpected credential type: ${credential::class.java.name}")
    }
}