package com.chatforia.android.messages

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageStoreTest {

    @Test
    fun replaceAll_sortsMessagesByCreatedAtThenId() {
        val store = MessageStore()

        val later = message(
            id = 2,
            text = "later",
            createdAt = "2026-01-01T00:00:02Z"
        )

        val earlier = message(
            id = 1,
            text = "earlier",
            createdAt = "2026-01-01T00:00:01Z"
        )

        store.replaceAll(
            listOf(later, earlier)
        )

        val result = store.messages.value

        assertEquals(
            listOf(1, 2),
            result.map { it.id }
        )
    }

    @Test
    fun upsert_addsNewMessage() {
        val store = MessageStore()

        store.upsert(
            message(
                id = 10,
                text = "hello"
            )
        )

        assertEquals(1, store.messages.value.size)
        assertEquals("hello", store.messages.value.first().rawContent)
    }

    @Test
    fun upsert_mergesMessageWithSameServerId() {
        val store = MessageStore()

        store.upsert(
            message(
                id = 20,
                text = "old text",
                createdAt = "2026-01-01T00:00:01Z"
            )
        )

        store.upsert(
            message(
                id = 20,
                text = "new text",
                createdAt = "2026-01-01T00:00:02Z"
            )
        )

        val result = store.messages.value

        assertEquals(1, result.size)
        assertEquals(20, result.first().id)
        assertEquals("new text", result.first().rawContent)
    }

    @Test
    fun upsert_mergesOptimisticMessageWithSameClientMessageId() {
        val store = MessageStore()

        store.upsert(
            message(
                id = -123,
                clientMessageId = "client-1",
                text = "optimistic",
                optimistic = true
            )
        )

        store.upsert(
            message(
                id = 123,
                clientMessageId = "client-1",
                text = "saved",
                optimistic = false
            )
        )

        val result = store.messages.value

        assertEquals(1, result.size)
        assertEquals(123, result.first().id)
        assertEquals("saved", result.first().rawContent)
        assertFalse(result.first().optimistic)
    }

    @Test
    fun upsertAck_updatesOptimisticMessageAndMarksSent() {
        val store = MessageStore()

        store.upsert(
            message(
                id = -456,
                clientMessageId = "client-ack",
                text = "pending",
                optimistic = true,
                failed = false
            )
        )

        store.upsertAck(
            id = 456,
            clientMessageId = "client-ack",
            chatRoomId = 99,
            createdAt = "2026-01-01T00:01:00Z"
        )

        val result = store.messages.value.first()

        assertEquals(456, result.id)
        assertEquals(99, result.chatRoomId)
        assertEquals("2026-01-01T00:01:00Z", result.createdAt)
        assertFalse(result.optimistic)
        assertFalse(result.failed)

        assertEquals(
            MessageDeliveryState.SENT,
            store.deliveryStateFor("client-ack")
        )
    }

    @Test
    fun markFailed_marksDeliveryStateAndMessageFailed() {
        val store = MessageStore()

        store.upsert(
            message(
                id = -789,
                clientMessageId = "client-failed",
                text = "will fail",
                optimistic = true,
                failed = false
            )
        )

        store.markFailed("client-failed")

        val result = store.messages.value.first()

        assertFalse(result.optimistic)
        assertTrue(result.failed)

        assertEquals(
            MessageDeliveryState.FAILED,
            store.deliveryStateFor("client-failed")
        )
    }

    @Test
    fun setDeliveryState_ignoresBlankClientMessageId() {
        val store = MessageStore()

        store.setDeliveryState(
            clientMessageId = "",
            state = MessageDeliveryState.PENDING
        )

        assertNull(
            store.deliveryStateFor("")
        )
    }

    @Test
    fun markDeleted_marksMessageDeletedAndClearsContent() {
        val store = MessageStore()

        store.upsert(
            message(
                id = 300,
                text = "delete me"
            )
        )

        store.markDeleted(
            messageId = 300,
            deletedAt = "2026-01-01T00:05:00Z"
        )

        val result = store.messages.value.first()

        assertEquals(true, result.deletedForAll)
        assertEquals("2026-01-01T00:05:00Z", result.deletedAt)
        assertEquals("", result.rawContent)
        assertEquals("", result.content)
        assertEquals("", result.decryptedContent)
    }

    @Test
    fun remove_removesMessageAndPreventsFutureReplaceAllFromRestoringIt() {
        val store = MessageStore()

        store.upsert(
            message(
                id = 999001,
                text = "remove me"
            )
        )

        store.remove(999001)

        assertTrue(store.messages.value.isEmpty())

        store.replaceAll(
            listOf(
                message(
                    id = 999001,
                    text = "server tried to restore me"
                )
            )
        )

        assertTrue(store.messages.value.isEmpty())
    }

    @Test
    fun addReadBy_addsReaderOnlyOnce() {
        val store = MessageStore()

        store.upsert(
            message(
                id = 500,
                text = "read receipt"
            )
        )

        val reader = SenderDto(
            id = 77,
            username = "reader"
        )

        store.addReadBy(
            messageId = 500,
            reader = reader
        )

        store.addReadBy(
            messageId = 500,
            reader = reader
        )

        val result = store.messages.value.first()

        assertEquals(1, result.readBy.size)
        assertEquals(77, result.readBy.first().id)
    }

    private fun message(
        id: Int,
        text: String? = "hello",
        createdAt: String = "2026-01-01T00:00:00Z",
        clientMessageId: String? = null,
        senderId: Int = 1,
        chatRoomId: Int? = 10,
        optimistic: Boolean = false,
        failed: Boolean = false
    ): MessageDto {
        return MessageDto(
            id = id,
            rawContent = text,
            content = text,
            translatedForMe = null,
            decryptedContent = text,
            createdAt = createdAt,
            sender = SenderDto(
                id = senderId,
                username = "user-$senderId"
            ),
            chatRoomId = chatRoomId,
            clientMessageId = clientMessageId,
            optimistic = optimistic,
            failed = failed
        )
    }
}