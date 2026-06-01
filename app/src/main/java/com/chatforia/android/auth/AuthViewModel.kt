package com.chatforia.android.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)

    val state: StateFlow<AuthState> = _state

    init {
        bootstrap()
    }

    fun bootstrap() {
        _state.value = AuthState.Loading

        viewModelScope.launch {
            val user = repository.bootstrap()

            _state.value =
                when {
                    user == null -> AuthState.LoggedOut
                    needsOnboarding(user) -> AuthState.NeedsOnboarding(user)
                    else -> AuthState.LoggedIn(user)
                }
        }
    }

    suspend fun login(
        identifier: String,
        password: String
    ) {
        val user = repository.login(identifier, password)

        _state.value =
            if (needsOnboarding(user)) {
                AuthState.NeedsOnboarding(user)
            } else {
                AuthState.LoggedIn(user)
            }
    }

    suspend fun loginWithGoogle(
        idToken: String
    ) {
        val user =
            repository.loginWithGoogle(idToken)

        _state.value =
            if (needsOnboarding(user)) {
                AuthState.NeedsOnboarding(user)
            } else {
                AuthState.LoggedIn(user)
            }
    }

    fun replaceCurrentUser(
        user: UserDto
    ) {
        _state.value =
            if (needsOnboarding(user)) {
                AuthState.NeedsOnboarding(user)
            } else {
                AuthState.LoggedIn(user)
            }
    }

    fun markOnboardingComplete(
        user: UserDto
    ) {
        _state.value = AuthState.LoggedIn(user)
    }

    fun logout() {
        repository.logout()
        _state.value = AuthState.LoggedOut
    }

    private fun needsOnboarding(user: UserDto): Boolean {
        val onboardingIncomplete =
            user.onboardingCompletedAt
                ?.trim()
                .isNullOrEmpty()

        val languageMissing =
            user.preferredLanguage
                ?.trim()
                .isNullOrEmpty()

        val username =
            user.username
                ?.lowercase()
                ?.trim()
                .orEmpty()

        val hasTemporaryUsername =
            username.startsWith("user_") ||
                    username.startsWith("pending_")

        return onboardingIncomplete ||
                languageMissing ||
                hasTemporaryUsername
    }
}