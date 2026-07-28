package com.chatforia.android.crypto

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chatforia.android.R
import com.chatforia.android.auth.AuthRepository
import com.chatforia.android.auth.UserDto
import com.chatforia.android.network.ApiClient

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
    val scrollState = rememberScrollState()

    val keyStorage =
        remember(user.id) {
            KeyStorage(
                context = context,
                accountUserId = user.id
            )
        }

    val accountKeyManager =
        remember(user.id) {
            AccountKeyManager(keyStorage)
        }

    val viewModel =
        remember(user.id) {
            KeySetupViewModel(
                remoteKeyBackupRepository = RemoteKeyBackupRepository(apiClient),
                keyStorage = keyStorage,
                authRepository = authRepository,
                accountKeyManager = accountKeyManager,
                userId = user.id,
                keyBackupCrypto = KeyBackupCrypto()
            )
        }

    Column(
        modifier = Modifier
            .fillMaxSize()

            // Keeps content above Android's bottom navigation controls.
            .navigationBarsPadding()

            // Keeps content reachable when the keyboard is visible.
            .imePadding()

            // Allows the entire recovery page to scroll.
            .verticalScroll(scrollState)

            // Extra bottom padding ensures the final buttons can scroll
            // completely above the Android navigation bar.
            .padding(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 32.dp
            )
    ) {
        Text(
            text = stringResource(
                R.string.android_key_restore_gate_restore_encryption
            ),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        KeySetupScreen(
            viewModel = viewModel,
            modifier = Modifier.fillMaxWidth(),
            onRecoveryValidated = onRecovered
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                stringResource(
                    R.string.android_profile_log_out
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
