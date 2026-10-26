package com.chatforia.android.chats

import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationUnreadReconcileTest {
    @Test fun updatesOnlyCountsWithoutOverwritingLivePreviews() {
        val live = ConversationDto(
            kind = "chat",
            id = 85,
            title = "Friend",
            updatedAt = "2026-10-26T01:52:39Z",
            unreadCount = 0,
            last = ConversationLastDto(
                text = "Latest decrypted message",
                messageId = 1372,
                at = "2026-10-26T01:52:39Z"
            )
        )
        val staleServerPreview = ConversationDto(
            kind = "chat",
            id = 85,
            title = "Old friend title",
            updatedAt = "2026-10-26T01:51:00Z",
            unreadCount = 3,
            last = ConversationLastDto(
                text = "[encrypted message]",
                messageId = 1369
            )
        )
        val merged = mergeConversationUnreadCounts(
            listOf(live),
            listOf(staleServerPreview)
        )
        assertEquals(live.copy(unreadCount = 3), merged.single())
    }

    @Test fun preservesMissingConversationsAndTransportIdentity() {
        val chat = ConversationDto(
            kind = "chat", id = 85, title = "Chat", unreadCount = 4
        )
        val sms = ConversationDto(
            kind = "sms", id = 85, title = "SMS", unreadCount = 2
        )
        val update = ConversationDto(
            kind = "sms", id = 85, title = "SMS", unreadCount = 0
        )
        assertEquals(
            listOf(chat, sms.copy(unreadCount = 0)),
            mergeConversationUnreadCounts(listOf(chat, sms), listOf(update))
        )
    }

    @Test fun sameAuthoritativeCountDoesNotIncrementOnDuplicateEvents() {
        val current = listOf(ConversationDto(
            kind = "chat", id = 85, title = "Chat", unreadCount = 2
        ))
        val remote = listOf(ConversationDto(
            kind = "chat", id = 85, title = "Chat", unreadCount = 2
        ))
        val first = mergeConversationUnreadCounts(current, remote)
        val second = mergeConversationUnreadCounts(first, remote)
        assertEquals(2, second.single().unreadCount)
    }
}
