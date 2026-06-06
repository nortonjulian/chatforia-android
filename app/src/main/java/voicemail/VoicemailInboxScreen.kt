package com.chatforia.android.voicemail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatforia.android.socket.SocketManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class VoicemailUiState(
    val items: List<VoicemailDto> = emptyList(),
    val selected: VoicemailDto? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class VoicemailViewModel(
    private val repository: VoicemailRepository,
    private val socketManager: SocketManager
) : ViewModel() {

    private val _state = MutableStateFlow(VoicemailUiState())
    val state: StateFlow<VoicemailUiState> = _state

    init {
        viewModelScope.launch {
            socketManager.voicemailEvents.collect {
                load()
            }
        }
    }

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                _state.value =
                    _state.value.copy(
                        items = repository.fetchVoicemails(),
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

    fun select(item: VoicemailDto) {
        _state.value = _state.value.copy(selected = item)

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.markRead(item.id, true)
            }

            load()
        }
    }

    fun delete(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.deleteVoicemail(id)
            }

            load()
        }
    }
}

@Composable
fun VoicemailInboxScreen(
    viewModel: VoicemailViewModel
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
            "Voicemail",
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

        if (state.items.isEmpty() && !state.isLoading) {
            Text("No voicemails yet.")
        }

        LazyColumn {
            items(state.items) { item ->
                VoicemailRow(
                    item = item,
                    onPlay = { viewModel.select(item) },
                    onDelete = { viewModel.delete(item.id) }
                )

                HorizontalDivider()
            }
        }

        state.selected?.let {
            Spacer(modifier = Modifier.height(16.dp))

            VoicemailPlayerScreen(voicemail = it)
        }
    }
}

@Composable
private fun VoicemailRow(
    item: VoicemailDto,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.displayName
                    ?: item.fromNumber
                    ?: item.from
                    ?: "Unknown caller",
                fontWeight = if (item.isRead == false) FontWeight.Bold else FontWeight.Normal
            )

            Text(
                item.createdAt ?: "",
                style = MaterialTheme.typography.bodySmall
            )

            if (!item.transcript.isNullOrBlank()) {
                Text(
                    item.transcript,
                    maxLines = 2,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        IconButton(onClick = onPlay) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Play voicemail")
        }

        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete voicemail")
        }
    }
}