package com.chatforia.android.crypto

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chatforia.android.auth.AuthRepository
import com.chatforia.android.auth.UserDto
import com.chatforia.android.network.ApiClient
import androidx.compose.ui.platform.LocalContext

@Composable
fun KeyRestoreGate(
    user: UserDto,
    message: String,
    apiClient: ApiClient,
    authRepository: AuthRepository,
    onRecovered: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current

    val keyStorage =
        remember {
            KeyStorage(context)
        }

    val accountKeyManager =
        remember {
            AccountKeyManager(keyStorage)
        }

    val viewModel =
        remember {
            KeySetupViewModel(
                remoteKeyBackupRepository = RemoteKeyBackupRepository(apiClient),
                keyStorage = keyStorage,
                authRepository = authRepository,
                accountKeyManager = accountKeyManager,
                keyBackupCrypto = KeyBackupCrypto()
            )
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Restore Encryption",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        KeySetupScreen(
            viewModel = viewModel
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onRecovered
        ) {
            Text("I've restored my key")
        }

        TextButton(
            onClick = onLogout
        ) {
            Text("Log out")
        }
    }
}