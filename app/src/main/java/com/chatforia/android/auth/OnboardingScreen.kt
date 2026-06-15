package com.chatforia.android.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chatforia.android.ui.theme.ChatforiaColors
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.chatforia.android.R

@Composable
fun OnboardingScreen(
    user: UserDto,
    settingsRepository: SettingsRepository,
    onUserUpdated: (UserDto) -> Unit,
    onComplete: (UserDto) -> Unit
) {
    var step by remember { mutableStateOf(0) }
    var username by remember {
        mutableStateOf(
            user.username
                ?.takeUnless {
                    it.lowercase().startsWith("user_") ||
                            it.lowercase().startsWith("pending_")
                }
                .orEmpty()
        )
    }
    var selectedLanguage by remember {
        mutableStateOf(user.preferredLanguage ?: "en")
    }
    var currentUser by remember { mutableStateOf(user) }
    var isSaving by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val totalSteps = 4

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatforiaColors.screenBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.android_onboarding_welcome),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = ChatforiaColors.primaryText
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.width(240.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(totalSteps) { index ->
                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .weight(1f)
                        .background(
                            color = if (index <= step) {
                                ChatforiaColors.accent
                            } else {
                                ChatforiaColors.border
                            },
                            shape = RoundedCornerShape(50)
                        )
                )
            }
        }

        errorText?.let {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ChatforiaColors.cardBackground,
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 8.dp,
            tonalElevation = 2.dp
        ) {
            Box(
                modifier = Modifier.padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                when (step) {
                    0 -> WelcomeStep()
                    1 -> UsernameStep(
                        username = username,
                        onUsernameChange = { username = it }
                    )
                    2 -> LanguageStep(
                        selectedLanguage = selectedLanguage,
                        onLanguageChange = { selectedLanguage = it }
                    )
                    else -> ReadyStep()
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step > 0) {
                OutlinedButton(
                    onClick = {
                        errorText = null
                        step -= 1
                    },
                    enabled = !isSaving
                ) {
                    Text(stringResource(R.string.android_plan_back))
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    scope.launch {
                        errorText = null

                        when (step) {
                            0 -> step = 1

                            1 -> {
                                val trimmed =
                                    username.trim().lowercase()

                                if (trimmed.isBlank()) {
                                    errorText = "Choose a username."
                                    return@launch
                                }

                                if (trimmed.length < 3) {
                                    errorText =
                                        "Username must be at least 3 characters."
                                    return@launch
                                }

                                isSaving = true

                                try {
                                    val updated =
                                        settingsRepository.updateUsername(
                                            trimmed
                                        )

                                    currentUser = updated
                                    onUserUpdated(updated)
                                    step = 2
                                } catch (error: Exception) {
                                    errorText =
                                        error.message
                                            ?: "Could not save username."
                                } finally {
                                    isSaving = false
                                }
                            }

                            2 -> {
                                isSaving = true

                                try {
                                    val updated =
                                        settingsRepository.updateSettings(
                                            SettingsUpdateRequest(
                                                preferredLanguage = selectedLanguage,
                                                uiLanguage = selectedLanguage,

                                                autoTranslate = currentUser.autoTranslate ?: false,
                                                showOriginalWithTranslation =
                                                    currentUser.showOriginalWithTranslation ?: false,

                                                theme = currentUser.theme ?: "Dawn",
                                                allowExplicitContent = currentUser.allowExplicitContent ?: false,
                                                showReadReceipts = currentUser.showReadReceipts ?: false,
                                                autoDeleteSeconds = currentUser.autoDeleteSeconds ?: 0,

                                                privacyBlurEnabled = currentUser.privacyBlurEnabled ?: false,
                                                privacyBlurOnUnfocus = currentUser.privacyBlurOnUnfocus ?: false,
                                                privacyHoldToReveal = currentUser.privacyHoldToReveal ?: false,
                                                notifyOnCopy = currentUser.notifyOnCopy ?: false,

                                                riaRemember = currentUser.riaRemember ?: true,

                                                voicemailEnabled = currentUser.voicemailEnabled ?: true,
                                                voicemailAutoDeleteDays = currentUser.voicemailAutoDeleteDays,
                                                voicemailForwardEmail =
                                                    currentUser.voicemailForwardEmail
                                                        ?: currentUser.email.orEmpty(),
                                                voicemailGreetingText =
                                                    currentUser.voicemailGreetingText
                                                        ?: currentUser.voicemailGreeting
                                                        ?: "",

                                                enableSmartReplies =
                                                    currentUser.enableSmartReplies
                                                        ?: currentUser.smartRepliesEnabled
                                                        ?: true,

                                                maskAIProfanity =
                                                    currentUser.maskAIProfanity
                                                        ?: currentUser.profanityMaskEnabled
                                                        ?: false,

                                                messageTone =
                                                    currentUser.messageTone
                                                        ?: currentUser.messageSound
                                                        ?: currentUser.tone
                                                        ?: "Default.mp3",

                                                ringtone = currentUser.ringtone ?: "Classic.mp3",
                                                soundVolume = currentUser.soundVolume?.toInt() ?: 70
                                            )
                                        )

                                    currentUser = updated
                                    onUserUpdated(updated)
                                    step = 3
                                } catch (error: Exception) {
                                    errorText =
                                        error.message
                                            ?: "Could not save language."
                                } finally {
                                    isSaving = false
                                }
                            }

                            else -> {
                                onComplete(currentUser)
                            }
                        }
                    }
                },
                enabled = !isSaving
            ) {
                Text(
                    if (isSaving) {
                        "Saving..."
                    } else {
                        when (step) {
                            0 -> "Continue"
                            1 -> "Save username"
                            2 -> "Save language"
                            else -> "Start chatting"
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = ChatforiaColors.accent,
            modifier = Modifier.size(44.dp)
        )

        Text(
            text = stringResource(R.string.android_login_welcome_to),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = ChatforiaColors.primaryText,
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(R.string.android_onboarding_set_up_your_profile_so_can_personalize_your_expe),
            color = ChatforiaColors.secondaryText,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun UsernameStep(
    username: String,
    onUsernameChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = stringResource(R.string.android_onboarding_choose_a_username),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = ChatforiaColors.primaryText,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = stringResource(R.string.android_onboarding_this_is_how_people_will_recognize_you_in),
            color = ChatforiaColors.secondaryText,
            textAlign = TextAlign.Center
        )

        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text(stringResource(R.string.android_profile_username)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None
            ),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun LanguageStep(
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = stringResource(R.string.android_onboarding_choose_your_language),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = ChatforiaColors.primaryText,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = stringResource(R.string.android_onboarding_will_use_this_for_translations_and_your_app_expe),
            color = ChatforiaColors.secondaryText,
            textAlign = TextAlign.Center
        )

        LanguageSelectionView(
            selectedLanguage = selectedLanguage,
            onLanguageChange = onLanguageChange
        )
    }
}

@Composable
private fun ReadyStep() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.ChatBubble,
            contentDescription = null,
            tint = ChatforiaColors.accent,
            modifier = Modifier.size(44.dp)
        )

        Text(
            text = stringResource(R.string.android_onboarding_you_re_ready),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = ChatforiaColors.primaryText,
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(R.string.android_onboarding_start_chatting_calling_translating_and_connectin),
            color = ChatforiaColors.secondaryText,
            textAlign = TextAlign.Center
        )
    }
}