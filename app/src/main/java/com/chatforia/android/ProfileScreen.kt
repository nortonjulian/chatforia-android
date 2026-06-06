package com.chatforia.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatforia.android.ui.theme.ChatforiaColors
import androidx.compose.material.icons.filled.AutoAwesome
import com.chatforia.android.ui.components.ChatforiaSectionCard
import com.chatforia.android.auth.UserDto
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.chatforia.android.crypto.KeyBackupCrypto
import com.chatforia.android.crypto.KeySetupScreen
import com.chatforia.android.crypto.KeySetupViewModel
import com.chatforia.android.crypto.KeyStorage
import com.chatforia.android.crypto.RemoteKeyBackupRepository
import com.chatforia.android.network.ApiClient
import com.chatforia.android.auth.AuthRepository
import com.chatforia.android.crypto.AccountKeyManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.chatforia.android.auth.SettingsRepository
import com.chatforia.android.auth.SettingsViewModel
import com.chatforia.android.auth.LanguageSelectionView
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import com.chatforia.android.crypto.LinkedDevicesScreen
import com.chatforia.android.crypto.LinkedDevicesViewModel
import com.chatforia.android.crypto.DevicePairingScreen
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.clickable
import androidx.compose.runtime.setValue
import com.chatforia.android.billing.UpgradeView
import com.chatforia.android.billing.PlanView
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import com.chatforia.android.sounds.SoundSettingsView

@Composable
fun ProfileScreen(
    user: UserDto,
    apiClient: ApiClient,
    authRepository: AuthRepository,
    onLogout: () -> Unit,
    settingsRepository: SettingsRepository,
    linkedDevicesViewModel: LinkedDevicesViewModel,
    onUserUpdated: (UserDto) -> Unit,
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

    val settingsViewModel = remember {
        SettingsViewModel(settingsRepository)
    }

    val scope = rememberCoroutineScope()

    val settingsState by settingsViewModel.state.collectAsState()

    var showLinkedDevices by remember {
        mutableStateOf(false)
    }

    var showDevicePairing by remember {
        mutableStateOf(false)
    }

    var showPlan by remember { mutableStateOf(false) }

    var showUpgrade by remember { mutableStateOf(false) }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(user.id) {
        settingsViewModel.load(user)
    }

    val keySetupViewModel =
        remember {
            KeySetupViewModel(
                remoteKeyBackupRepository =
                    RemoteKeyBackupRepository(apiClient),
                keyStorage = keyStorage,
                authRepository = authRepository,
                accountKeyManager = accountKeyManager,
                keyBackupCrypto = KeyBackupCrypto()
            )
        }

    if (showLinkedDevices) {

        LinkedDevicesScreen(
            viewModel = linkedDevicesViewModel
        )

        return
    }

    if (showDevicePairing) {

        DevicePairingScreen(
            onOpenLinkedDevices = {
                showDevicePairing = false
                showLinkedDevices = true
            }
        )

        return
    }

    if (showUpgrade) {
        UpgradeView(
            apiClient = apiClient,
            onClose = { showUpgrade = false }
        )
        return
    }

    if (showPlan) {
        PlanView(
            user = user,
            onBack = { showPlan = false },
            onUpgrade = {
                showPlan = false
                showUpgrade = true
            }
        )
        return
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Account") },
            text = { Text("This action cannot be undone. Are you sure you want to delete your account?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false

                        scope.launch {
                            try {
                                settingsRepository.deleteAccount()
                                onLogout()
                            } catch (e: Exception) {
                                println("Failed to delete account: ${e.message}")
                            }
                        }
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatforiaColors.screenBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "Profile",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = ChatforiaColors.primaryText,
            modifier = Modifier.padding(top = 20.dp, bottom = 18.dp)
        )

        ProfileHeaderCard(user)

        Spacer(modifier = Modifier.height(16.dp))

        UpgradeCard(
            onClick = { showUpgrade = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        ChatforiaSectionCard(title = "Account") {
            ProfileRow(
                icon = Icons.Default.Person,
                title = "Username",
                subtitle = user.username ?: "Unknown"
            )

            HorizontalDivider(color = ChatforiaColors.border)

            ProfileRow(
                icon = Icons.Default.Email,
                title = "Email",
                subtitle = user.email ?: "No email"
            )

            HorizontalDivider(color = ChatforiaColors.border)

            ProfileRow(
                icon = Icons.Default.Star,
                title = "Plan",
                subtitle = user.plan ?: "Free"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        ChatforiaSectionCard(title = "Plan") {
            ProfileRow(
                icon = Icons.Default.CreditCard,
                title = "Plan & Billing",
                subtitle = "Current plan: ${user.plan ?: "Free"}",
                showChevron = true,
                onClick = { showPlan = true }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        ChatforiaSectionCard(title = "Wireless") {
            ProfileRow(
                icon = Icons.Default.Wifi,
                title = "Chatforia Mobile",
                subtitle = "eSIM and wireless settings",
                showChevron = true
            )

            HorizontalDivider(color = ChatforiaColors.border)

            ProfileRow(
                icon = Icons.Default.Phone,
                title = "Phone Number",
                subtitle = "Manage your Chatforia number",
                showChevron = true
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        ChatforiaSectionCard(title = "Settings") {
            Text(
                text = "Preferred Language",
                style = MaterialTheme.typography.bodyLarge,
                color = ChatforiaColors.primaryText
            )

            Spacer(modifier = Modifier.height(10.dp))

            LanguageSelectionView(
                selectedLanguage = settingsState.preferredLanguage,
                onLanguageChange = { code ->
                    settingsViewModel.update {
                        it.copy(preferredLanguage = code)
                    }
                }
            )

            HorizontalDivider(color = ChatforiaColors.border)

            SettingSwitchRow(
                title = "Auto-translate messages",
                subtitle = "Automatically translate messages into your preferred language.",
                checked = settingsState.autoTranslate,
                onCheckedChange = { enabled ->
                    settingsViewModel.update {
                        it.copy(autoTranslate = enabled)
                    }
                }
            )

            HorizontalDivider(color = ChatforiaColors.border)

            SettingSwitchRow(
                title = "Show original with translation",
                subtitle = "Show the original message alongside the translation.",
                checked = settingsState.showOriginalWithTranslation,
                onCheckedChange = { enabled ->
                    settingsViewModel.update {
                        it.copy(showOriginalWithTranslation = enabled)
                    }
                }
            )

            KeySetupScreen(
                viewModel = keySetupViewModel
            )

            HorizontalDivider(color = ChatforiaColors.border)

            ProfileRow(
                icon = Icons.Default.Security,
                title = "Linked Devices",
                subtitle = "Manage trusted devices",
                showChevron = true,
                onClick = {
                    showLinkedDevices = true
                }
            )

            HorizontalDivider(color = ChatforiaColors.border)

            ProfileRow(
                icon = Icons.Default.Phone,
                title = "Pair New Device",
                subtitle = "Approve a new device",
                showChevron = true,
                onClick = {
                    showDevicePairing = true
                }
            )

            ProfileRow(
                icon = Icons.Default.Settings,
                title = "Appearance",
                subtitle = user.theme ?: "Dawn",
                showChevron = true
            )

            HorizontalDivider(color = ChatforiaColors.border)

            SoundSettingsView(
                currentPlan = user.plan,
                state = settingsState,
                onMessageToneChange = { tone ->
                    settingsViewModel.update {
                        it.copy(messageTone = tone)
                    }
                },
                onRingtoneChange = { ringtone ->
                    settingsViewModel.update {
                        it.copy(ringtone = ringtone)
                    }
                },
                onVolumeChange = { volume ->
                    settingsViewModel.update {
                        it.copy(soundVolume = volume)
                    }
                },
                onUpgradeRequired = {
                    showUpgrade = true
                }
            )

            HorizontalDivider(color = ChatforiaColors.border)

            SoundSettingsView(
                currentPlan = user.plan,
                state = settingsState,
                onMessageToneChange = { tone ->
                    settingsViewModel.update {
                        it.copy(messageTone = tone)
                    }
                },
                onRingtoneChange = { ringtone ->
                    settingsViewModel.update {
                        it.copy(ringtone = ringtone)
                    }
                },
                onVolumeChange = { volume ->
                    settingsViewModel.update {
                        it.copy(soundVolume = volume)
                    }
                },
                onUpgradeRequired = {
                    showUpgrade = true
                }
            )

            HorizontalDivider(color = ChatforiaColors.border)

            ProfileRow(
                icon = Icons.Default.Security,
                title = "Security",
                subtitle = "Encryption key protected",
                showChevron = true
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        ChatforiaSectionCard(title = "Privacy") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Read Receipts",
                        style = MaterialTheme.typography.bodyLarge,
                        color = ChatforiaColors.primaryText
                    )

                    Text(
                        text = "Let others see when you've read messages",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChatforiaColors.secondaryText
                    )
                }

                Switch(
                    checked = settingsState.showReadReceipts,
                    onCheckedChange = { enabled ->
                        settingsViewModel.update {
                            it.copy(showReadReceipts = enabled)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ChatforiaSectionCard(title = "Legal & Support") {
            LegalSupportRow("Privacy Policy", "https://chatforia.com/privacy")
            HorizontalDivider(color = ChatforiaColors.border)

            LegalSupportRow("Terms of Service", "https://chatforia.com/legal/terms")
            HorizontalDivider(color = ChatforiaColors.border)

            LegalSupportRow("SMS Policy", "https://chatforia.com/legal/sms")
            HorizontalDivider(color = ChatforiaColors.border)

            LegalSupportRow("Contact Support", "mailto:support@chatforia.com")
        }

        Button(
            onClick = {
                settingsViewModel.save(
                    onUserUpdated = onUserUpdated
                )
            },
            enabled = !settingsState.isSaving,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                if (settingsState.isSaving)
                    "Saving..."
                else
                    "Save Settings"
            )
        }

        Button(
            onClick = { showDeleteConfirm = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = androidx.compose.ui.graphics.Color(0xFFFFE1E1),
                contentColor = androidx.compose.ui.graphics.Color(0xFFFF3B3B)
            )
        ) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Delete Account")
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = ChatforiaColors.accent
            )
        ) {
            Icon(
                Icons.Default.Logout,
                contentDescription = "Log out"
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text("Log out")
        }
    }
}

@Composable
private fun ProfileHeaderCard(
    user: UserDto
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = ChatforiaColors.cardBackground,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(76.dp),
                shape = CircleShape,
                color = ChatforiaColors.highlightedSurface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text =
                            user.username
                                ?.firstOrNull()
                                ?.uppercase()
                                ?: "?",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChatforiaColors.primaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = user.username ?: "Unknown",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ChatforiaColors.primaryText
            )

            Text(
                text = user.email ?: "No email",
                style = MaterialTheme.typography.bodyMedium,
                color = ChatforiaColors.secondaryText
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text =
                    user.plan
                        ?.uppercase()
                        ?: "FREE",
                style = MaterialTheme.typography.labelMedium,
                color = ChatforiaColors.accent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun UpgradeCard(
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ChatforiaColors.cardBackground,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = "Upgrade",
                tint = ChatforiaColors.accent
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Upgrade",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = ChatforiaColors.primaryText
                )

                Text(
                    text = "Choose Plus or Premium",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChatforiaColors.secondaryText
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Open upgrade",
                tint = ChatforiaColors.secondaryText
            )
        }
    }
}

@Composable
private fun ProfileRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = ChatforiaColors.accent,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = ChatforiaColors.primaryText
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = ChatforiaColors.secondaryText
            )
        }

        if (showChevron) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = ChatforiaColors.secondaryText
            )
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = ChatforiaColors.primaryText
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = ChatforiaColors.secondaryText
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun LegalSupportRow(
    title: String,
    url: String
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = ChatforiaColors.primaryText,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.Default.OpenInNew,
            contentDescription = "Open",
            tint = ChatforiaColors.secondaryText
        )
    }
}