package com.chatforia.android.blocking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatforia.android.R
import com.chatforia.android.network.ApiClient
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.HttpMethod
import com.chatforia.android.ui.theme.ChatforiaColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class BlockedNumberDto(
    val id: Int,
    val phone: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
private data class BlockedNumbersResponse(
    val items: List<BlockedNumberDto> = emptyList()
)

@Serializable
private data class UnblockNumberResponse(
    val success: Boolean,
    val id: Int
)

class BlockedNumbersRepository(
    private val apiClient: ApiClient
) {
    suspend fun listBlockedNumbers(): List<BlockedNumberDto> =
        withContext(Dispatchers.IO) {
            val response: BlockedNumbersResponse =
                apiClient.send(
                    ApiRequest(
                        path = "/sms/blocked-numbers",
                        method = HttpMethod.GET,
                        requiresAuth = true
                    )
                )

            response.items
        }

    suspend fun unblockNumber(id: Int) =
        withContext(Dispatchers.IO) {
            val response: UnblockNumberResponse =
                apiClient.send(
                    ApiRequest(
                        path = "/sms/blocked-numbers/$id",
                        method = HttpMethod.DELETE,
                        requiresAuth = true
                    )
                )

            if (!response.success) {
                throw IllegalStateException(
                    "Phone number was not unblocked."
                )
            }
        }
}

data class BlockedNumbersState(
    val isLoading: Boolean = false,
    val unblockingId: Int? = null,
    val items: List<BlockedNumberDto> = emptyList(),
    val error: String? = null
)

class BlockedNumbersViewModel(
    private val repository: BlockedNumbersRepository
) : ViewModel() {
    private val _state = MutableStateFlow(BlockedNumbersState())
    val state: StateFlow<BlockedNumbersState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value =
                _state.value.copy(
                    isLoading = true,
                    error = null
                )

            try {
                _state.value =
                    _state.value.copy(
                        isLoading = false,
                        items = repository.listBlockedNumbers()
                    )
            } catch (exception: Exception) {
                _state.value =
                    _state.value.copy(
                        isLoading = false,
                        error =
                            exception.message
                                ?: "Failed to load blocked numbers."
                    )
            }
        }
    }

    fun unblock(item: BlockedNumberDto) {
        viewModelScope.launch {
            _state.value =
                _state.value.copy(
                    unblockingId = item.id,
                    error = null
                )

            try {
                repository.unblockNumber(item.id)

                _state.value =
                    _state.value.copy(
                        unblockingId = null,
                        items =
                            _state.value.items.filterNot {
                                it.id == item.id
                            }
                    )
            } catch (exception: Exception) {
                _state.value =
                    _state.value.copy(
                        unblockingId = null,
                        error =
                            exception.message
                                ?: "Failed to unblock phone number."
                    )
            }
        }
    }
}

@Composable
fun BlockedNumbersView(
    viewModel: BlockedNumbersViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var pendingUnblock by remember {
        mutableStateOf<BlockedNumberDto?>(null)
    }

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    pendingUnblock?.let { item ->
        AlertDialog(
            onDismissRequest = {
                pendingUnblock = null
            },
            title = {
                Text(
                    stringResource(
                        R.string.android_blocked_numbers_unblock_title
                    )
                )
            },
            text = {
                Text(
                    stringResource(
                        R.string.android_blocked_numbers_unblock_confirmation,
                        item.phone
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.unblock(item)
                        pendingUnblock = null
                    }
                ) {
                    Text(
                        stringResource(
                            R.string.android_blocked_numbers_unblock
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingUnblock = null
                    }
                ) {
                    Text(
                        stringResource(
                            R.string.android_chats_cancel
                        )
                    )
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatforiaColors.screenBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription =
                        stringResource(R.string.android_plan_back),
                    tint = ChatforiaColors.primaryText
                )
            }

            Text(
                text =
                    stringResource(
                        R.string.android_blocked_numbers_title
                    ),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = ChatforiaColors.primaryText
            )
        }

        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = ChatforiaColors.accent
                    )
                }
            }

            state.items.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text =
                            stringResource(
                                R.string.android_blocked_numbers_empty
                            ),
                        style = MaterialTheme.typography.titleMedium,
                        color = ChatforiaColors.primaryText
                    )

                    Spacer(modifier = Modifier.padding(4.dp))

                    Text(
                        text =
                            stringResource(
                                R.string.android_blocked_numbers_empty_description
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChatforiaColors.secondaryText
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text =
                                stringResource(
                                    R.string.android_blocked_numbers_description
                                ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = ChatforiaColors.secondaryText,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    items(
                        items = state.items,
                        key = { it.id }
                    ) { item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = ChatforiaColors.cardBackground,
                            shape = MaterialTheme.shapes.large
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = item.phone,
                                        style =
                                            MaterialTheme.typography.titleMedium,
                                        color =
                                            ChatforiaColors.primaryText
                                    )

                                    Text(
                                        text =
                                            stringResource(
                                                R.string.android_blocked_numbers_blocked_on,
                                                item.createdAt.take(10)
                                            ),
                                        style =
                                            MaterialTheme.typography.bodySmall,
                                        color =
                                            ChatforiaColors.secondaryText
                                    )
                                }

                                TextButton(
                                    enabled =
                                        state.unblockingId != item.id,
                                    onClick = {
                                        pendingUnblock = item
                                    }
                                ) {
                                    if (state.unblockingId == item.id) {
                                        CircularProgressIndicator(
                                            modifier =
                                                Modifier.padding(4.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(
                                            stringResource(
                                                R.string.android_blocked_numbers_unblock
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        state.error?.let { error ->
            HorizontalDivider(
                color = ChatforiaColors.border
            )

            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
