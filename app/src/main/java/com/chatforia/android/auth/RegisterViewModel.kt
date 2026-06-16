package com.chatforia.android.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatforia.android.crypto.KeyStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import analytics.AnalyticsManager
import analytics.AnalyticsTracker
data class RegisterUiState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val phone: String = "",
    val smsConsent: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class RegisterViewModel(
    private val authRepository: AuthRepository,
    private val tokenStorage: TokenStorage,
    private val keyStorage: KeyStorage,
    private val onRegistered: () -> Unit,
    private val analytics: AnalyticsTracker = AnalyticsManager
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state

    fun updateUsername(value: String) {
        _state.value = _state.value.copy(username = value, errorMessage = null)
    }

    fun updateEmail(value: String) {
        _state.value = _state.value.copy(email = value, errorMessage = null)
    }

    fun updatePassword(value: String) {
        _state.value = _state.value.copy(password = value, errorMessage = null)
    }

    fun updateConfirmPassword(value: String) {
        _state.value = _state.value.copy(confirmPassword = value, errorMessage = null)
    }

    fun updatePhone(value: String) {
        _state.value = _state.value.copy(phone = value, errorMessage = null)
    }

    fun updateSmsConsent(value: Boolean) {
        _state.value = _state.value.copy(smsConsent = value, errorMessage = null)
    }

    fun submit() {
        val current = _state.value

        val username = current.username.trim()
        val email = current.email.trim()
        val phone = current.phone.trim()

        if (username.isBlank()) {
            setError("Username is required.")
            return
        }

        if (!isValidEmail(email)) {
            setError("Enter a valid email address.")
            return
        }

        if (current.password.isBlank()) {
            setError("Password is required.")
            return
        }

        if (current.password.length < 6) {
            setError("Password must be at least 6 characters.")
            return
        }

        if (current.password != current.confirmPassword) {
            setError("Passwords do not match.")
            return
        }

        if (phone.isNotBlank() && !current.smsConsent) {
            setError("SMS consent is required when adding a phone number.")
            return
        }

        viewModelScope.launch {
            _state.value =
                _state.value.copy(
                    isSubmitting = true,
                    errorMessage = null,
                    successMessage = null
                )

            try {
                val response =
                    authRepository.register(
                        username = username,
                        email = email,
                        password = current.password,
                        phone = phone.takeIf { it.isNotBlank() },
                        smsConsent =
                            if (phone.isBlank()) null else current.smsConsent
                    )

                val token = response.token
                val resolvedUser = response.resolvedUser

                if (!token.isNullOrBlank() && resolvedUser != null) {
                    val privateKey = response.privateKey
                    val publicKey = resolvedUser.publicKey

                    if (
                        !privateKey.isNullOrBlank() &&
                        !publicKey.isNullOrBlank()
                    ) {
                        keyStorage.saveKeyPair(
                            publicKey = publicKey,
                            privateKey = privateKey
                        )
                    }

                    analytics.identify(
                        userId = resolvedUser.id,
                        properties = mapOf(
                            "username" to (resolvedUser.username ?: ""),
                            "preferred_language" to (resolvedUser.preferredLanguage ?: "")
                        )
                    )

                    analytics.capture(
                        "account signed up",
                        mapOf(
                            "has_phone" to phone.isNotBlank(),
                            "sms_consent" to current.smsConsent
                        )
                    )

                    tokenStorage.save(token)
                    onRegistered()
                    return@launch
                }

                _state.value =
                    _state.value.copy(
                        isSubmitting = false,
                        successMessage =
                            response.message
                                ?: "Check your email to verify your account."
                    )

            } catch (error: Exception) {
                _state.value =
                    _state.value.copy(
                        isSubmitting = false,
                        errorMessage =
                            error.message ?: "Failed to create account."
                    )
            }
        }
    }

    private fun setError(message: String) {
        _state.value =
            _state.value.copy(
                errorMessage = message,
                successMessage = null
            )
    }

    private fun isValidEmail(email: String): Boolean {
        return Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email)
    }
}