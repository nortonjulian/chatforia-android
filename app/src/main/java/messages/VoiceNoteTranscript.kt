package com.chatforia.android.messages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.chatforia.android.auth.TokenStorage
import com.chatforia.android.network.ApiClient
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Opt-in receiver-only voice-note transcript. Does not modify message plaintext,
 * ciphertext, audio, or send message text to transcription. Backend authorizes the
 * current account/message and fetches only the existing audio attachment.
 */
@Composable
internal fun VoiceNoteTranscript(
    message: MessageDto,
    accountId: Int,
    enabled: Boolean
) {
    if (!enabled || message.id <= 0 || message.deletedForAll == true) return

    val context = LocalContext.current
    val api = remember(accountId) { ApiClient(TokenStorage(context.applicationContext)) }
    var retry by remember(message.id, accountId) { mutableIntStateOf(0) }
    var outcome by remember(message.id, message.revision, accountId) {
        mutableStateOf<VoiceTranscriptResult>(VoiceTranscriptResult.Loading)
    }

    LaunchedEffect(accountId, message.id, message.revision, message.editedAt, enabled, retry) {
        outcome = VoiceTranscriptResult.Loading
        outcome = try {
            val raw = withContext(Dispatchers.IO) {
                api.sendRaw(ApiRequest(
                    path = "media/${message.id}/transcribe",
                    method = HttpMethod.POST,
                    requiresAuth = true
                ))
            }
            val result = Json.parseToJsonElement(raw).jsonObject
            val segments = result["transcript"]?.jsonObject?.get("segments")?.jsonArray
            val words = segments?.joinToString(" ") {
                it.jsonObject["text"]?.jsonPrimitive?.content.orEmpty()
            }?.trim()
            if (words == null) VoiceTranscriptResult.Error
            else VoiceTranscriptResult.Ready(words)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            VoiceTranscriptResult.Error
        }
    }

    Column(modifier = Modifier.padding(top = 4.dp)) {
        when (val current = outcome) {
            VoiceTranscriptResult.Loading ->
                Text("Transcribing…", style = MaterialTheme.typography.bodySmall)
            is VoiceTranscriptResult.Ready ->
                Text(
                    current.words.ifBlank { "No speech detected." },
                    style = MaterialTheme.typography.bodyMedium
                )
            VoiceTranscriptResult.Error -> {
                Text("Voice-note transcription unavailable.", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { retry++ }) { Text("Retry transcription") }
            }
        }
    }
}

private sealed interface VoiceTranscriptResult {
    data object Loading : VoiceTranscriptResult
    data class Ready(val words: String) : VoiceTranscriptResult
    data object Error : VoiceTranscriptResult
}
