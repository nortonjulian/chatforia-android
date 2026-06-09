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
import com.chatforia.android.numbers.PhoneNumberView
import com.chatforia.android.wireless.WirelessHomeView
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.draw.alpha
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Image
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.chatforia.android.upload.UploadRepository
import com.chatforia.android.ui.theme.ThemeOption
import com.chatforia.android.ui.theme.AppThemes
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import com.chatforia.android.sounds.AudioPlayerService
import androidx.compose.material.icons.filled.Accessibility

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

    val uploadRepository = remember {
        UploadRepository(
            apiClient = apiClient,
            context = context
        )
    }

    var isUploadingAvatar by remember { mutableStateOf(false) }
    var avatarError by remember { mutableStateOf<String?>(null) }

    var showThemePicker by remember { mutableStateOf(false) }

    var showAccessibility by remember { mutableStateOf(false) }

    val avatarPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            scope.launch {
                isUploadingAvatar = true
                avatarError = null

                try {
                    val response = uploadRepository.uploadAvatar(uri)
                    onUserUpdated(user.copy(avatarUrl = response.avatarUrl))
                } catch (e: Exception) {
                    avatarError = e.message ?: "Could not upload photo."
                } finally {
                    isUploadingAvatar = false
                }
            }
        }

    fun removeAvatar() {
        scope.launch {
            isUploadingAvatar = true
            avatarError = null

            try {
                val response = settingsRepository.removeAvatar()
                onUserUpdated(user.copy(avatarUrl = response.avatarUrl))
            } catch (e: Exception) {
                avatarError = e.message ?: "Could not remove photo."
            } finally {
                isUploadingAvatar = false
            }
        }
    }

    val settingsState by settingsViewModel.state.collectAsState()

    LaunchedEffect(
        settingsState.messageTone,
        settingsState.ringtone,
        settingsState.soundVolume
    ) {
        AudioPlayerService.save(
            context = context,
            messageTone = settingsState.messageTone,
            ringtone = settingsState.ringtone,
            soundVolume = settingsState.soundVolume
        )
    }

    LaunchedEffect(user.theme) {
        ChatforiaColors.applyTheme(user.theme ?: "dawn")
    }

    var showLinkedDevices by remember {
        mutableStateOf(false)
    }

    var showDevicePairing by remember {
        mutableStateOf(false)
    }

    var showPlan by remember { mutableStateOf(false) }

    var showUpgrade by remember { mutableStateOf(false) }

    var showWireless by remember { mutableStateOf(false) }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    var showPhoneNumbers by remember { mutableStateOf(false) }

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
            viewModel = linkedDevicesViewModel,
            accountPublicKey = user.publicKey
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

    if (showWireless) {
        WirelessHomeView(
            apiClient = apiClient,
            onBack = { showWireless = false }
        )
        return
    }

    if (showAccessibility) {
        AccessibilitySettingsScreen(
            state = settingsState,
            onBack = { showAccessibility = false },
            onUpdate = { nextState ->
                settingsViewModel.update { nextState }
            },
            onSave = {
                settingsViewModel.saveAccessibility(onUserUpdated)
            },
            onUpgradeRequired = {
                showAccessibility = false
                showUpgrade = true
            }
        )
        return
    }

    if (showPhoneNumbers) {
        PhoneNumberView(
            apiClient = apiClient,
            user = user,
            onBack = { showPhoneNumbers = false },
            onUpgradeRequired = {
                showPhoneNumbers = false
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

        ProfileHeaderCard(
            user = user,
            isUploadingAvatar = isUploadingAvatar,
            avatarError = avatarError,
            onChangePhoto = {
                avatarPickerLauncher.launch("image/*")
            },
            onRemovePhoto = {
                removeAvatar()
            }
        )

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
                showChevron = true,
                onClick = {
                    showWireless = true
                }
            )

            HorizontalDivider(color = ChatforiaColors.border)

            ProfileRow(
                icon = Icons.Default.Phone,
                title = "Phone Number",
                subtitle = "Manage your Chatforia number",
                showChevron = true,
                onClick = {
                    showPhoneNumbers = true
                }
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
                subtitle = AppThemes.nameFor(settingsState.theme),
                showChevron = true,
                onClick = {
                    showThemePicker = true
                }
            )

            HorizontalDivider(color = ChatforiaColors.border)

            HorizontalDivider(color = ChatforiaColors.border)

            ProfileRow(
                icon = Icons.Default.Accessibility,
                title = "Accessibility",
                subtitle = "Visual alerts, captions, and voice note accessibility",
                showChevron = true,
                onClick = {
                    showAccessibility = true
                }
            )

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
            SettingSwitchRow(
                title = "Allow explicit content",
                subtitle = "",
                checked = settingsState.allowExplicitContent,
                onCheckedChange = { enabled ->
                    settingsViewModel.update { it.copy(allowExplicitContent = enabled) }
                }
            )

            SettingSwitchRow(
                title = "Show read receipts",
                subtitle = "",
                checked = settingsState.showReadReceipts,
                onCheckedChange = { enabled ->
                    settingsViewModel.update {
                        it.copy(showReadReceipts = enabled)
                    }
                }
            )

            SettingSwitchRow(
                title = "Blur messages by default",
                subtitle = "",
                checked = settingsState.privacyBlurEnabled,
                onCheckedChange = { enabled ->
                    settingsViewModel.update { it.copy(privacyBlurEnabled = enabled) }
                }
            )

            SettingSwitchRow(
                title = "Blur when app is unfocused",
                subtitle = "",
                checked = settingsState.privacyBlurOnUnfocus,
                onCheckedChange = { enabled ->
                    settingsViewModel.update { it.copy(privacyBlurOnUnfocus = enabled) }
                }
            )

            SettingSwitchRow(
                title = "Hold to reveal messages",
                subtitle = "",
                checked = settingsState.privacyHoldToReveal,
                onCheckedChange = { enabled ->
                    settingsViewModel.update { it.copy(privacyHoldToReveal = enabled) }
                }
            )

            SettingSwitchRow(
                title = "Notify when messages are copied",
                subtitle = "",
                checked = settingsState.notifyOnCopy,
                onCheckedChange = { enabled ->
                    settingsViewModel.update { it.copy(notifyOnCopy = enabled) }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        ChatforiaSectionCard(title = "Random Chat") {
            Text(
                text = "Your age range",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = ChatforiaColors.primaryText
            )

            Spacer(modifier = Modifier.height(8.dp))

            AgeRangePicker(
                selectedAgeBand = settingsState.ageBand,
                onAgeBandChange = { ageBand ->
                    settingsViewModel.update {
                        it.copy(ageBand = ageBand)
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            SettingSwitchRow(
                title = "Use age-based matching",
                subtitle = "",
                checked = settingsState.wantsAgeFilter,
                enabled = settingsState.ageBand != null,
                onCheckedChange = { enabled ->
                    settingsViewModel.update {
                        it.copy(wantsAgeFilter = enabled)
                    }
                }
            )

            SettingSwitchRow(
                title = "Let Ria remember conversations",
                subtitle = "",
                checked = settingsState.riaRemember,
                onCheckedChange = { enabled ->
                    settingsViewModel.update {
                        it.copy(riaRemember = enabled)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        ChatforiaSectionCard(title = "Voicemail") {
            SettingSwitchRow(
                title = "Forward voicemail to email",
                subtitle = "",
                checked = settingsState.voicemailEnabled,
                onCheckedChange = { enabled ->
                    settingsViewModel.update {
                        it.copy(voicemailEnabled = enabled)
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            SettingsTextField(
                label = "Auto-delete voicemails after (days)",
                value = settingsState.voicemailAutoDeleteDays?.toString() ?: "",
                placeholder = "Keep forever",
                onValueChange = { value ->
                    settingsViewModel.update {
                        it.copy(
                            voicemailAutoDeleteDays =
                                value.filter(Char::isDigit).toIntOrNull()
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            SettingsTextField(
                label = "Forward voicemail to email",
                value = settingsState.voicemailForwardEmail,
                placeholder = "Email address",
                onValueChange = { value ->
                    settingsViewModel.update {
                        it.copy(voicemailForwardEmail = value)
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            SettingsTextField(
                label = "Text fallback greeting",
                value = settingsState.voicemailGreetingText,
                placeholder = "Greeting",
                minLines = 3,
                onValueChange = { value ->
                    settingsViewModel.update {
                        it.copy(voicemailGreetingText = value)
                    }
                }
            )
        }

        ChatforiaSectionCard(title = "Legal & Support") {
            LegalSupportRow("Privacy Policy", "https://chatforia.com/privacy")
            HorizontalDivider(color = ChatforiaColors.border)

            LegalSupportRow("Terms of Service", "https://chatforia.com/legal/terms")
            HorizontalDivider(color = ChatforiaColors.border)

            LegalSupportRow("SMS Policy", "https://chatforia.com/legal/sms")
            HorizontalDivider(color = ChatforiaColors.border)

            LegalSupportRow("Contact Support", "mailto:support@chatforia.com")
        }

        Spacer(modifier = Modifier.height(18.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            ChatforiaGradientButton(
                text = if (settingsState.isSaving) "Saving..." else "Save Settings",
                enabled = !settingsState.isSaving,
                onClick = {
                    settingsViewModel.save(
                        onUserUpdated = onUserUpdated
                    )
                },
                modifier = Modifier
                    .width(220.dp)
                    .height(50.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val deleteBackground =
            if (ChatforiaColors.screenBackground.luminance() > 0.5f)
                Color(0xFFF4DEDA)
            else
                Color(0xFF240A18)

        Button(
            onClick = { showDeleteConfirm = true },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth(0.72f)
                .height(44.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = deleteBackground,
                contentColor = Color(0xFFFF4D57)
            )
        ) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Delete Account")
        }

        Spacer(modifier = Modifier.height(20.dp))

        val logoutBackground =
            if (ChatforiaColors.screenBackground.luminance() > 0.5f)
                Color.White
            else
                ChatforiaColors.cardBackground

        val logoutForeground =
            if (ChatforiaColors.screenBackground.luminance() > 0.5f)
                Color(0xFF2A1D18)
            else
                ChatforiaColors.primaryText

        Button(
            onClick = onLogout,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth(0.72f)
                .height(44.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = logoutBackground,
                contentColor = logoutForeground
            )
        ) {
            Icon(Icons.Default.Logout, contentDescription = "Log out")
            Spacer(Modifier.width(8.dp))
            Text("Log out")
        }
    }

    if (showThemePicker) {
        ThemePickerDialog(
            currentTheme = settingsState.theme,
            currentPlan = user.plan,
            onDismiss = { showThemePicker = false },
            onThemeSelected = { code ->
                settingsViewModel.update {
                    it.copy(theme = code)
                }

                ChatforiaColors.applyTheme(code)

                settingsViewModel.save(
                    onUserUpdated = onUserUpdated
                )
            },
            onUpgradeRequired = {
                showThemePicker = false
                showUpgrade = true
            }
        )
    }
}

@Composable
private fun ThemePickerDialog(
    currentTheme: String,
    currentPlan: String?,
    onDismiss: () -> Unit,
    onThemeSelected: (String) -> Unit,
    onUpgradeRequired: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = ChatforiaColors.accent)
            }
        },
        title = {
            Text(
                text = "Theme",
                color = ChatforiaColors.primaryText,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Choose how Chatforia looks across the app.",
                    color = ChatforiaColors.secondaryText
                )

                Spacer(modifier = Modifier.height(16.dp))

                ThemeOptionGroup(
                    title = "Free",
                    options = AppThemes.all.filter { it.requiredPlan == "FREE" },
                    currentTheme = currentTheme,
                    currentPlan = currentPlan,
                    onThemeSelected = onThemeSelected,
                    onUpgradeRequired = onUpgradeRequired
                )

                Spacer(modifier = Modifier.height(16.dp))

                ThemeOptionGroup(
                    title = "Premium",
                    options = AppThemes.all.filter { it.requiredPlan != "FREE" },
                    currentTheme = currentTheme,
                    currentPlan = currentPlan,
                    onThemeSelected = onThemeSelected,
                    onUpgradeRequired = onUpgradeRequired
                )
            }
        },
        containerColor = ChatforiaColors.cardBackground,
        titleContentColor = ChatforiaColors.primaryText,
        textContentColor = ChatforiaColors.primaryText
    )
}

@Composable
private fun ThemeOptionGroup(
    title: String,
    options: List<ThemeOption>,
    currentTheme: String,
    currentPlan: String?,
    onThemeSelected: (String) -> Unit,
    onUpgradeRequired: () -> Unit
) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = ChatforiaColors.secondaryText,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ChatforiaColors.cardBackground,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                options.forEachIndexed { index, option ->
                    val locked = !AppThemes.canAccess(option.code, currentPlan)
                    val selected = currentTheme == option.code

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (locked) {
                                    onUpgradeRequired()
                                } else {
                                    onThemeSelected(option.code)
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color =
                                    if (locked)
                                        ChatforiaColors.secondaryText.copy(alpha = 0.7f)
                                    else
                                        ChatforiaColors.primaryText
                            )

                            Text(
                                text =
                                    if (locked)
                                        "Requires ${option.requiredPlan.lowercase().replaceFirstChar { char -> char.uppercase() }}"
                                    else
                                        "Available now",
                                style = MaterialTheme.typography.bodySmall,
                                color = ChatforiaColors.secondaryText
                            )
                        }

                        when {
                            selected && !locked -> {
                                Text(
                                    text = "✓",
                                    color = ChatforiaColors.accent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                )
                            }

                            locked -> {
                                Text(
                                    text = "🔒",
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }

                    if (index < options.lastIndex) {
                        HorizontalDivider(color = ChatforiaColors.border)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeaderCard(
    user: UserDto,
    isUploadingAvatar: Boolean,
    avatarError: String?,
    onChangePhoto: () -> Unit,
    onRemovePhoto: () -> Unit
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
                modifier = Modifier.size(88.dp),
                shape = CircleShape,
                color = Color(0xFF123A4A)
            ) {
                if (!user.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = "Profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = user.username?.firstOrNull()?.uppercase() ?: "?",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChatforiaColors.primaryText
                        )
                    }
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

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = user.plan?.uppercase() ?: "FREE",
                style = MaterialTheme.typography.labelMedium,
                color = ChatforiaColors.accent,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            TextButton(
                onClick = onChangePhoto,
                enabled = !isUploadingAvatar,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = ChatforiaColors.accent
                )
            ) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Change Photo")
            }

            if (!user.avatarUrl.isNullOrBlank()) {
                TextButton(
                    onClick = onRemovePhoto,
                    enabled = !isUploadingAvatar,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFFF3B3B)
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Remove Photo")
                }
            }

            if (isUploadingAvatar) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Uploading…",
                    color = ChatforiaColors.secondaryText,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (!avatarError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = avatarError,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall
                )
            }
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
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .alpha(if (enabled) 1f else 0.55f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = ChatforiaColors.primaryText
            )

            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChatforiaColors.secondaryText
                )
            }
        }

        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ChatforiaColors.accent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFC7C7C7),
                uncheckedBorderColor = Color(0xFFC7C7C7)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgeRangePicker(
    selectedAgeBand: String?,
    onAgeBandChange: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val options = listOf(
        null to "Select age range",
        "TEEN_13_17" to "13–17",
        "ADULT_18_24" to "18–24",
        "ADULT_25_34" to "25–34",
        "ADULT_35_49" to "35–49",
        "ADULT_50_PLUS" to "50+"
    )

    val selectedLabel =
        options.firstOrNull { it.first == selectedAgeBand }?.second
            ?: "Select age range"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.menuAnchor(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = ChatforiaColors.accent
            )
        ) {
            Text(selectedLabel)
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
                            option.second,
                            color = ChatforiaColors.primaryText
                        )
                    },
                    onClick = {
                        onAgeBandChange(option.first)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsTextField(
    label: String,
    value: String,
    placeholder: String,
    minLines: Int = 1,
    onValueChange: (String) -> Unit
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = ChatforiaColors.primaryText
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = Color(0xFFBDBDBD)
                )
            },
            minLines = minLines,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = ChatforiaColors.primaryText,
                unfocusedTextColor = ChatforiaColors.primaryText,

                focusedPlaceholderColor = ChatforiaColors.secondaryText,
                unfocusedPlaceholderColor = ChatforiaColors.secondaryText,

                focusedContainerColor = ChatforiaColors.cardBackground,
                unfocusedContainerColor = ChatforiaColors.cardBackground,

                focusedBorderColor = ChatforiaColors.border,
                unfocusedBorderColor = ChatforiaColors.border,

                cursorColor = ChatforiaColors.accent
            )
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

@Composable
fun ChatforiaGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        ChatforiaColors.buttonStart,
                        ChatforiaColors.buttonEnd
                    )
                )
            )
            .clickable(enabled = enabled) {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = ChatforiaColors.buttonForeground,
            fontWeight = FontWeight.Bold
        )
    }
}