package com.chatforia.android.voicemail

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatforia.android.R
import com.chatforia.android.socket.SocketManager
import com.chatforia.android.ui.theme.ChatforiaColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
            _state.value = _state.value.copy(
                isLoading = true,
                error = null
            )

            try {
                _state.value = _state.value.copy(
                    items = repository.fetchVoicemails(),
                    isLoading = false
                )
            } catch (error: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = error.message
                )
            }
        }
    }

    fun select(item: VoicemailDto) {
        _state.value = _state.value.copy(selected = item)

        if (item.isRead != true) {
            markRead(item, true)
        }
    }

    fun clearSelection() {
        _state.value = _state.value.copy(selected = null)
    }

    fun markRead(
        item: VoicemailDto,
        isRead: Boolean
    ) {
        _state.value = _state.value.copy(
            items = _state.value.items.map { existing ->
                if (existing.id == item.id) {
                    existing.copy(isRead = isRead)
                } else {
                    existing
                }
            },
            selected = _state.value.selected?.let { selected ->
                if (selected.id == item.id) {
                    selected.copy(isRead = isRead)
                } else {
                    selected
                }
            }
        )

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.markRead(item.id, isRead)
            }.onFailure {
                load()
            }
        }
    }

    fun delete(item: VoicemailDto) {
        _state.value = _state.value.copy(
            items = _state.value.items.filterNot {
                it.id == item.id
            },
            selected = if (_state.value.selected?.id == item.id) {
                null
            } else {
                _state.value.selected
            }
        )

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.deleteVoicemail(item.id)
            }.onFailure {
                load()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoicemailInboxScreen(
    viewModel: VoicemailViewModel,
    onCallBack: (VoicemailDto) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    var pendingDelete by remember {
        mutableStateOf<VoicemailDto?>(null)
    }

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            state.isLoading && state.items.isEmpty() -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            state.items.isEmpty() -> {
                EmptyVoicemailState()
            }

            else -> {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(28.dp),
                    color = ChatforiaColors.cardBackground,
                    tonalElevation = 2.dp
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            horizontal = 12.dp,
                            vertical = 6.dp
                        )
                    ) {
                        items(
                            items = state.items,
                            key = { it.id }
                        ) { item ->
                            VoicemailRow(
                                item = item,
                                onPlay = {
                                    viewModel.select(item)
                                },
                                onToggleRead = {
                                    viewModel.markRead(
                                        item = item,
                                        isRead = item.isRead != true
                                    )
                                },
                                onDelete = {
                                    pendingDelete = item
                                }
                            )

                            HorizontalDivider()
                        }
                    }
                }
            }
        }

        state.error?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }

    state.selected?.let { selected ->
        ModalBottomSheet(
            onDismissRequest = viewModel::clearSelection,
            containerColor = ChatforiaColors.screenBackground
        ) {
            VoicemailDetailSheet(
                voicemail = selected,
                onCallBack = {
                    viewModel.clearSelection()
                    onCallBack(selected)
                },
                onToggleRead = {
                    viewModel.markRead(
                        item = selected,
                        isRead = selected.isRead != true
                    )
                },
                onDelete = {
                    pendingDelete = selected
                }
            )
        }
    }

    pendingDelete?.let { voicemail ->
        AlertDialog(
            onDismissRequest = {
                pendingDelete = null
            },
            title = {
                Text(stringResource(R.string.android_voicemail_inbox_delete_voicemail))
            },
            text = {
                Text(
                    stringResource(R.string.android_voicemail_delete_message)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(voicemail)
                        pendingDelete = null
                    }
                ) {
                    Text(stringResource(R.string.android_voicemail_delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                    }
                ) {
                    Text(stringResource(R.string.android_voicemail_cancel))
                }
            }
        )
    }
}

@Composable
private fun VoicemailRow(
    item: VoicemailDto,
    onPlay: () -> Unit,
    onToggleRead: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var menuExpanded by remember(item.id) {
        mutableStateOf(false)
    }

    val callerNumber = item.fromNumber ?: item.from

    val localContactName by produceState<String?>(
        initialValue = null,
        key1 = item.callerUserId,
        key2 = callerNumber
    ) {
        value =
            if (
                item.callerUserId != null ||
                callerNumber.isNullOrBlank()
            ) {
                null
            } else {
                withContext(Dispatchers.IO) {
                    lookupPhoneContactName(
                        context = context,
                        rawNumber = callerNumber
                    )
                }
            }
    }

    val callerLabel =
        item.displayName
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: item.username
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { "@$it" }
            ?: localContactName
            ?: callerNumber
            ?: stringResource(R.string.android_voicemail_unknown_caller)

    val unread = item.isRead != true

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 7.dp)
                .size(9.dp)
                .background(
                    color = if (unread) {
                        ChatforiaColors.accent
                    } else {
                        androidx.compose.ui.graphics.Color.Transparent
                    },
                    shape = CircleShape
                )
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = callerLabel,
                fontWeight = if (unread) {
                    FontWeight.Bold
                } else {
                    FontWeight.SemiBold
                },
                color = ChatforiaColors.primaryText,
                maxLines = 1
            )

            Text(
                text = formatVoicemailMetadata(item),
                style = MaterialTheme.typography.bodySmall,
                color = ChatforiaColors.secondaryText,
                maxLines = 1
            )

            item.transcript
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { transcript ->
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = transcript,
                        maxLines = 2,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChatforiaColors.secondaryText
                    )
                }
        }

        IconButton(onClick = onPlay) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = stringResource(
                    R.string.android_voicemail_inbox_play_voicemail
                ),
                tint = ChatforiaColors.accent
            )
        }

        Box {
            IconButton(
                onClick = {
                    menuExpanded = true
                }
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.android_voicemail_options),
                    tint = ChatforiaColors.secondaryText
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = {
                    menuExpanded = false
                }
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            if (unread) {
                                stringResource(R.string.android_voicemail_mark_read)
                            } else {
                                stringResource(R.string.android_voicemail_mark_unread)
                            }
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onToggleRead()
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text(stringResource(R.string.android_voicemail_delete))
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
private fun VoicemailDetailSheet(
    voicemail: VoicemailDto,
    onCallBack: () -> Unit,
    onToggleRead: () -> Unit,
    onDelete: () -> Unit
) {
    val callerLabel =
        voicemail.displayName
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: voicemail.username
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { "@$it" }
            ?: voicemail.fromNumber
            ?: voicemail.from
            ?: stringResource(R.string.android_voicemail_unknown_caller)

    val callbackNumber =
        voicemail.fromNumber ?: voicemail.from

    val canCallBack =
        voicemail.callerUserId != null ||
            !callbackNumber.isNullOrBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        Text(
            text = callerLabel,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = ChatforiaColors.primaryText
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = formatVoicemailMetadata(voicemail),
            style = MaterialTheme.typography.bodyMedium,
            color = ChatforiaColors.secondaryText
        )

        Spacer(modifier = Modifier.height(18.dp))

        if (canCallBack) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onCallBack,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ChatforiaColors.accent
                )
            ) {
                Icon(
                    Icons.Default.Phone,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(stringResource(R.string.android_voicemail_call_back))
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        VoicemailPlayerScreen(
            voicemail = voicemail
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onToggleRead
            ) {
                Text(
                    if (voicemail.isRead == true) {
                        stringResource(R.string.android_voicemail_mark_unread)
                    } else {
                        stringResource(R.string.android_voicemail_mark_read)
                    }
                )
            }

            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onDelete
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(stringResource(R.string.android_voicemail_delete))
            }
        }
    }
}

private fun formatVoicemailMetadata(
    voicemail: VoicemailDto
): String {
    val date = formatVoicemailDate(voicemail.createdAt)
    val duration = voicemail.durationSec?.let(::formatDuration)

    return listOfNotNull(
        date.takeIf { it.isNotBlank() },
        duration
    ).joinToString(" · ")
}

private fun formatDuration(
    seconds: Int
): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val remainder = safeSeconds % 60

    return "$minutes:${remainder.toString().padStart(2, '0')}"
}

private fun lookupPhoneContactName(
    context: Context,
    rawNumber: String
): String? {
    val hasPermission =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

    if (!hasPermission) {
        return null
    }

    val directLookupUri =
        Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(rawNumber)
        )

    val directMatch =
        runCatching {
            context.contentResolver.query(
                directLookupUri,
                arrayOf(
                    ContactsContract.PhoneLookup.DISPLAY_NAME
                ),
                null,
                null,
                null
            )?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    null
                } else {
                    val nameIndex =
                        cursor.getColumnIndex(
                            ContactsContract.PhoneLookup.DISPLAY_NAME
                        )

                    if (nameIndex < 0) {
                        null
                    } else {
                        cursor
                            .getString(nameIndex)
                            ?.trim()
                            ?.takeIf { it.isNotBlank() }
                    }
                }
            }
        }.getOrNull()

    if (!directMatch.isNullOrBlank()) {
        return directMatch
    }

    val targetDigits =
        rawNumber.filter { character ->
            character.isDigit()
        }

    if (targetDigits.length < 7) {
        return null
    }

    return runCatching {
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            null
        )?.use { cursor ->
            val nameIndex =
                cursor.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                )

            val numberIndex =
                cursor.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )

            if (nameIndex < 0 || numberIndex < 0) {
                return@use null
            }

            var matchedName: String? = null

            while (
                cursor.moveToNext() &&
                matchedName == null
            ) {
                val storedNumber =
                    cursor.getString(numberIndex).orEmpty()

                val storedDigits =
                    storedNumber.filter { character ->
                        character.isDigit()
                    }

                val androidMatch =
                    android.telephony.PhoneNumberUtils.compare(
                        rawNumber,
                        storedNumber
                    )

                val exactDigitMatch =
                    targetDigits == storedDigits

                val nationalNumberMatch =
                    targetDigits.length >= 10 &&
                        storedDigits.length >= 10 &&
                        targetDigits.takeLast(10) ==
                            storedDigits.takeLast(10)

                if (
                    androidMatch ||
                    exactDigitMatch ||
                    nationalNumberMatch
                ) {
                    matchedName =
                        cursor
                            .getString(nameIndex)
                            ?.trim()
                            ?.takeIf { it.isNotBlank() }
                }
            }

            matchedName
        }
    }.getOrNull()
}

private fun formatVoicemailDate(
    rawDate: String?
): String {
    if (rawDate.isNullOrBlank()) {
        return ""
    }

    return runCatching {
        val instant = Instant.parse(rawDate)

        DateTimeFormatter
            .ofPattern(
                "MMM d, yyyy 'at' h:mm a",
                Locale.getDefault()
            )
            .withZone(ZoneId.systemDefault())
            .format(instant)
    }.getOrElse {
        rawDate
    }
}

@Composable
private fun EmptyVoicemailState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.GraphicEq,
            contentDescription = null,
            tint = ChatforiaColors.accent,
            modifier = Modifier.size(44.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = stringResource(R.string.android_voicemail_inbox_no_voicemails_yet),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = ChatforiaColors.primaryText
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.android_voicemail_empty_description),
            style = MaterialTheme.typography.bodyLarge,
            color = ChatforiaColors.secondaryText,
            textAlign = TextAlign.Center
        )
    }
}