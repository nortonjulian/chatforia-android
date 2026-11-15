package com.chatforia.android

import org.junit.Assert.assertEquals
import org.junit.Test

class LicenseNoticeTest {
    @Test fun collapsesOnlySameNameAndSameNotice() {
        val bytes = "firstsecond".toByteArray()
        val result = parseLicenseNotices(
            "0:5 Library\n0:5 Library\n0:5 LIBRARY\n5:6 Library\n0:5 Other library",
            bytes
        )
        assertEquals(
            listOf(
                LicenseNotice("Library", "first"),
                LicenseNotice("Library", "second"),
                LicenseNotice("Other library", "first")
            ), result
        )
    }

    @Test fun offsetsAreBytesAndAttributionIsPreserved() {
        val first = "Copyright © Example\nPermission granted.\n"
        val second = "https://example.org/license"
        val firstBytes = first.toByteArray(Charsets.UTF_8)
        val secondBytes = second.toByteArray(Charsets.UTF_8)
        val result = parseLicenseNotices(
            "0:${firstBytes.size} First library\n${firstBytes.size}:${secondBytes.size} Second library\n",
            firstBytes + secondBytes
        )
        assertEquals(listOf(LicenseNotice("First library", first), LicenseNotice("Second library", second)), result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOutOfBoundsRange() { parseLicenseNotices("2:4 Example", byteArrayOf(1, 2, 3)) }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOverflowRange() { parseLicenseNotices("1:9223372036854775807 Example", byteArrayOf(1, 2)) }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMalformedMetadata() { parseLicenseNotices("broken", byteArrayOf()) }
}
