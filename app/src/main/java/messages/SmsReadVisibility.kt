package com.chatforia.android.messages

internal fun visibleIncomingSmsMessageIds(
    messages: List<SmsMessageDto>,
    visibleIds: Set<Int>
): List<Int> =
    messages.asSequence()
        .filter { message ->
            message.id > 0 && !message.isOutgoing && message.id in visibleIds
        }
        .map { it.id }
        .distinct()
        .toList()
