package com.chatforia.android.forwarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ForwardingSettingsState(
    val forwardingEnabledSms: Boolean = false,
    val forwardSmsToPhone: Boolean = false,
    val forwardPhoneNumber: String = "",
    val forwardSmsToEmail: Boolean = false,
    val forwardEmail: String = "",
    val forwardingEnabledCalls: Boolean = false,
    val forwardToPhoneE164: String = "",
    val forwardQuietHoursStart: Int? = null,
    val forwardQuietHoursEnd: Int? = null,

    val initial: ForwardingSettingsDto = ForwardingSettingsDto(),

    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val banner: String? = null
) {
    val currentDto: ForwardingSettingsDto
        get() = ForwardingSettingsDto(
            forwardingEnabledSms = forwardingEnabledSms,
            forwardSmsToPhone = forwardSmsToPhone,
            forwardPhoneNumber = forwardPhoneNumber,
            forwardSmsToEmail = forwardSmsToEmail,
            forwardEmail = forwardEmail,
            forwardingEnabledCalls = forwardingEnabledCalls,
            forwardToPhoneE164 = forwardToPhoneE164,
            forwardQuietHoursStart = forwardQuietHoursStart,
            forwardQuietHoursEnd = forwardQuietHoursEnd
        )

    val hasChanges: Boolean
        get() = currentDto != initial

    val validationErrors: Map<String, String>
        get() {
            val errors = mutableMapOf<String, String>()

            if (forwardingEnabledSms) {
                if (!forwardSmsToPhone && !forwardSmsToEmail) {
                    errors["smsToggle"] = "Choose at least one destination."
                }

                if (forwardSmsToPhone && !isValidE164(forwardPhoneNumber)) {
                    errors["forwardPhoneNumber"] = "Enter a valid E.164 phone number."
                }

                if (forwardSmsToEmail && !isValidEmail(forwardEmail)) {
                    errors["forwardEmail"] = "Enter a valid email."
                }
            }

            if (forwardingEnabledCalls && !isValidE164(forwardToPhoneE164)) {
                errors["forwardToPhoneE164"] = "Enter a valid E.164 phone number."
            }

            val quietStartInvalid =
                forwardQuietHoursStart != null &&
                        (forwardQuietHoursStart < 0 || forwardQuietHoursStart > 23)

            val quietEndInvalid =
                forwardQuietHoursEnd != null &&
                        (forwardQuietHoursEnd < 0 || forwardQuietHoursEnd > 23)

            if (quietStartInvalid || quietEndInvalid) {
                errors["quiet"] = "Quiet hours must be between 0 and 23."
            }

            return errors
        }

    val canSave: Boolean
        get() = hasChanges && validationErrors.isEmpty() && !isSaving
}

class ForwardingSettingsViewModel(
    private val repository: ForwardingSettingsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ForwardingSettingsState())
    val state: StateFlow<ForwardingSettingsState> = _state

    fun load() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    banner = null
                )
            }

            try {
                val dto = repository.fetchSettings()
                loadFromDto(dto)
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        errorMessage = e.message ?: "Failed to load forwarding settings."
                    )
                }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun save() {
        viewModelScope.launch {
            val current = _state.value

            if (!current.canSave) return@launch

            _state.update {
                it.copy(
                    isSaving = true,
                    errorMessage = null,
                    banner = null
                )
            }

            try {
                val saved = repository.saveSettings(
                    current.currentDto.normalized()
                )

                loadFromDto(saved)

                _state.update {
                    it.copy(
                        banner = "Forwarding settings saved."
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        errorMessage = e.message ?: "Failed to save forwarding settings."
                    )
                }
            } finally {
                _state.update { it.copy(isSaving = false) }
            }
        }
    }

    fun reset() {
        loadFromDto(_state.value.initial)
    }

    fun update(
        transform: (ForwardingSettingsState) -> ForwardingSettingsState
    ) {
        _state.update {
            transform(it).copy(
                banner = null,
                errorMessage = null
            )
        }
    }

    private fun loadFromDto(dto: ForwardingSettingsDto) {
        _state.update {
            it.copy(
                forwardingEnabledSms = dto.forwardingEnabledSms,
                forwardSmsToPhone = dto.forwardSmsToPhone,
                forwardPhoneNumber = dto.forwardPhoneNumber,
                forwardSmsToEmail = dto.forwardSmsToEmail,
                forwardEmail = dto.forwardEmail,
                forwardingEnabledCalls = dto.forwardingEnabledCalls,
                forwardToPhoneE164 = dto.forwardToPhoneE164,
                forwardQuietHoursStart = dto.forwardQuietHoursStart,
                forwardQuietHoursEnd = dto.forwardQuietHoursEnd,
                initial = dto,
                errorMessage = null
            )
        }
    }
}

private fun ForwardingSettingsDto.normalized(): ForwardingSettingsDto {
    return copy(
        forwardPhoneNumber = forwardPhoneNumber.normalizePhone(),
        forwardToPhoneE164 = forwardToPhoneE164.normalizePhone(),
        forwardEmail = forwardEmail.trim()
    )
}

private fun String.normalizePhone(): String {
    return replace(Regex("[^\\d+]"), "")
}

private fun isValidE164(value: String): Boolean {
    return Regex("^\\+?[1-9]\\d{7,14}$").matches(value.normalizePhone())
}

private fun isValidEmail(value: String): Boolean {
    return Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(value.trim())
}