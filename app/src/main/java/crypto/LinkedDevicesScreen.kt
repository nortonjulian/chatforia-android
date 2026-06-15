package com.chatforia.android.crypto

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.chatforia.android.ChatforiaGradientButton
import androidx.compose.ui.res.stringResource
import com.chatforia.android.R

data class LinkedDevicesUiState(
    val devices: List<LinkedDeviceDto> = emptyList(),
    val pending: List<LinkedDeviceDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class LinkedDevicesViewModel(
    private val repository: LinkedDevicesRepository,
    private val keyStorage: KeyStorage,
    private val deviceIdentityStorage: DeviceIdentityStorage,
    private val provisioningCrypto: DeviceProvisioningCrypto = DeviceProvisioningCrypto()
) : ViewModel() {

    private val _state = MutableStateFlow(LinkedDevicesUiState())
    val state: StateFlow<LinkedDevicesUiState> = _state

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                val allDevices = repository.fetchMine()
                val pendingDevices = repository.fetchPendingPairing()

                _state.value = _state.value.copy(
                    devices = allDevices.filter {
                        it.pairingStatus == null || it.pairingStatus == "approved"
                    },
                    pending = pendingDevices,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load devices"
                )
            }
        }
    }

    fun approve(device: LinkedDeviceDto) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val localPrivateKey = keyStorage.readPrivateKey()
                    ?: throw IllegalStateException("This device is missing your encryption key.")

                val targetPublicKey = device.publicKey
                    ?: throw IllegalStateException("Pending device is missing a public key.")

                val wrappedAccountKey =
                    provisioningCrypto.wrapAccountKeyForDevice(
                        accountPrivateKeyB64 = localPrivateKey,
                        targetDevicePublicKeyB64 = targetPublicKey
                    )

                repository.approve(
                    deviceId = device.deviceId,
                    wrappedAccountKey = wrappedAccountKey
                )

                load()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to approve device"
                )
            }
        }
    }

    fun reject(deviceId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.reject(deviceId)
                load()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to reject device"
                )
            }
        }
    }

    fun revoke(deviceId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.revoke(deviceId)
                load()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to revoke device"
                )
            }
        }
    }

    fun completePairingForThisDevice(accountPublicKey: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val deviceId = deviceIdentityStorage.getOrCreateDeviceId()

                val device = repository.fetchPairingStatus(deviceId)
                    ?: throw IllegalStateException("Device pairing request was not found.")

                if (device.pairingStatus != "approved") {
                    throw IllegalStateException("This device has not been approved yet.")
                }

                val wrappedAccountKey = device.wrappedAccountKey
                    ?: throw IllegalStateException("Approved device is missing wrapped account key.")

                val devicePrivateKey = deviceIdentityStorage.readPrivateKey()
                    ?: throw IllegalStateException("This device is missing its device identity key.")

                val restoredAccountPrivateKey =
                    provisioningCrypto.unwrapProvisionedAccountKey(
                        wrappedAccountKeyJson = wrappedAccountKey,
                        currentDevicePrivateKeyB64 = devicePrivateKey
                    )

                val accountPublicKey = accountPublicKey
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: throw IllegalStateException("Missing account public key.")

                keyStorage.saveKeyPair(
                    publicKey = accountPublicKey,
                    privateKey = restoredAccountPrivateKey
                )

                _state.value = _state.value.copy(
                    error = null
                )

                load()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to complete device pairing"
                )
            }
        }
    }

    fun requestPairingForThisDevice(
        name: String = "Android Device",
        platform: String = "Android"
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val deviceId = deviceIdentityStorage.getOrCreateDeviceId()
                val keyPair = deviceIdentityStorage.getOrCreateKeyPair()

                repository.requestPairing(
                    DeviceRegisterRequest(
                        deviceId = deviceId,
                        name = name,
                        platform = platform,
                        publicKey = keyPair.first,
                        keyAlgorithm = "curve25519",
                        keyVersion = 1
                    )
                )

                _state.value = _state.value.copy(
                    error = null
                )

                load()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to request device pairing"
                )
            }
        }
    }
}

@Composable
fun LinkedDevicesScreen(
    viewModel: LinkedDevicesViewModel,
    accountPublicKey: String?
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Linked Devices",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        ChatforiaGradientButton(
            text = stringResource(R.string.android_linked_devices_request_approval_for_this_device),
            onClick = {
                viewModel.requestPairingForThisDevice()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        ChatforiaGradientButton(
            text = stringResource(R.string.android_linked_devices_finish_device_approval),
            onClick = {
                viewModel.completePairingForThisDevice(
                    accountPublicKey = accountPublicKey
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.isLoading) {
            CircularProgressIndicator()
        }

        if (state.error != null) {
            Text(
                state.error ?: "",
                color = MaterialTheme.colorScheme.error
            )
        }

        Text("Your devices", fontWeight = FontWeight.SemiBold)

        LazyColumn {
            items(state.devices) { device ->
                DeviceRow(
                    device = device,
                    onRevoke = viewModel::revoke
                )
                HorizontalDivider()
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("Pending approvals", fontWeight = FontWeight.SemiBold)

        state.pending.forEach { device ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DeviceSummary(device)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = { viewModel.approve(device) }
                    ) {
                        Text(stringResource(R.string.android_linked_devices_approve))
                    }

                    TextButton(
                        onClick = { viewModel.reject(device.deviceId) }
                    ) {
                        Text(stringResource(R.string.android_linked_devices_reject))
                    }
                }
            }

            HorizontalDivider()
        }
    }
}

@Composable
private fun DeviceRow(
    device: LinkedDeviceDto,
    onRevoke: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        DeviceSummary(device)

        TextButton(
            onClick = { onRevoke(device.deviceId) }
        ) {
            Text(stringResource(R.string.android_linked_devices_revoke))
        }
    }
}

@Composable
private fun DeviceSummary(device: LinkedDeviceDto) {
    Column {
        Text(device.name ?: "Unknown device")
        Text(
            listOfNotNull(device.platform, device.status).joinToString(" • "),
            style = MaterialTheme.typography.bodySmall
        )
    }
}