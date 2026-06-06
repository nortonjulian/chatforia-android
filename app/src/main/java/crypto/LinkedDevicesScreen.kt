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

data class LinkedDevicesUiState(
    val devices: List<LinkedDeviceDto> = emptyList(),
    val pending: List<LinkedDeviceDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class LinkedDevicesViewModel(
    private val repository: LinkedDevicesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LinkedDevicesUiState())
    val state: StateFlow<LinkedDevicesUiState> = _state

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                _state.value =
                    _state.value.copy(
                        devices = repository.fetchMine(),
                        pending = repository.fetchPendingPairing(),
                        isLoading = false
                    )
            } catch (e: Exception) {
                _state.value =
                    _state.value.copy(
                        isLoading = false,
                        error = e.message
                    )
            }
        }
    }

    fun reject(deviceId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.reject(deviceId)
            }

            load()
        }
    }
}

@Composable
fun LinkedDevicesScreen(
    viewModel: LinkedDevicesViewModel
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
                DeviceRow(device)
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

                TextButton(
                    onClick = { viewModel.reject(device.id) }
                ) {
                    Text("Reject")
                }
            }

            HorizontalDivider()
        }
    }
}

@Composable
private fun DeviceRow(device: LinkedDeviceDto) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        DeviceSummary(device)
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