package com.chatforia.android.crypto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatforia.android.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class KeySetupViewModel(
    private val remoteKeyBackupRepository: RemoteKeyBackupRepository,
    private val keyStorage: KeyStorage,
    private val authRepository: AuthRepository,
    private val accountKeyManager: AccountKeyManager,
    private val userId: Int,
    private val keyBackupCrypto: KeyBackupCrypto = KeyBackupCrypto()
) : ViewModel() {

    private val _state = MutableStateFlow(KeySetupState())
    val state: StateFlow<KeySetupState> = _state

    private data class LocalKeyStatus(
        val hasPrivateKey: Boolean,
        val hasCompleteKeyPair: Boolean,
        val matchesAccountKey: Boolean
    )

    private fun inspectLocalKey(
        serverPublicKey: String?
    ): LocalKeyStatus {
        val localPublicKey =
            keyStorage.readPublicKey()
                ?.trim()
                .orEmpty()

        val localPrivateKey =
            keyStorage.readPrivateKey()
                ?.trim()
                .orEmpty()

        val normalizedServerKey =
            serverPublicKey
                ?.trim()
                .orEmpty()

        val hasPrivateKey =
            localPrivateKey.isNotEmpty()

        val hasCompletePair =
            localPublicKey.isNotEmpty() &&
                localPrivateKey.isNotEmpty()

        val matchesAccount =
            hasCompletePair &&
                normalizedServerKey.isNotEmpty() &&
                localPublicKey == normalizedServerKey

        return LocalKeyStatus(
            hasPrivateKey = hasPrivateKey,
            hasCompleteKeyPair = hasCompletePair,
            matchesAccountKey = matchesAccount
        )
    }

    fun refreshBackupStatus() {
        viewModelScope.launch {
            _state.value =
                _state.value.copy(
                    isCheckingBackup = true,
                    error = null
                )

            try {
                val serverUser =
                    authRepository.fetchMe()

                val localStatus =
                    inspectLocalKey(
                        serverUser.publicKey
                    )

                try {
                    val backup =
                        remoteKeyBackupRepository
                            .fetchBackup()

                    _state.value =
                        _state.value.copy(
                            isCheckingBackup = false,
                            hasLocalPrivateKey =
                                localStatus.hasPrivateKey,
                            hasCompleteLocalKey =
                                localStatus.hasCompleteKeyPair,
                            hasMatchingLocalKey =
                                localStatus.matchesAccountKey,
                            hasRemoteBackup =
                                backup
                                    ?.encryptedPrivateKeyBundle !=
                                    null,
                            isBackupStatusKnown = true,
                            error = null
                        )

                } catch (error: Exception) {
                    _state.value =
                        _state.value.copy(
                            isCheckingBackup = false,
                            hasLocalPrivateKey =
                                localStatus.hasPrivateKey,
                            hasCompleteLocalKey =
                                localStatus.hasCompleteKeyPair,
                            hasMatchingLocalKey =
                                localStatus.matchesAccountKey,
                            hasRemoteBackup = false,
                            isBackupStatusKnown = false,
                            error =
                                friendlyRecoveryMessage(
                                    error,
                                    "Failed to check secure message recovery."
                                )
                        )
                }

            } catch (error: Exception) {
                _state.value =
                    _state.value.copy(
                        isCheckingBackup = false,
                        hasMatchingLocalKey = false,
                        isBackupStatusKnown = false,
                        error =
                            friendlyRecoveryMessage(
                                error,
                                "Failed to verify this device’s secure message key."
                            )
                    )
            }
        }
    }

    fun restoreFromRemoteBackup(
        password: String
    ) {
        viewModelScope.launch {
            _state.value =
                _state.value.copy(
                    isRestoring = true,
                    error = null,
                    successMessage = null
                )

            try {
                val recoveryPassword =
                    password.trim()

                if (recoveryPassword.length < 8) {
                    throw IllegalArgumentException(
                        "Secure Messages Passcode must be at least 8 characters."
                    )
                }

                val backup =
                    remoteKeyBackupRepository
                        .fetchBackup()
                        ?: throw IllegalStateException(
                            "No Secure Messages recovery backup was found for this account."
                        )

                val restored =
                    keyBackupCrypto
                        .decryptRemoteBackup(
                            backup = backup,
                            password = recoveryPassword
                        )

                val serverUser =
                    authRepository.fetchMe()

                val serverPublicKey =
                    serverUser.publicKey
                        ?.trim()
                        .orEmpty()

                val restoredPublicKey =
                    restored.publicKey.trim()

                if (serverPublicKey.isBlank()) {
                    throw IllegalStateException(
                        "This account does not have a secure message key."
                    )
                }

                if (
                    restoredPublicKey.isBlank() ||
                    restored.privateKey.isBlank() ||
                    restoredPublicKey != serverPublicKey
                ) {
                    throw IllegalStateException(
                        "The restored secure message key does not match this account."
                    )
                }

                keyStorage.saveKeyPair(
                    publicKey = restored.publicKey,
                    privateKey = restored.privateKey
                )

                val verifiedStatus =
                    inspectLocalKey(
                        serverUser.publicKey
                    )

                if (!verifiedStatus.matchesAccountKey) {
                    throw IllegalStateException(
                        "The restored secure message key could not be verified on this device."
                    )
                }

                _state.value =
                    _state.value.copy(
                        isRestoring = false,
                        hasLocalPrivateKey = true,
                        hasCompleteLocalKey = true,
                        hasMatchingLocalKey = true,
                        hasRemoteBackup = true,
                        isBackupStatusKnown = true,
                        successMessage =
                            "Secure messages restored.",
                        error = null
                    )

            } catch (error: Exception) {
                _state.value =
                    _state.value.copy(
                        isRestoring = false,
                        error =
                            friendlyRecoveryMessage(
                                error,
                                "Failed to restore secure messages."
                            )
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
                accountKeyManager
                    .resetAccountEncryption(
                        userId = userId
                    ) { publicKey ->
                        authRepository
                            .rotateEncryptionKey(
                                publicKey
                            )
                    }

                val serverUser =
                    authRepository.fetchMe()

                val verifiedStatus =
                    inspectLocalKey(
                        serverUser.publicKey
                    )

                if (!verifiedStatus.matchesAccountKey) {
                    throw IllegalStateException(
                        "The new secure message key could not be verified."
                    )
                }

                _state.value =
                    _state.value.copy(
                        hasLocalPrivateKey = true,
                        hasCompleteLocalKey = true,
                        hasMatchingLocalKey = true,
                        hasRemoteBackup = false,
                        isBackupStatusKnown = true,
                        successMessage =
                            "Secure messages were reset. Create a new recovery backup.",
                        error = null
                    )

            } catch (error: Exception) {
                _state.value =
                    _state.value.copy(
                        error =
                            friendlyRecoveryMessage(
                                error,
                                "Failed to start fresh with secure messages."
                            )
                    )
            }
        }
    }

    fun createRemoteBackup(
        password: String
    ) {
        viewModelScope.launch {
            _state.value =
                _state.value.copy(
                    isCreatingBackup = true,
                    error = null,
                    successMessage = null
                )

            try {
                val recoveryPassword =
                    password.trim()

                if (recoveryPassword.length < 8) {
                    throw IllegalArgumentException(
                        "Secure Messages Passcode must be at least 8 characters."
                    )
                }

                val serverUser =
                    authRepository.fetchMe()

                val localStatus =
                    inspectLocalKey(
                        serverUser.publicKey
                    )

                if (!localStatus.matchesAccountKey) {
                    throw IllegalStateException(
                        "This device’s secure message key does not match the account key. Restore secure messages before creating or updating the recovery backup."
                    )
                }

                val publicKey =
                    keyStorage.readPublicKey()
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                        ?: throw IllegalStateException(
                            "No local public key was found."
                        )

                val privateKey =
                    keyStorage.readPrivateKey()
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                        ?: throw IllegalStateException(
                            "No local private key was found."
                        )

                val wasUpdatingBackup =
                    _state.value.hasRemoteBackup

                val payload =
                    keyBackupCrypto
                        .createRemoteBackup(
                            publicKey = publicKey,
                            privateKey = privateKey,
                            password = recoveryPassword
                        )

                remoteKeyBackupRepository
                    .uploadBackup(payload)

                val verification =
                    remoteKeyBackupRepository
                        .fetchBackup()

                if (
                    verification
                        ?.encryptedPrivateKeyBundle
                        .isNullOrBlank()
                ) {
                    throw IllegalStateException(
                        "The recovery backup could not be verified."
                    )
                }

                _state.value =
                    _state.value.copy(
                        isCreatingBackup = false,
                        hasLocalPrivateKey = true,
                        hasCompleteLocalKey = true,
                        hasMatchingLocalKey = true,
                        hasRemoteBackup = true,
                        isBackupStatusKnown = true,
                        successMessage =
                            if (wasUpdatingBackup) {
                                "Recovery backup updated."
                            } else {
                                "Recovery backup created."
                            },
                        error = null
                    )

            } catch (error: Exception) {
                _state.value =
                    _state.value.copy(
                        isCreatingBackup = false,
                        error =
                            friendlyRecoveryMessage(
                                error,
                                "Failed to save the recovery backup."
                            )
                    )
            }
        }
    }

    /*
     * Retained for internal diagnostics and tests. The production recovery
     * screen no longer exposes a control that deletes a working local key.
     */
    fun clearLocalKeys() {
        keyStorage.clearKeys()

        _state.value =
            _state.value.copy(
                hasLocalPrivateKey = false,
                hasCompleteLocalKey = false,
                hasMatchingLocalKey = false,
                successMessage = null,
                error = null
            )

        refreshBackupStatus()
    }

    private fun friendlyRecoveryMessage(
        error: Throwable,
        fallback: String
    ): String {
        val rawMessage =
            error.message
                ?.trim()
                .orEmpty()

        if (
            rawMessage.startsWith(
                "Secure Messages Passcode"
            ) ||
            rawMessage.startsWith(
                "This device"
            ) ||
            rawMessage.startsWith(
                "This account"
            ) ||
            rawMessage.startsWith(
                "The restored"
            ) ||
            rawMessage.startsWith(
                "The new secure"
            ) ||
            rawMessage.startsWith(
                "No Secure Messages"
            ) ||
            rawMessage.startsWith(
                "The recovery backup"
            )
        ) {
            return rawMessage
        }

        val normalized =
            rawMessage.uppercase()

        return when {
            "BACKUP_KEY_MISMATCH" in normalized ->
                "This device’s secure message key does not match the account key. Restore secure messages from the existing recovery backup."

            "ENCRYPTION_KEY_FROZEN" in normalized ||
                "KEY_CHANGE_FROZEN" in normalized ||
                "HTTP 423" in normalized ->
                "Secure message key changes are temporarily unavailable. Please try again later."

            "UNAUTHORIZED" in normalized ||
                "HTTP 401" in normalized ->
                "Your session expired. Please sign in again."

            "NO BACKUP" in normalized ||
                "BACKUP_NOT_FOUND" in normalized ->
                "No Secure Messages recovery backup was found for this account."

            "DECRYPT" in normalized ||
                "AEAD" in normalized ||
                "BAD TAG" in normalized ||
                "WRONG PASSWORD" in normalized ->
                "That Secure Messages Passcode is incorrect, or the recovery backup could not be opened."

            "TIMEOUT" in normalized ||
                "UNABLE TO RESOLVE HOST" in normalized ||
                "NETWORK" in normalized ||
                "CONNECTION" in normalized ->
                "Check your internet connection and try again."

            else -> fallback
        }
    }
}

data class KeySetupState(
    /*
     * Kept for source compatibility with existing tests. UI decisions must
     * use hasMatchingLocalKey rather than private-key existence.
     */
    val hasLocalPrivateKey: Boolean = false,
    val hasCompleteLocalKey: Boolean = false,
    val hasMatchingLocalKey: Boolean = false,
    val hasRemoteBackup: Boolean = false,
    val isBackupStatusKnown: Boolean = false,
    val isCheckingBackup: Boolean = false,
    val isRestoring: Boolean = false,
    val isCreatingBackup: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
