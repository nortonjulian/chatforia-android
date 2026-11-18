package com.chatforia.android.chats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationDeletePreviewTest {
    private val chats = listOf(
        ConversationDto(
            kind = "chat",
            id = 85,
            title = "Regina",
            last = ConversationLastDto(text = "Test", messageId = 1372)
        ),
        ConversationDto(
            kind = "chat",
            id = 86,
            title = "Other",
            last = ConversationLastDto(text = "Newer", messageId = 1399)
        ),
        ConversationDto(
            kind = "sms",
            id = 85,
            title = "SMS",
            last = ConversationLastDto(text = "SMS", messageId = 1372)
        )
    )

    @Test fun nestedProductionTombstoneRefreshesOnlyCurrentLastMessage() {
        val target = parseConversationDeleteTarget(
            """{"item":{"id":1372,"chatRoomId":85,"deletedForAll":true}}"""
        )
        assertEquals(ConversationDeleteTarget(85, 1372), target)
        assertTrue(shouldRefreshDeletedPreview(chats, target!!.roomId, target.messageId))
    }

    @Test fun flatAndDeleteForMeEventsAreSupported() {
        assertEquals(
            ConversationDeleteTarget(85, 1372),
            parseConversationDeleteTarget("""{"id":1372,"chatRoomId":85}""")
        )
        assertEquals(
            ConversationDeleteTarget(85, 1372),
            parseConversationDeleteTarget(
                """{"roomId":85,"item":{"id":1372,"deletedForMe":true}}"""
            )
        )
    }

    @Test fun olderOrUnrelatedDeletesLeaveCurrentPreviewUntouched() {
        assertFalse(shouldRefreshDeletedPreview(chats, 85, 1369))
        assertFalse(shouldRefreshDeletedPreview(chats, 86, 1372))
        assertFalse(shouldRefreshDeletedPreview(chats, 999, 1372))
    }

    @Test fun malformedPayloadNeverGuessesMessageOrRoom() {
        assertNull(parseConversationDeleteTarget("""{"item":{"id":1372}}"""))
        assertNull(parseConversationDeleteTarget("""{"roomId":85}"""))
        assertNull(parseConversationDeleteTarget("{}"))
    }
}
