package com.chatforia.android.messages

import java.time.Instant

fun MessageDto.isExpired(now: Instant = Instant.now()): Boolean =
    expiresAt?.let { value ->
        runCatching { !Instant.parse(value).isAfter(now) }.getOrDefault(false)
    } ?: false

fun isElapsedDeadline(value: String?, now: Instant = Instant.now()): Boolean =
    value?.let { runCatching { !Instant.parse(it).isAfter(now) }.getOrDefault(false) }
        ?: false
