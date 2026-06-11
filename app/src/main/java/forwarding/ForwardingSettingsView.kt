package com.chatforia.android.forwarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatforia.android.ChatforiaGradientButton
import com.chatforia.android.ui.components.ChatforiaSectionCard
import com.chatforia.android.ui.theme.ChatforiaColors

@Composable
fun ForwardingSettingsView(
    viewModel: ForwardingSettingsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatforiaColors.screenBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = ChatforiaColors.primaryText
                )
            }

            Text(
                text = "Call & Text Forwarding",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = ChatforiaColors.primaryText
            )
        }

        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ChatforiaColors.accent)
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            ChatforiaSectionCard(title = "Forwarding") {
                Text(
                    text = "Forward incoming calls and texts to your verified phone or email.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChatforiaColors.secondaryText
                )

                StatusText(state)

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 14.dp),
                    color = ChatforiaColors.border
                )

                SettingSwitchRowLocal(
                    title = "Enable text forwarding",
                    checked = state.forwardingEnabledSms,
                    onCheckedChange = { enabled ->
                        viewModel.update {
                            it.copy(forwardingEnabledSms = enabled)
                        }
                    }
                )

                state.validationErrors["smsToggle"]?.let {
                    ErrorText(it)
                }

                SettingSwitchRowLocal(
                    title = "Forward texts to phone",
                    checked = state.forwardSmsToPhone,
                    onCheckedChange = { enabled ->
                        viewModel.update {
                            it.copy(forwardSmsToPhone = enabled)
                        }
                    }
                )

                ForwardingTextField(
                    label = "Destination phone",
                    value = state.forwardPhoneNumber,
                    placeholder = "+15551234567",
                    enabled = state.forwardSmsToPhone,
                    keyboardType = KeyboardType.Phone,
                    error = state.validationErrors["forwardPhoneNumber"],
                    onValueChange = { value ->
                        viewModel.update {
                            it.copy(forwardPhoneNumber = value)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingSwitchRowLocal(
                    title = "Forward texts to email",
                    checked = state.forwardSmsToEmail,
                    onCheckedChange = { enabled ->
                        viewModel.update {
                            it.copy(forwardSmsToEmail = enabled)
                        }
                    }
                )

                ForwardingTextField(
                    label = "Destination email",
                    value = state.forwardEmail,
                    placeholder = "me@example.com",
                    enabled = state.forwardSmsToEmail,
                    keyboardType = KeyboardType.Email,
                    error = state.validationErrors["forwardEmail"],
                    onValueChange = { value ->
                        viewModel.update {
                            it.copy(forwardEmail = value)
                        }
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 14.dp),
                    color = ChatforiaColors.border
                )

                SettingSwitchRowLocal(
                    title = "Enable call forwarding",
                    checked = state.forwardingEnabledCalls,
                    onCheckedChange = { enabled ->
                        viewModel.update {
                            it.copy(forwardingEnabledCalls = enabled)
                        }
                    }
                )

                ForwardingTextField(
                    label = "Destination phone for calls",
                    value = state.forwardToPhoneE164,
                    placeholder = "+15551234567",
                    enabled = state.forwardingEnabledCalls,
                    keyboardType = KeyboardType.Phone,
                    error = state.validationErrors["forwardToPhoneE164"],
                    onValueChange = { value ->
                        viewModel.update {
                            it.copy(forwardToPhoneE164 = value)
                        }
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 14.dp),
                    color = ChatforiaColors.border
                )

                Text(
                    text = "Quiet Hours",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = ChatforiaColors.primaryText
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ForwardingTextField(
                        label = "Start",
                        value = state.forwardQuietHoursStart?.toString() ?: "",
                        placeholder = "0",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                        onValueChange = { value ->
                            viewModel.update {
                                it.copy(
                                    forwardQuietHoursStart = value.toIntOrNull()
                                )
                            }
                        }
                    )

                    ForwardingTextField(
                        label = "End",
                        value = state.forwardQuietHoursEnd?.toString() ?: "",
                        placeholder = "23",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                        onValueChange = { value ->
                            viewModel.update {
                                it.copy(
                                    forwardQuietHoursEnd = value.toIntOrNull()
                                )
                            }
                        }
                    )
                }

                state.validationErrors["quiet"]?.let {
                    ErrorText(it)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { viewModel.reset() },
                        enabled = state.hasChanges && !state.isSaving,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text("Reset")
                    }

                    ChatforiaGradientButton(
                        text = if (state.isSaving) "Saving..." else "Save",
                        enabled = state.canSave,
                        onClick = { viewModel.save() },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusText(state: ForwardingSettingsState) {
    if (!state.banner.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = state.banner,
            color = ChatforiaColors.accent,
            style = MaterialTheme.typography.bodySmall
        )
    }

    if (!state.errorMessage.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = state.errorMessage,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ErrorText(text: String) {
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = text,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun SettingSwitchRowLocal(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = ChatforiaColors.primaryText,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun ForwardingTextField(
    label: String,
    value: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    error: String? = null,
    onValueChange: (String) -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = ChatforiaColors.primaryText
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            placeholder = {
                Text(placeholder)
            },
            isError = error != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (!error.isNullOrBlank()) {
            ErrorText(error)
        }
    }
}