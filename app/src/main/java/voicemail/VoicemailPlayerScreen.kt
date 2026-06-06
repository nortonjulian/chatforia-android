package com.chatforia.android.voicemail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun VoicemailPlayerScreen(
    voicemail: VoicemailDto
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Selected voicemail",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            voicemail.audioUrl ?: "No audio URL available.",
            style = MaterialTheme.typography.bodySmall
        )

        if (!voicemail.transcript.isNullOrBlank()) {
            Text(voicemail.transcript)
        }
    }
}