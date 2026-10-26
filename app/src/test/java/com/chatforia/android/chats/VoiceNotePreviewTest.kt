package com.chatforia.android.chats

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceNotePreviewTest {
    @Test fun normalizesMediaVoiceMarkersOnly() {
        assertEquals("Voice note", voiceNotePreview("[voice note]", true))
        assertEquals("Voice note", voiceNotePreview(" [AUDIO] ", true))
    }

    @Test fun leavesUserTextAndNonMediaMarkersUntouched() {
        assertEquals("[voice note]", voiceNotePreview("[voice note]", false))
        assertEquals("Actually [voice note]", voiceNotePreview("Actually [voice note]", true))
        assertEquals("A real caption", voiceNotePreview("A real caption", true))
    }
}
