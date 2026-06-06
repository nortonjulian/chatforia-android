package com.chatforia.android.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val preferredLanguage: String = "en",
    val autoTranslate: Boolean = false,
    val showOriginalWithTranslation: Boolean = false,
    val theme: String = "dawn",
    val allowExplicitContent: Boolean = false,
    val showReadReceipts: Boolean = false,
    val autoDeleteSeconds: Int = 0,

    val privacyBlurEnabled: Boolean = false,
    val privacyBlurOnUnfocus: Boolean = false,
    val privacyHoldToReveal: Boolean = false,
    val notifyOnCopy: Boolean = false,

    val foriaRemember: Boolean = true,
    val enableSmartReplies: Boolean = true,
    val maskAIProfanity: Boolean = false,

    val messageTone: String = "Default.mp3",
    val ringtone: String = "Classic.mp3",
    val soundVolume: Int = 70,

    val isSaving: Boolean = false,
    val error: String? = null,
    val success: String? = null
)

class SettingsViewModel(
    private val repository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state

    fun load(user: UserDto) {
        _state.value = SettingsUiState(
            preferredLanguage = user.preferredLanguage ?: "en",
            autoTranslate = false,
            showOriginalWithTranslation = false,
            theme = user.theme ?: "dawn",
            allowExplicitContent = user.allowExplicitContent ?: false,
            showReadReceipts =
                user.showReadReceipts ?: false,
            autoDeleteSeconds = user.autoDeleteSeconds ?: 0,
            privacyBlurEnabled = user.privacyBlurEnabled ?: false,
            privacyBlurOnUnfocus = user.privacyBlurOnUnfocus ?: false,
            privacyHoldToReveal = user.privacyHoldToReveal ?: false,
            notifyOnCopy = user.notifyOnCopy ?: false,
            foriaRemember = user.foriaRemember ?: true,
            enableSmartReplies = user.smartRepliesEnabled ?: true,
            maskAIProfanity = user.profanityMaskEnabled ?: false,
            messageTone = user.messageSound ?: user.tone ?: "Default.mp3",
            ringtone = user.ringtone ?: "Classic.mp3",
            soundVolume = user.soundVolume?.toInt() ?: 70
        )
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
                        foriaRemember = current.foriaRemember,
                        enableSmartReplies = current.enableSmartReplies,
                        maskAIProfanity = current.maskAIProfanity,
                        messageTone = current.messageTone,
                        ringtone = current.ringtone,
                        soundVolume = current.soundVolume
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