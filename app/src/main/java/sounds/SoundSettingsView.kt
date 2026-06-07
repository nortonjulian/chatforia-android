package com.chatforia.android.sounds

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chatforia.android.auth.SettingsUiState
import com.chatforia.android.ui.theme.ChatforiaColors

@Composable
fun SoundSettingsView(
    currentPlan: String?,
    state: SettingsUiState,
    onMessageToneChange: (String) -> Unit,
    onRingtoneChange: (String) -> Unit,
    onVolumeChange: (Int) -> Unit,
    onUpgradeRequired: () -> Unit = {}
) {
    val context = LocalContext.current
    val player = remember { AudioPlayerService(context) }

    var showMessagePicker by remember { mutableStateOf(false) }
    var showRingtonePicker by remember { mutableStateOf(false) }

    val planName = currentPlan?.uppercase() ?: "FREE"
    val isPremium = planName == "PREMIUM"

    DisposableEffect(Unit) {
        onDispose { player.stop() }
    }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(
            text = "Free includes basic sounds. Upgrade to unlock more tones and ringtones.",
            color = ChatforiaColors.secondaryText,
            style = MaterialTheme.typography.bodyMedium
        )

        SoundSummaryRow(
            title = "Message Tone",
            value = state.messageTone.removeSuffix(".mp3"),
            planLabel = if (isPremium) "Premium" else "Free",
            onClick = { showMessagePicker = true }
        )

        SoundSummaryRow(
            title = "Ringtone",
            value = state.ringtone.removeSuffix(".mp3"),
            planLabel = if (isPremium) "Premium" else "Free",
            onClick = { showRingtonePicker = true }
        )

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Volume",
                    color = ChatforiaColors.primaryText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "${state.soundVolume}%",
                    color = ChatforiaColors.secondaryText
                )
            }

            Slider(
                value = state.soundVolume.toFloat(),
                onValueChange = { onVolumeChange(it.toInt()) },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = ChatforiaColors.accent,
                    activeTrackColor = ChatforiaColors.accent,
                    inactiveTrackColor = ChatforiaColors.highlightedSurface
                )
            )
        }
    }

    if (showMessagePicker) {
        SoundPickerDialog(
            title = "Message Tone",
            currentPlan = planName,
            selectedFilename = state.messageTone,
            options = AppMessageTones.all,
            isPremium = isPremium,
            onDismiss = { showMessagePicker = false },
            onSelect = { option ->
                onMessageToneChange(option.filename)
                player.playMessageTone(option.filename, state.soundVolume)
            },
            onLockedTap = onUpgradeRequired
        )
    }

    if (showRingtonePicker) {
        SoundPickerDialog(
            title = "Ringtone",
            currentPlan = planName,
            selectedFilename = state.ringtone,
            options = AppRingtones.all,
            isPremium = isPremium,
            onDismiss = { showRingtonePicker = false },
            onSelect = { option ->
                onRingtoneChange(option.filename)
                player.playRingtone(option.filename, state.soundVolume)
            },
            onLockedTap = onUpgradeRequired
        )
    }
}

@Composable
private fun SoundSummaryRow(
    title: String,
    value: String,
    planLabel: String,
    onClick: () -> Unit
) {
    Column {
        Text(
            text = title,
            color = ChatforiaColors.primaryText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            shape = RoundedCornerShape(18.dp),
            color = ChatforiaColors.cardBackground,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = value,
                        color = ChatforiaColors.primaryText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = planLabel,
                        color = ChatforiaColors.secondaryText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = ChatforiaColors.secondaryText
                )
            }
        }
    }
}

@Composable
private fun SoundPickerDialog(
    title: String,
    currentPlan: String,
    selectedFilename: String,
    options: List<SoundOption>,
    isPremium: Boolean,
    onDismiss: () -> Unit,
    onSelect: (SoundOption) -> Unit,
    onLockedTap: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ChatforiaColors.cardBackground,
        titleContentColor = ChatforiaColors.primaryText,
        textContentColor = ChatforiaColors.secondaryText,
        title = {
            Text(
                text = title,
                color = ChatforiaColors.primaryText,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Current plan: $currentPlan",
                    color = ChatforiaColors.secondaryText,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "FREE",
                    color = ChatforiaColors.secondaryText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                options
                    .filter { it.requiredPlan == RequiredPlan.Free }
                    .forEach { option ->
                        SoundPickerRow(
                            option = option,
                            selectedFilename = selectedFilename,
                            locked = false,
                            onSelect = onSelect,
                            onLockedTap = onLockedTap
                        )
                    }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "PREMIUM",
                    color = ChatforiaColors.secondaryText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                options
                    .filter { it.requiredPlan == RequiredPlan.Premium }
                    .forEach { option ->
                        SoundPickerRow(
                            option = option,
                            selectedFilename = selectedFilename,
                            locked = !isPremium,
                            onSelect = onSelect,
                            onLockedTap = onLockedTap
                        )
                    }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Done",
                    color = ChatforiaColors.accent
                )
            }
        }
    )
}

@Composable
private fun SoundPickerRow(
    option: SoundOption,
    selectedFilename: String,
    locked: Boolean,
    onSelect: (SoundOption) -> Unit,
    onLockedTap: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (locked) {
                    onLockedTap()
                } else {
                    onSelect(option)
                }
            }
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = option.label,
                color =
                    if (locked)
                        ChatforiaColors.secondaryText
                    else
                        ChatforiaColors.primaryText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = if (locked) "Requires Premium" else "Available now",
                color = ChatforiaColors.secondaryText,
                style = MaterialTheme.typography.bodySmall
            )
        }

        when {
            locked -> {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = ChatforiaColors.secondaryText
                )
            }

            selectedFilename == option.filename -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = ChatforiaColors.accent
                )
            }
        }
    }
}