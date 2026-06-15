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
import androidx.compose.ui.res.stringResource
import com.chatforia.android.R


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
                    contentDescription = stringResource(R.string.android_plan_back),
                    tint = ChatforiaColors.primaryText
                )
            }

            Text(
                text = stringResource(R.string.android_profile_accessibility),
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
            ChatforiaSectionCard(title = stringResource(R.string.android_profile_accessibility)) {
                Text(
                    text = stringResource(R.string.android_accessibility_settings_options_to_make_easier_to_use_without_relying_on),
                    color = ChatforiaColors.secondaryText,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(12.dp))

                A11yDropdownRow(
                    title = stringResource(R.string.android_accessibility_settings_interface_font_size),
                    value = state.a11yUiFont,
                    options = fontOptions,
                    onValueChange = { value ->
                        onUpdate(state.copy(a11yUiFont = value))
                    }
                )

                Text(
                    text = stringResource(R.string.android_accessibility_settings_starts_at_normal_size_increase_if_you_prefer_lar),
                    color = ChatforiaColors.secondaryText,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            ChatforiaSectionCard(title = stringResource(R.string.android_accessibility_settings_notifications)) {
                SettingSwitchRow(
                    title = stringResource(R.string.android_accessibility_settings_visual_alerts_for_messages_calls),
                    subtitle = stringResource(R.string.android_accessibility_settings_show_banners_and_visual_indicators_so_you_do_not),
                    checked = state.a11yVisualAlerts,
                    onCheckedChange = {
                        onUpdate(state.copy(a11yVisualAlerts = it))
                    }
                )

                HorizontalDivider(color = ChatforiaColors.border)

                SettingSwitchRow(
                    title = stringResource(R.string.android_accessibility_settings_vibrate_on_new_messages),
                    subtitle = stringResource(R.string.android_accessibility_settings_trigger_device_vibration_with_notifications_when),
                    checked = state.a11yVibrate,
                    onCheckedChange = {
                        onUpdate(state.copy(a11yVibrate = it))
                    }
                )

                HorizontalDivider(color = ChatforiaColors.border)

                SettingSwitchRow(
                    title = stringResource(R.string.android_accessibility_settings_flash_screen_on_incoming_call),
                    subtitle = stringResource(R.string.android_accessibility_settings_briefly_flash_the_screen_when_a_call_rings),
                    checked = state.a11yFlashOnCall,
                    onCheckedChange = {
                        onUpdate(state.copy(a11yFlashOnCall = it))
                    }
                )
            }

            ChatforiaSectionCard(title = stringResource(R.string.android_accessibility_settings_live_captions)) {
                SettingSwitchRow(
                    title = stringResource(R.string.android_accessibility_settings_enable_live_captions),
                    subtitle = stringResource(R.string.android_accessibility_settings_show_real_time_captions_during_calls_premium_req),
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
                    title = stringResource(R.string.android_accessibility_settings_caption_font_size),
                    value = state.a11yCaptionFont,
                    options = fontOptions,
                    onValueChange = { value ->
                        onUpdate(state.copy(a11yCaptionFont = value))
                    }
                )

                HorizontalDivider(color = ChatforiaColors.border)

                A11yDropdownRow(
                    title = stringResource(R.string.android_accessibility_settings_caption_background),
                    value = state.a11yCaptionBg,
                    options = captionBgOptions,
                    onValueChange = { value ->
                        onUpdate(state.copy(a11yCaptionBg = value))
                    }
                )
            }

            ChatforiaSectionCard(title = stringResource(R.string.android_accessibility_settings_voice_notes)) {
                SettingSwitchRow(
                    title = stringResource(R.string.android_accessibility_settings_auto_transcribe_voice_notes),
                    subtitle = stringResource(R.string.android_accessibility_settings_attach_a_transcript_to_audio_messages_you_receiv),
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