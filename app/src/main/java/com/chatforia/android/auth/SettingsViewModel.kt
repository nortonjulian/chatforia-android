package com.chatforia.android.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.chatforia.android.sounds.resolvedMessageToneForPlan
import com.chatforia.android.sounds.resolvedRingtoneForPlan
import com.chatforia.android.ui.theme.AppThemes

data class SettingsUiState(
    val preferredLanguage: String = "en",
    val autoTranslate: Boolean = false,
    val showOriginalWithTranslation: Boolean = false,
    val theme: String = "dawn",
    val allowExplicitContent: Boolean = false,
    val showReadReceipts: Boolean = false,
    val autoDeleteSeconds: Int = 0,

    val a11yUiFont: String = "md",
    val a11yVisualAlerts: Boolean = false,
    val a11yVibrate: Boolean = false,
    val a11yFlashOnCall: Boolean = false,
    val a11yLiveCaptions: Boolean = false,
    val a11yVoiceNoteSTT: Boolean = false,
    val a11yCaptionFont: String = "lg",
    val a11yCaptionBg: String = "dark",

    val privacyBlurEnabled: Boolean = false,
    val privacyBlurOnUnfocus: Boolean = false,
    val privacyHoldToReveal: Boolean = false,
    val notifyOnCopy: Boolean = false,

    val riaRemember: Boolean = true,
    val enableSmartReplies: Boolean = true,
    val maskAIProfanity: Boolean = false,

    val messageTone: String = "Default.mp3",
    val ringtone: String = "Classic.mp3",
    val soundVolume: Int = 70,

    val isSaving: Boolean = false,
    val error: String? = null,
    val success: String? = null,

    val ageBand: String? = null,
    val wantsAgeFilter: Boolean = true,
    val randomChatAllowedBands: List<String> = emptyList(),

    val voicemailEnabled: Boolean = true,
    val voicemailAutoDeleteDays: Int? = null,
    val voicemailForwardEmail: String = "",
    val voicemailGreetingText: String = "",
)

class SettingsViewModel(
    private val repository: UserSettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state

    fun load(user: UserDto) {
        _state.value = SettingsUiState(
            preferredLanguage = user.preferredLanguage ?: "en",
            autoTranslate = user.autoTranslate ?: false,
            showOriginalWithTranslation =
                user.showOriginalWithTranslation ?: false,
            theme = AppThemes.resolvedThemeForPlan(
                code = user.theme,
                plan = user.plan
            ),
            allowExplicitContent = user.allowExplicitContent ?: false,
            showReadReceipts =
                user.showReadReceipts ?: false,
            autoDeleteSeconds = user.autoDeleteSeconds ?: 0,
            privacyBlurEnabled = user.privacyBlurEnabled ?: false,
            privacyBlurOnUnfocus = user.privacyBlurOnUnfocus ?: false,
            privacyHoldToReveal = user.privacyHoldToReveal ?: false,
            notifyOnCopy = user.notifyOnCopy ?: false,
            riaRemember = user.riaRemember ?: true,
            enableSmartReplies =
                user.enableSmartReplies
                    ?: user.smartRepliesEnabled
                    ?: true,

            maskAIProfanity =
                user.maskAIProfanity
                    ?: user.profanityMaskEnabled
                    ?: false,

            messageTone = resolvedMessageToneForPlan(
                filename = user.messageTone ?: user.messageSound ?: user.tone,
                plan = user.plan
            ),
            ringtone = resolvedRingtoneForPlan(
                filename = user.ringtone,
                plan = user.plan
            ),
            soundVolume = user.soundVolume?.toInt() ?: 70,
            ageBand = user.ageBand,
            wantsAgeFilter = user.wantsAgeFilter ?: true,
            randomChatAllowedBands = user.randomChatAllowedBands ?: emptyList(),

            voicemailEnabled = user.voicemailEnabled ?: true,
            voicemailAutoDeleteDays = user.voicemailAutoDeleteDays,
            voicemailForwardEmail =
                user.voicemailForwardEmail ?: user.email ?: "",
            voicemailGreetingText =
                user.voicemailGreetingText ?: user.voicemailGreeting ?: "",

            a11yUiFont = user.a11yUiFont ?: "md",
            a11yVisualAlerts = user.a11yVisualAlerts ?: false,
            a11yVibrate = user.a11yVibrate ?: false,
            a11yFlashOnCall = user.a11yFlashOnCall ?: false,
            a11yLiveCaptions = user.a11yLiveCaptions ?: false,
            a11yVoiceNoteSTT = user.a11yVoiceNoteSTT ?: false,
            a11yCaptionFont = user.a11yCaptionFont ?: "lg",
            a11yCaptionBg = user.a11yCaptionBg ?: "dark",
        )
    }

    fun saveAccessibility(onUserUpdated: (UserDto) -> Unit) {
        viewModelScope.launch {
            val current = _state.value
            _state.value = current.copy(isSaving = true, error = null, success = null)

            try {
                val updatedUser = repository.updateAccessibility(
                    AccessibilitySettingsUpdateRequest(
                        a11yUiFont = current.a11yUiFont,
                        a11yVisualAlerts = current.a11yVisualAlerts,
                        a11yVibrate = current.a11yVibrate,
                        a11yFlashOnCall = current.a11yFlashOnCall,
                        a11yLiveCaptions = current.a11yLiveCaptions,
                        a11yVoiceNoteSTT = current.a11yVoiceNoteSTT,
                        a11yCaptionFont = current.a11yCaptionFont,
                        a11yCaptionBg = current.a11yCaptionBg,
                    )
                )

                onUserUpdated(updatedUser)
                load(updatedUser)

                _state.value = _state.value.copy(
                    isSaving = false,
                    success = "Accessibility settings saved."
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = e.message ?: "Failed to save accessibility settings."
                )
            }
        }
    }

    fun update(transform: (SettingsUiState) -> SettingsUiState) {
        _state.value = transform(_state.value)
    }

    fun save(onUserUpdated: (UserDto) -> Unit) {
        viewModelScope.launch {
            val current = _state.value
            _state.value = current.copy(isSaving = true, error = null, success = null)

            try {
                val updatedUser = repository.updateSettings(
                    SettingsUpdateRequest(
                        preferredLanguage = current.preferredLanguage,
                        autoTranslate = current.autoTranslate,
                        showOriginalWithTranslation = current.showOriginalWithTranslation,
                        theme = current.theme,
                        allowExplicitContent = current.allowExplicitContent,
                        showReadReceipts = current.showReadReceipts,
                        autoDeleteSeconds = current.autoDeleteSeconds,
                        privacyBlurEnabled = current.privacyBlurEnabled,
                        privacyBlurOnUnfocus = current.privacyBlurOnUnfocus,
                        privacyHoldToReveal = current.privacyHoldToReveal,
                        notifyOnCopy = current.notifyOnCopy,
                        riaRemember = current.riaRemember,
                        enableSmartReplies = current.enableSmartReplies,
                        maskAIProfanity = current.maskAIProfanity,
                        messageTone = current.messageTone,
                        ringtone = current.ringtone,
                        soundVolume = current.soundVolume,
                        ageBand = current.ageBand,
                        wantsAgeFilter = current.wantsAgeFilter,
                        randomChatAllowedBands =
                            current.randomChatAllowedBands,

                        voicemailEnabled = current.voicemailEnabled,
                        voicemailAutoDeleteDays =
                            current.voicemailAutoDeleteDays,
                        voicemailForwardEmail =
                            current.voicemailForwardEmail,
                        voicemailGreetingText =
                            current.voicemailGreetingText,

                        uiLanguage = current.preferredLanguage,
                    )
                )

                onUserUpdated(updatedUser)

                _state.value = _state.value.copy(
                    isSaving = false,
                    success = "Settings saved."
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = e.message ?: "Failed to save settings."
                )
            }
        }
    }
}