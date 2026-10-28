package com.chatforia.android.upload

import java.io.ByteArrayOutputStream
import java.io.InputStream

internal fun readBoundedMedia(
    input: InputStream,
    maxBytes: Long
): ByteArray {
    require(maxBytes > 0) { "Maximum upload size must be positive." }

    val output = ByteArrayOutputStream()
    val buffer = ByteArray(64 * 1024)
    var total = 0L

    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        if (count == 0) continue

        total += count
        if (total > maxBytes) {
            throw IllegalArgumentException(
                "Selected media exceeds the current 10 MB upload limit."
            )
        }

        output.write(buffer, 0, count)
    }

    return output.toByteArray()
}
