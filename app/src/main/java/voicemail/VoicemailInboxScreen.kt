package com.chatforia.android.voicemail

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatforia.android.socket.SocketManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.ui.text.style.TextAlign
import com.chatforia.android.ui.theme.ChatforiaColors
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.chatforia.android.R
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
        modifier = Modifier.fillMaxSize()
    ) {
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
            EmptyVoicemailState()
        } else {
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
    val context = LocalContext.current

    val callerNumber =
        item.fromNumber
            ?: item.from

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
            ?: "Unknown caller"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = callerLabel,
                fontWeight =
                    if (item.isRead == false) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    }
            )

            Text(
                text = formatVoicemailDate(item.createdAt),
                style = MaterialTheme.typography.bodySmall
            )

            if (!item.transcript.isNullOrBlank()) {
                Text(
                    text = item.transcript,
                    maxLines = 2,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        IconButton(onClick = onPlay) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = stringResource(
                    R.string.android_voicemail_inbox_play_voicemail
                )
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = stringResource(
                    R.string.android_voicemail_inbox_delete_voicemail
                )
            )
        }
    }
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
            text = "When someone leaves you a voicemail,\nit will show up here.",
            style = MaterialTheme.typography.bodyLarge,
            color = ChatforiaColors.secondaryText,
            textAlign = TextAlign.Center
        )
    }
}