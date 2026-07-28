package com.chatforia.android.crypto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.stringResource
import com.chatforia.android.R
@Composable
fun KeySetupScreen(
    viewModel: KeySetupViewModel,
    modifier: Modifier = Modifier,
    onRecoveryValidated: (() -> Unit)? = null
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

    var startFreshText by remember {
        mutableStateOf("")
    }

    var showStartFreshSection by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        viewModel.refreshBackupStatus()
    }

    /*
     * A recovery gate may close only after the complete local key pair
     * has been compared with the current account public key.
     */
    LaunchedEffect(
        state.hasMatchingLocalKey,
        state.isCheckingBackup
    ) {
        if (
            state.hasMatchingLocalKey &&
            !state.isCheckingBackup
        ) {
            onRecoveryValidated?.invoke()
        }
    }

    /*
     * Never retain passcodes after a successful operation.
     */
    LaunchedEffect(state.successMessage) {
        if (!state.successMessage.isNullOrBlank()) {
            backupPassword = ""
            confirmPassword = ""
            startFreshText = ""
        }
    }

    /*
     * Clear stale fields whenever the screen changes between restore,
     * create, and update modes.
     */
    LaunchedEffect(
        state.hasMatchingLocalKey,
        state.hasRemoteBackup,
        state.isBackupStatusKnown
    ) {
        backupPassword = ""
        confirmPassword = ""
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
                text = stringResource(R.string.android_key_setup_encryption),
                style = MaterialTheme.typography.titleLarge,
                color = ChatforiaColors.primaryText
            )

            Text(
                text = when {
                    state.hasMatchingLocalKey ->
                        stringResource(R.string.android_key_setup_device_can_read_secure_messages)

                    state.isCheckingBackup ->
                        stringResource(R.string.android_key_setup_checking_secure_message_recovery)

                    state.hasRemoteBackup ->
                        stringResource(R.string.android_key_setup_recovery_backup_found)

                    else ->
                        stringResource(R.string.android_key_setup_missing_secure_message_key)
                },
                style = MaterialTheme.typography.bodyMedium
            )

            if (state.isCheckingBackup) {
                CircularProgressIndicator()
            }

            HorizontalDivider(color = ChatforiaColors.border)

            when {

                state.hasMatchingLocalKey -> {

                    Text(
                        text = if (state.hasRemoteBackup) {
                            stringResource(R.string.android_key_setup_secure_message_recovery)
                        } else {
                            stringResource(R.string.android_key_setup_create_recovery_backup)
                        },
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = backupPassword,
                        onValueChange = { backupPassword = it },
                        label = { Text(stringResource(R.string.android_key_setup_recovery_passcode)) },
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
                        label = { Text(stringResource(R.string.android_key_setup_confirm_recovery_passcode)) },
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
                                    !state.isCreatingBackup &&
                                    state.isBackupStatusKnown,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.isCreatingBackup) {
                            CircularProgressIndicator()
                        } else {
                            Text(
                                if (state.hasRemoteBackup) {
                                    stringResource(R.string.android_key_setup_update_recovery_backup)
                                } else {
                                    stringResource(R.string.android_key_setup_create_recovery_backup)
                                }
                            )
                        }
                    }
                }

                state.hasRemoteBackup -> {
                    Text(
                        text = stringResource(R.string.android_key_setup_restore_encrypted_chats),
                        style = MaterialTheme.typography.titleMedium,
                        color = ChatforiaColors.primaryText
                    )

                    OutlinedTextField(
                        value = backupPassword,
                        onValueChange = { backupPassword = it },
                        label = { Text(stringResource(R.string.android_key_setup_recovery_passcode)) },
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
                            Text(stringResource(R.string.android_key_setup_restore_chats))
                        }
                    }
                }

                else -> {

                    Text(
                        text = stringResource(R.string.android_key_setup_no_recovery_backup_found),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = stringResource(R.string.android_key_setup_open_on_a_device_that_can_already_read_your_encr)
                    )
                }
            }


            if (
                state.isBackupStatusKnown &&
                !state.hasRemoteBackup &&
                !state.isCheckingBackup
            ) {
                Text(
                    text = stringResource(R.string.android_key_setup_no_recovery_backup_found_yet_create_one_from_thi),
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

            HorizontalDivider(color = ChatforiaColors.border)

            TextButton(
                onClick = {
                    showStartFreshSection = !showStartFreshSection

                    if (!showStartFreshSection) {
                        startFreshText = ""
                    }
                }
            ) {
                Text(
                    text = stringResource(R.string.android_key_setup_cannot_recover_secure_messages),
                    color = ChatforiaColors.accent
                )
            }

            if (showStartFreshSection) {
                Text(
                    text = stringResource(R.string.android_key_setup_start_fresh_warning),
                    color = ChatforiaColors.secondaryText
                )

                OutlinedTextField(
                    value = startFreshText,
                    onValueChange = { startFreshText = it },
                    label = { Text(stringResource(R.string.android_key_setup_type_start_fresh)) },
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

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    ChatforiaColors.buttonStart,
                                    ChatforiaColors.buttonEnd
                                )
                            )
                        )
                        .clickable {
                            if (startFreshText.trim().uppercase() == "START FRESH") {
                                showResetDialog = true
                            }
                        }
                        .padding(horizontal = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.android_key_setup_start_fresh_confirm),
                        color = ChatforiaColors.buttonForeground
                    )
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = {
                showResetDialog = false
            },

            title = {
                Text(stringResource(R.string.android_key_setup_start_fresh_title))
            },

            text = {
                Text(
                    stringResource(
                        R.string.android_key_setup_start_fresh_dialog_body,
                        "\n"
                    )
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        viewModel.resetEncryption()
                    }
                ) {
                    Text(stringResource(R.string.android_key_setup_start_fresh_confirm))
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                    }
                ) {
                    Text(stringResource(R.string.android_chats_cancel))
                }
            }
        )
    }
}
