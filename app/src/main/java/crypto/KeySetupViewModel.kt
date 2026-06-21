package com.chatforia.android.crypto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.chatforia.android.auth.AuthRepository

class KeySetupViewModel(
    private val remoteKeyBackupRepository: RemoteKeyBackupRepository,
    private val keyStorage: KeyStorage,
    private val authRepository: AuthRepository,
    private val accountKeyManager: AccountKeyManager,
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
                val recoveryPassword = password.trim()

                if (recoveryPassword.length < 8) {
                    throw Exception("Recovery passcode must be at least 8 characters.")
                }

                val backup =
                    remoteKeyBackupRepository.fetchBackup()
                        ?: throw Exception("No remote key backup found.")

                val restored =
                    keyBackupCrypto.decryptRemoteBackup(
                        backup = backup,
                        password = recoveryPassword
                    )

                val serverUser = authRepository.fetchMe()
                val serverPublicKey = serverUser.publicKey?.trim().orEmpty()
                val restoredPublicKey = restored.publicKey.trim()

                if (serverPublicKey.isBlank()) {
                    throw Exception("This account does not have a server encryption key.")
                }

                if (restoredPublicKey != serverPublicKey) {
                    throw Exception("The restored encryption key does not match this account.")
                }

                keyStorage.saveKeyPair(
                    publicKey = restored.publicKey,
                    privateKey = restored.privateKey
                )

                _state.value =
                    _state.value.copy(
                        isRestoring = false,
                        hasLocalPrivateKey = true,
                        hasRemoteBackup = true,
                        successMessage = "Encrypted chats restored."
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

    fun resetEncryption() {
        viewModelScope.launch {

            _state.value =
                _state.value.copy(
                    error = null,
                    successMessage = null
                )

            try {

                accountKeyManager.resetAccountEncryption { publicKey ->
                    authRepository.rotateEncryptionKey(publicKey)
                }

                _state.value =
                    _state.value.copy(
                        hasLocalPrivateKey = true,
                        hasRemoteBackup = false,
                        successMessage =
                            "Encryption reset. Create a new Recovery Backup."
                    )

                refreshBackupStatus()

            } catch (e: Exception) {

                _state.value =
                    _state.value.copy(
                        error =
                            e.message
                                ?: "Failed to reset encryption."
                    )
            }
        }
    }

    fun createRemoteBackup(password: String) {
        viewModelScope.launch {
            _state.value =
                _state.value.copy(
                    isCreatingBackup = true,
                    error = null,
                    successMessage = null
                )

            try {
                val recoveryPassword = password.trim()

                if (recoveryPassword.length < 8) {
                    throw Exception("Recovery passcode must be at least 8 characters.")
                }

                val publicKey =
                    keyStorage.readPublicKey()
                        ?: throw Exception("No local public key found.")

                val privateKey =
                    keyStorage.readPrivateKey()
                        ?: throw Exception("No local private key found.")

                val payload =
                    keyBackupCrypto.createRemoteBackup(
                        publicKey = publicKey,
                        privateKey = privateKey,
                        password = recoveryPassword
                    )

                remoteKeyBackupRepository.uploadBackup(payload)

                _state.value =
                    _state.value.copy(
                        isCreatingBackup = false,
                        hasLocalPrivateKey = true,
                        hasRemoteBackup = true,
                        successMessage = "Recovery Backup created."
                    )

            } catch (e: Exception) {
                _state.value =
                    _state.value.copy(
                        isCreatingBackup = false,
                        hasLocalPrivateKey = keyStorage.hasPrivateKey(),
                        error = e.message ?: "Failed to create Recovery Backup."
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

        refreshBackupStatus()
    }
}

data class KeySetupState(
    val hasLocalPrivateKey: Boolean = false,
    val hasRemoteBackup: Boolean = false,
    val isCheckingBackup: Boolean = false,
    val isRestoring: Boolean = false,
    val isCreatingBackup: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)