package com.chatforia.android.crypto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class KeySetupViewModel(
    private val remoteKeyBackupRepository: RemoteKeyBackupRepository,
    private val keyStorage: KeyStorage,
    private val keyBackupCrypto: KeyBackupCrypto = KeyBackupCrypto()
) : ViewModel() {

    private val _state = MutableStateFlow(KeySetupState())
    val state: StateFlow<KeySetupState> = _state

    fun refreshBackupStatus() {
        viewModelScope.launch {
            _state.value =
                _state.value.copy(
                    isCheckingBackup = true,
                    error = null
                )

            try {
                val backup = remoteKeyBackupRepository.fetchBackup()

                _state.value =
                    _state.value.copy(
                        isCheckingBackup = false,
                        hasLocalPrivateKey = keyStorage.hasPrivateKey(),
                        hasRemoteBackup = backup?.encryptedPrivateKeyBundle != null
                    )

            } catch (e: Exception) {
                _state.value =
                    _state.value.copy(
                        isCheckingBackup = false,
                        hasLocalPrivateKey = keyStorage.hasPrivateKey(),
                        hasRemoteBackup = false,
                        error = e.message ?: "Failed to check key backup."
                    )
            }
        }
    }

    fun restoreFromRemoteBackup(password: String) {
        viewModelScope.launch {
            _state.value =
                _state.value.copy(
                    isRestoring = true,
                    error = null,
                    successMessage = null
                )

            try {
                val backup =
                    remoteKeyBackupRepository.fetchBackup()
                        ?: throw Exception("No remote key backup found.")

                val restored =
                    keyBackupCrypto.decryptRemoteBackup(
                        backup = backup,
                        password = password
                    )

                keyStorage.saveKeyPair(
                    publicKey = restored.publicKey,
                    privateKey = restored.privateKey
                )

                _state.value =
                    _state.value.copy(
                        isRestoring = false,
                        hasLocalPrivateKey = true,
                        hasRemoteBackup = true,
                        successMessage = "Encryption key restored."
                    )

            } catch (e: Exception) {
                _state.value =
                    _state.value.copy(
                        isRestoring = false,
                        hasLocalPrivateKey = keyStorage.hasPrivateKey(),
                        error = e.message ?: "Failed to restore encryption key."
                    )
            }
        }
    }

    fun clearLocalKeys() {
        keyStorage.clearKeys()

        _state.value =
            _state.value.copy(
                hasLocalPrivateKey = false,
                successMessage = "Local encryption keys removed.",
                error = null
            )
    }
}

data class KeySetupState(
    val hasLocalPrivateKey: Boolean = false,
    val hasRemoteBackup: Boolean = false,
    val isCheckingBackup: Boolean = false,
    val isRestoring: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)