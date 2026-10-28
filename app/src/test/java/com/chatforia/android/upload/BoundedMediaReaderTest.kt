package com.chatforia.android.upload

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream

class BoundedMediaReaderTest {

    @Test
    fun readsMediaBelowLimit() {
        val original = ByteArray(1024) { (it % 251).toByte() }

        val result = readBoundedMedia(
            ByteArrayInputStream(original),
            maxBytes = 2048L
        )

        assertArrayEquals(original, result)
    }

    @Test
    fun acceptsMediaExactlyAtLimit() {
        val original = ByteArray(4096) { (it % 199).toByte() }

        val result = readBoundedMedia(
            ByteArrayInputStream(original),
            maxBytes = 4096L
        )

        assertArrayEquals(original, result)
    }

    @Test
    fun rejectsMediaOneByteOverLimit() {
        val original = ByteArray(4097)

        val error = assertThrows(
            IllegalArgumentException::class.java
        ) {
            readBoundedMedia(
                ByteArrayInputStream(original),
                maxBytes = 4096L
            )
        }

        assertEquals(
            "Selected media exceeds the current 10 MB upload limit.",
            error.message
        )
    }

    @Test
    fun handlesEmptyInput() {
        val result = readBoundedMedia(
            ByteArrayInputStream(byteArrayOf()),
            maxBytes = 1024L
        )

        assertEquals(0, result.size)
    }

    @Test
    fun rejectsInvalidMaximumSize() {
        assertThrows(IllegalArgumentException::class.java) {
            readBoundedMedia(
                ByteArrayInputStream(byteArrayOf(1)),
                maxBytes = 0L
            )
        }
    }
}
