package com.chatforia.android.messages

import java.io.File

data class VoiceNoteDraft(
    val file: File,
    val durationSec: Double
)