package com.chatforia.android.messages

/**
 * Conceal every existing and incoming Chatforia message while the user's
 * default-blur setting is enabled. Reveal is local to the displayed row.
 * Removed/deleted messages only show their normal deletion notice.
 *
 * Never determine this from message age or network arrival time.
 */
internal fun shouldConcealChatMessage(
    blurByDefault: Boolean,
    revealed: Boolean,
    deletedForAll: Boolean,
    deletedForSender: Boolean
): Boolean =
    blurByDefault &&
        !revealed &&
        !deletedForAll &&
        !deletedForSender
