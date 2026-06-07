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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import com.chatforia.android.ui.theme.ChatforiaColors
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
@Composable
fun KeySetupScreen(
    viewModel: KeySetupViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    var backupPassword by remember {
        mutableStateOf("")
    }

    var confirmPassword by remember {
        mutableStateOf("")
    }

    var showResetDialog by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        viewModel.refreshBackupStatus()
    }

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ChatforiaColors.cardBackground
        )
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

            HorizontalDivider(color = ChatforiaColors.border)

            when {

                state.hasLocalPrivateKey -> {

                    Text(
                        text = if (state.hasRemoteBackup) {
                            "Recovery Backup"
                        } else {
                            "Create Recovery Backup"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = backupPassword,
                        onValueChange = { backupPassword = it },
                        label = { Text("Recovery Passcode") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ChatforiaColors.primaryText,
                            unfocusedTextColor = ChatforiaColors.primaryText,
                            focusedLabelColor = ChatforiaColors.secondaryText,
                            unfocusedLabelColor = ChatforiaColors.secondaryText,
                            focusedBorderColor = ChatforiaColors.border,
                            unfocusedBorderColor = ChatforiaColors.border,
                            cursorColor = ChatforiaColors.accent
                        )
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Recovery Passcode") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ChatforiaColors.primaryText,
                            unfocusedTextColor = ChatforiaColors.primaryText,
                            focusedLabelColor = ChatforiaColors.secondaryText,
                            unfocusedLabelColor = ChatforiaColors.secondaryText,
                            focusedBorderColor = ChatforiaColors.border,
                            unfocusedBorderColor = ChatforiaColors.border,
                            cursorColor = ChatforiaColors.accent
                        )
                    )

                    Button(
                        onClick = {
                            viewModel.createRemoteBackup(
                                backupPassword
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChatforiaColors.accent,
                            contentColor = ChatforiaColors.buttonForeground
                        ),
                        enabled =
                            backupPassword.length >= 8 &&
                                    backupPassword == confirmPassword &&
                                    !state.isCreatingBackup,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.isCreatingBackup) {
                            CircularProgressIndicator()
                        } else {
                            Text(
                                if (state.hasRemoteBackup) {
                                    "Update Recovery Backup"
                                } else {
                                    "Create Recovery Backup"
                                }
                            )
                        }
                    }
                }

                state.hasRemoteBackup -> {
                    Text(
                        text = "Restore encrypted chats",
                        style = MaterialTheme.typography.titleMedium,
                        color = ChatforiaColors.primaryText
                    )

                    OutlinedTextField(
                        value = backupPassword,
                        onValueChange = { backupPassword = it },
                        label = { Text("Recovery Passcode") },
                        visualTransformation = PasswordVisualTransformation(),
                        enabled = !state.isRestoring,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ChatforiaColors.primaryText,
                            unfocusedTextColor = ChatforiaColors.primaryText,
                            focusedLabelColor = ChatforiaColors.secondaryText,
                            unfocusedLabelColor = ChatforiaColors.secondaryText,
                            focusedBorderColor = ChatforiaColors.border,
                            unfocusedBorderColor = ChatforiaColors.border,
                            cursorColor = ChatforiaColors.accent
                        )
                    )

                    Button(
                        onClick = {
                            viewModel.restoreFromRemoteBackup(backupPassword)
                        },
                        enabled =
                            backupPassword.length >= 8 &&
                                    !state.isRestoring,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.isRestoring) {
                            CircularProgressIndicator()
                        } else {
                            Text("Restore Chats")
                        }
                    }
                }

                else -> {

                    Text(
                        text = "No recovery backup found",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text =
                            "Open Chatforia on a device that can already read your encrypted chats and create a Recovery Passcode there."
                    )
                }
            }


            if (!state.hasRemoteBackup && !state.isCheckingBackup) {
                Text(
                    text = "No Recovery Backup found yet. Create one from this device so you can restore chats on iOS, Android, and web.",
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

                HorizontalDivider(color = ChatforiaColors.border)

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

            HorizontalDivider(color = ChatforiaColors.border)

            Text(
                text = "Reset Encryption",
                style = MaterialTheme.typography.titleMedium,
                color = ChatforiaColors.primaryText
            )

            Text(
                text = "Only use this if you have lost all trusted devices and cannot restore your original key.",
                color = ChatforiaColors.secondaryText
            )


            Button(
                onClick = {
                    showResetDialog = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ChatforiaColors.accent,
                    contentColor = ChatforiaColors.buttonForeground
                )
            ) {
                Text("Reset Encryption")
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = {
                showResetDialog = false
            },

            title = {
                Text("Reset Encryption?")
            },

            text = {
                Text(
                    "This will permanently replace your encryption key.\n\n" +
                            "Encrypted chats protected by your old key may no longer be readable.\n\n" +
                            "Only continue if you have lost all trusted devices and cannot restore your Recovery Backup."
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        viewModel.resetEncryption()
                    }
                ) {
                    Text("Reset Encryption")
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

