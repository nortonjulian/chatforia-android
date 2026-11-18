package com.chatforia.android.voicemail

import org.junit.Assert.assertEquals
import org.junit.Test

class VoicemailPlaybackFormattingTest {
    @Test fun progressIsBoundedAndHandlesUnknownLength() {
        assertEquals(0f, voicemailPlaybackFraction(5_000, 0), 0f)
        assertEquals(0f, voicemailPlaybackFraction(-100, 10_000), 0f)
        assertEquals(0.5f, voicemailPlaybackFraction(5_000, 10_000), 0f)
        assertEquals(1f, voicemailPlaybackFraction(12_000, 10_000), 0f)
    }

    @Test fun playbackClockIsReadable() {
        assertEquals("0:00", formatVoicemailPlaybackTime(-1))
        assertEquals("0:05", formatVoicemailPlaybackTime(5_000))
        assertEquals("1:05", formatVoicemailPlaybackTime(65_500))
    }
}
