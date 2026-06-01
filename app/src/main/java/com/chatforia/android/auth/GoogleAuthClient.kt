package com.chatforia.android.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.chatforia.android.network.Environment
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class GoogleAuthClient(
    private val context: Context
) {
    private val credentialManager =
        CredentialManager.create(context)

    suspend fun getIdToken(): String {
        val googleIdOption =
            GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(Environment.GOOGLE_WEB_CLIENT_ID)
                .build()

        val request =
            GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

        val result =
            credentialManager.getCredential(
                context = context,
                request = request
            )

        val credential =
            GoogleIdTokenCredential
                .createFrom(result.credential.data)

        return credential.idToken
    }
}