package com.chatforia.android.crypto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun KeySetupScreen(
    viewModel: KeySetupViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    var backupPassword by remember {
        mutableStateOf("")
    }

    LaunchedEffect(Unit) {
        viewModel.refreshBackupStatus()
    }

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Encryption",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = when {
                    state.hasLocalPrivateKey ->
                        "This device has your encryption key and can decrypt messages."

                    state.isCheckingBackup ->
                        "Checking encryption backup…"

                    state.hasRemoteBackup ->
                        "A backup was found for this account. Restore it to decrypt encrypted messages on this device."

                    else ->
                        "No local encryption key was found on this device."
                },
                style = MaterialTheme.typography.bodyMedium
            )

            if (state.isCheckingBackup) {
                CircularProgressIndicator()
            }

            Divider()

            Text(
                text = "Restore from backup",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = backupPassword,
                onValueChange = { backupPassword = it },
                label = {
                    Text("Backup password")
                },
                visualTransformation = PasswordVisualTransformation(),
                enabled = !state.isRestoring,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    viewModel.restoreFromRemoteBackup(backupPassword)
                },
                enabled =
                    backupPassword.length >= 6 &&
                            !state.isRestoring &&
                            state.hasRemoteBackup,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isRestoring) {
                    CircularProgressIndicator()
                } else {
                    Text("Restore encryption key")
                }
            }

            if (!state.hasRemoteBackup && !state.isCheckingBackup) {
                Text(
                    text = "No account backup was found. You can still create a new encryption key later, but older encrypted messages may remain locked.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (state.successMessage != null) {
                Text(
                    text = state.successMessage ?: "",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (state.error != null) {
                Text(
                    text = state.error ?: "",
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (state.hasLocalPrivateKey) {
                Divider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            viewModel.clearLocalKeys()
                            backupPassword = ""
                        }
                    ) {
                        Text("Clear local key")
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}