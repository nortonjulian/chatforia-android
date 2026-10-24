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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

/** Chatforia account blocks are independent of SMS/MMS telephone-number blocks. */
@Serializable
data class BlockedAccountDto(
    val id: Int,
    val userId: Int,
    val username: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val createdAt: String? = null
) {
    val title: String
        get() = displayName?.trim()?.takeIf { it.isNotEmpty() }
            ?: username?.trim()?.takeIf { it.isNotEmpty() }
            ?: "Chatforia user"
}

internal fun localBlockedDate(
    value: String?,
    zone: ZoneId = ZoneId.systemDefault()
): String? {
    val raw = value?.trim().orEmpty()
    if (raw.isEmpty()) return null

    return try {
        OffsetDateTime.parse(raw)
            .atZoneSameInstant(zone)
            .toLocalDate()
            .toString()
    } catch (_: DateTimeParseException) {
        raw.take(10)
    }
}

@Serializable
data class BlockedAccountsResponse(val items: List<BlockedAccountDto> = emptyList())

@Serializable
data class BlockAccountResponse(val success: Boolean, val item: BlockedAccountDto? = null)

@Serializable
data class UnblockAccountResponse(
    val success: Boolean,
    val removed: Boolean = false,
    val userId: Int
)

/** All HTTP operations run off the main thread and use the normal authenticated ApiClient. */
class BlockedAccountsRepository(private val apiClient: ApiClient) {
    suspend fun listBlockedAccounts(): List<BlockedAccountDto> = withContext(Dispatchers.IO) {
        val response: BlockedAccountsResponse = apiClient.send(
            ApiRequest(path = "blocks", method = HttpMethod.GET, requiresAuth = true)
        )
        response.items
    }

    suspend fun blockAccount(userId: Int) = withContext(Dispatchers.IO) {
        require(userId > 0) { "A valid Chatforia account is required." }
        val response: BlockAccountResponse = apiClient.send(
            ApiRequest(
                path = "blocks",
                method = HttpMethod.POST,
                bodyJson = "{\"targetUserId\":$userId}",
                requiresAuth = true
            )
        )
        check(response.success) { "The account could not be blocked." }
    }

    suspend fun unblockAccount(userId: Int) = withContext(Dispatchers.IO) {
        require(userId > 0) { "A valid Chatforia account is required." }
        val response: UnblockAccountResponse = apiClient.send(
            ApiRequest(
                path = "blocks/$userId",
                method = HttpMethod.DELETE,
                requiresAuth = true
            )
        )
        check(response.success && response.userId == userId) {
            "The account could not be unblocked."
        }
    }
}

data class BlockedAccountsState(
    val loading: Boolean = false,
    val unblockingId: Int? = null,
    val items: List<BlockedAccountDto> = emptyList(),
    val error: String? = null
)

class BlockedAccountsViewModel(private val repository: BlockedAccountsRepository) : ViewModel() {
    private val _state = MutableStateFlow(BlockedAccountsState())
    val state: StateFlow<BlockedAccountsState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val items = repository.listBlockedAccounts()
                _state.value = _state.value.copy(loading = false, items = items)
            } catch (e: Exception) {
                // Don't falsely display "no blocked accounts" if loading failed.
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.message ?: "Could not load blocked accounts."
                )
            }
        }
    }

    fun unblock(item: BlockedAccountDto) {
        if (_state.value.unblockingId != null) return
        viewModelScope.launch {
            _state.value = _state.value.copy(unblockingId = item.userId, error = null)
            try {
                repository.unblockAccount(item.userId)
                _state.value = _state.value.copy(
                    unblockingId = null,
                    items = _state.value.items.filterNot { it.userId == item.userId }
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    unblockingId = null,
                    error = e.message ?: "Could not unblock the account."
                )
            }
        }
    }
}

@Composable
fun BlockedAccountsView(viewModel: BlockedAccountsViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var pendingUnblock by remember { mutableStateOf<BlockedAccountDto?>(null) }

    LaunchedEffect(viewModel) { viewModel.load() }

    pendingUnblock?.let { account ->
        AlertDialog(
            onDismissRequest = { pendingUnblock = null },
            title = { Text(stringResource(R.string.android_blocked_accounts_unblock_title)) },
            text = {
                Text(stringResource(R.string.android_blocked_accounts_unblock_confirmation, account.title))
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.unblock(account)
                    pendingUnblock = null
                }) { Text(stringResource(R.string.android_blocked_accounts_unblock)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingUnblock = null }) {
                    Text(stringResource(R.string.android_chats_cancel))
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().background(ChatforiaColors.screenBackground)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.android_plan_back),
                    tint = ChatforiaColors.primaryText
                )
            }
            Text(
                stringResource(R.string.android_blocked_accounts_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ChatforiaColors.primaryText,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = viewModel::load, enabled = !state.loading) {
                Text(stringResource(R.string.android_blocked_accounts_refresh))
            }
        }
        when {
            state.loading && state.items.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = ChatforiaColors.accent) }

            state.items.isEmpty() -> Column(
                modifier = Modifier.fillMaxSize().weight(1f).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    if (state.error == null) stringResource(R.string.android_blocked_accounts_empty)
                    else stringResource(R.string.android_blocked_accounts_load_failed),
                    style = MaterialTheme.typography.titleMedium,
                    color = ChatforiaColors.primaryText
                )
                Spacer(modifier = Modifier.padding(4.dp))
                Text(
                    stringResource(R.string.android_blocked_accounts_empty_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChatforiaColors.secondaryText
                )
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        stringResource(R.string.android_blocked_accounts_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChatforiaColors.secondaryText,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                items(items = state.items, key = { it.userId }) { item ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = ChatforiaColors.cardBackground,
                        shape = MaterialTheme.shapes.large
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ChatforiaColors.primaryText
                                )
                                item.username?.takeIf { it.isNotBlank() }?.let { username ->
                                    Text(
                                        "@$username",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ChatforiaColors.secondaryText
                                    )
                                }
                                localBlockedDate(item.createdAt)?.let { date ->
                                    Text(
                                        stringResource(R.string.android_blocked_accounts_blocked_on, date),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ChatforiaColors.secondaryText
                                    )
                                }
                            }
                            TextButton(
                                enabled = state.unblockingId == null,
                                onClick = { pendingUnblock = item }
                            ) {
                                if (state.unblockingId == item.userId) {
                                    CircularProgressIndicator(strokeWidth = 2.dp)
                                } else {
                                    Text(stringResource(R.string.android_blocked_accounts_unblock))
                                }
                            }
                        }
                    }
                }
            }
        }
        state.error?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
