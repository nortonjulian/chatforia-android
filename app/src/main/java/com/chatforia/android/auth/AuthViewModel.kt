package com.chatforia.android.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.chatforia.android.crypto.AccountKeyManager

class AuthViewModel(
    private val repository: AuthRepository,
    private val accountKeyManager: AccountKeyManager
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)

    val state: StateFlow<AuthState> = _state

    init {
        bootstrap()
    }

    private suspend fun prepareEncryptionKeys(user: UserDto): UserDto {
        accountKeyManager.ensureLocalKeysExist(
            serverPublicKey = user.publicKey
        ) { publicKey ->
            repository.rotateEncryptionKey(publicKey)
        }

        return repository.fetchMe()
    }

    fun bootstrap() {
        _state.value = AuthState.Loading

        viewModelScope.launch {
            val user = repository.bootstrap()

            if (user == null) {
                _state.value = AuthState.LoggedOut
                return@launch
            }

            val preparedUser =
                try {
                    prepareEncryptionKeys(user)
                } catch (e: Exception) {
                    user
                }

            _state.value =
                when {
                    needsOnboarding(preparedUser) -> AuthState.NeedsOnboarding(preparedUser)
                    else -> AuthState.LoggedIn(preparedUser)
                }
        }
    }

    suspend fun resetEncryptionAndLogin(
        identifier: String,
        password: String
    ) {
        val user = repository.login(identifier, password)

        accountKeyManager.resetAccountEncryption { publicKey ->
            repository.rotateEncryptionKey(publicKey)
        }

        val refreshedUser = repository.fetchMe()

        _state.value =
            if (needsOnboarding(refreshedUser)) {
                AuthState.NeedsOnboarding(refreshedUser)
            } else {
                AuthState.LoggedIn(refreshedUser)
            }
    }

    suspend fun login(
        identifier: String,
        password: String
    ) {
        val user = repository.login(identifier, password)
        val preparedUser = prepareEncryptionKeys(user)

        _state.value =
            if (needsOnboarding(preparedUser)) {
                AuthState.NeedsOnboarding(preparedUser)
            } else {
                AuthState.LoggedIn(preparedUser)
            }
    }

    suspend fun loginWithGoogle(
        idToken: String
    ) {
        val user = repository.loginWithGoogle(idToken)
        val preparedUser = prepareEncryptionKeys(user)

        _state.value =
            if (needsOnboarding(preparedUser)) {
                AuthState.NeedsOnboarding(preparedUser)
            } else {
                AuthState.LoggedIn(preparedUser)
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

    fun setError(message: String) {
        println(message)
        _state.value = AuthState.LoggedOut
    }

    fun logout() {
        repository.logout()
        _state.value = AuthState.LoggedOut
    }

    private fun needsOnboarding(user: UserDto): Boolean {
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

        return languageMissing || hasTemporaryUsername
    }
}