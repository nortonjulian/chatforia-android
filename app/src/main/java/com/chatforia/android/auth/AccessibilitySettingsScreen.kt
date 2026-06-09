package com.chatforia.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chatforia.android.auth.SettingsUiState
import com.chatforia.android.ui.components.ChatforiaSectionCard
import com.chatforia.android.ui.theme.ChatforiaColors
import com.chatforia.android.ui.components.SettingSwitchRow


@Composable
fun AccessibilitySettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onUpdate: (SettingsUiState) -> Unit,
    onSave: () -> Unit,
    onUpgradeRequired: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatforiaColors.screenBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
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
                text = "Accessibility",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ChatforiaColors.primaryText
            )
        }

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ChatforiaSectionCard(title = "Accessibility") {
                Text(
                    text = "Options to make Chatforia easier to use without relying on sound.",
                    color = ChatforiaColors.secondaryText,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(12.dp))

                A11yDropdownRow(
                    title = "Interface font size",
                    value = state.a11yUiFont,
                    options = fontOptions,
                    onValueChange = { value ->
                        onUpdate(state.copy(a11yUiFont = value))
                    }
                )

                Text(
                    text = "Starts at normal size. Increase if you prefer larger text in accessibility settings.",
                    color = ChatforiaColors.secondaryText,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            ChatforiaSectionCard(title = "Notifications") {
                SettingSwitchRow(
                    title = "Visual alerts for messages & calls",
                    subtitle = "Show banners and visual indicators so you do not miss activity.",
                    checked = state.a11yVisualAlerts,
                    onCheckedChange = {
                        onUpdate(state.copy(a11yVisualAlerts = it))
                    }
                )

                HorizontalDivider(color = ChatforiaColors.border)

                SettingSwitchRow(
                    title = "Vibrate on new messages",
                    subtitle = "Trigger device vibration with notifications when supported.",
                    checked = state.a11yVibrate,
                    onCheckedChange = {
                        onUpdate(state.copy(a11yVibrate = it))
                    }
                )

                HorizontalDivider(color = ChatforiaColors.border)

                SettingSwitchRow(
                    title = "Flash screen on incoming call",
                    subtitle = "Briefly flash the screen when a call rings.",
                    checked = state.a11yFlashOnCall,
                    onCheckedChange = {
                        onUpdate(state.copy(a11yFlashOnCall = it))
                    }
                )
            }

            ChatforiaSectionCard(title = "Live captions") {
                SettingSwitchRow(
                    title = "Enable live captions",
                    subtitle = "Show real-time captions during calls. Premium required.",
                    checked = state.a11yLiveCaptions,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            onUpgradeRequired()
                        } else {
                            onUpdate(state.copy(a11yLiveCaptions = false))
                        }
                    }
                )

                HorizontalDivider(color = ChatforiaColors.border)

                A11yDropdownRow(
                    title = "Caption font size",
                    value = state.a11yCaptionFont,
                    options = fontOptions,
                    onValueChange = { value ->
                        onUpdate(state.copy(a11yCaptionFont = value))
                    }
                )

                HorizontalDivider(color = ChatforiaColors.border)

                A11yDropdownRow(
                    title = "Caption background",
                    value = state.a11yCaptionBg,
                    options = captionBgOptions,
                    onValueChange = { value ->
                        onUpdate(state.copy(a11yCaptionBg = value))
                    }
                )
            }

            ChatforiaSectionCard(title = "Voice notes") {
                SettingSwitchRow(
                    title = "Auto-transcribe voice notes",
                    subtitle = "Attach a transcript to audio messages you receive.",
                    checked = state.a11yVoiceNoteSTT,
                    onCheckedChange = {
                        onUpdate(state.copy(a11yVoiceNoteSTT = it))
                    }
                )
            }

            ChatforiaGradientButton(
                text = if (state.isSaving) "Saving..." else "Save",
                enabled = !state.isSaving,
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            )

            state.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private val fontOptions = listOf(
    "sm" to "Small",
    "md" to "Medium",
    "lg" to "Large",
    "xl" to "Extra Large"
)

private val captionBgOptions = listOf(
    "light" to "Light",
    "dark" to "Dark",
    "transparent" to "Transparent"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun A11yDropdownRow(
    title: String,
    value: String,
    options: List<Pair<String, String>>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val label = options.firstOrNull { it.first == value }?.second ?: value

    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = ChatforiaColors.primaryText
        )

        Spacer(Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            ) {
                Text(
                    text = label,
                    modifier = Modifier.weight(1f)
                )
            }

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(ChatforiaColors.cardBackground)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option.second,
                                color = ChatforiaColors.primaryText
                            )
                        },
                        onClick = {
                            onValueChange(option.first)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}