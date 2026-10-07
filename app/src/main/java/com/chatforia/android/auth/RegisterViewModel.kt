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
    val registrationCompleted: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class RegisterViewModel(
    private val authRepository: AuthRepository,
    private val tokenStorage: TokenStorage,
    private val keyStorage: KeyStorage,
    private val onRegistered: () -> Unit,
    private val analytics: AnalyticsTracker = AnalyticsManager,
    private val referralStore: CreatorReferralStore? = null
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

    fun submit() {
        val current = _state.value
        if (current.isSubmitting || current.registrationCompleted) return

        val username = current.username.trim()
        val email = current.email.trim()

        if (!Regex("^[a-zA-Z0-9_]{3,20}$").matches(username)) {
            setError("Username must be 3–20 letters, numbers, or underscores.")
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

        if (current.password.length < 8) {
            setError("Password must be at least 8 characters.")
            return
        }

        if (current.password != current.confirmPassword) {
            setError("Passwords do not match.")
            return
        }

        _state.value = _state.value.copy(
            isSubmitting = true,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            try {
                val referralCode = referralStore?.currentCode()
                val response =
                    authRepository.register(
                        username = username,
                        email = email,
                        password = current.password,
                        referralCode = referralCode
                    )
                referralStore?.clear()

                val token = response.token
                val resolvedUser = response.resolvedUser

                if (resolvedUser != null) {
                    val privateKey = response.privateKey
                    val publicKey = resolvedUser.publicKey

                    if (
                        !privateKey.isNullOrBlank() &&
                        !publicKey.isNullOrBlank()
                    ) {
                        keyStorage.saveKeyPair(
                            userId = resolvedUser.id,
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

                    val properties = mutableMapOf<String, Any>(
                        "method" to "email",
                        "plan" to "FREE"
                    )
                    referralCode?.let { properties["referral_code"] = it }
                    analytics.capture(
                        "user_registered",
                        properties
                    )

                    if (!response.requiresEmailVerification && !token.isNullOrBlank()) {
                        _state.value = _state.value.copy(registrationCompleted = true, password = "", confirmPassword = "")
                        tokenStorage.save(token)
                        onRegistered()
                        return@launch
                    }
                }

                _state.value =
                    _state.value.copy(
                        isSubmitting = false,
                        registrationCompleted = true,
                        password = "",
                        confirmPassword = "",
                        successMessage = "Check your email to verify your account, then log in."
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