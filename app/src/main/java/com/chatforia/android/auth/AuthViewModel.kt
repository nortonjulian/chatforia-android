package com.chatforia.android.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.chatforia.android.crypto.AccountKeyManager
import android.content.Context

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

    private suspend fun resolveLoggedInState(user: UserDto): AuthState {
        return try {
            val preparedUser = prepareEncryptionKeys(user)

            if (needsOnboarding(preparedUser)) {
                AuthState.NeedsOnboarding(preparedUser)
            } else {
                AuthState.LoggedIn(preparedUser)
            }
        } catch (e: Exception) {
            AuthState.NeedsKeyRestore(
                user = user,
                message = e.message ?: "This device needs your encryption key."
            )
        }
    }

    fun bootstrap() {
        _state.value = AuthState.Loading

        viewModelScope.launch {
            val user = repository.bootstrap()

            if (user == null) {
                _state.value = AuthState.LoggedOut
                return@launch
            }

            _state.value = resolveLoggedInState(user)
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
        _state.value = resolveLoggedInState(user)
    }

    suspend fun loginWithGoogle(
        idToken: String
    ) {
        val user = repository.loginWithGoogle(idToken)
        _state.value = resolveLoggedInState(user)
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

    fun loginWithExternalToken(token: String) {
        repository.saveExternalToken(token)
        bootstrap()
    }

    fun loginWithApple(context: Context) {
        AppleAuthClient(context).start()
    }

    fun showRegistration() {
        _state.value = AuthState.Registering
    }

    fun showLogin() {
        _state.value = AuthState.LoggedOut
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