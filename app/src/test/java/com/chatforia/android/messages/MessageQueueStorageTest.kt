package com.chatforia.android.messages

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class MessageQueueStorageTest {

    private lateinit var context: Context
    private lateinit var storage: MessageQueueStorage
    private lateinit var queueFile: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        storage = MessageQueueStorage(context)
        queueFile = File(context.filesDir, "chatforia_message_queue.json")

        queueFile.delete()
    }

    @After
    fun tearDown() {
        queueFile.delete()
    }

    @Test
    fun load_returnsEmptyListWhenFileDoesNotExist() = runBlocking {
        queueFile.delete()

        val result = storage.load()

        assertTrue(result.isEmpty())
    }

    @Test
    fun saveThenLoad_returnsSameJobs() = runBlocking {
        val jobs = listOf(
            QueuedMessageJob(
                clientMessageId = "client-1",
                roomId = 10,
                text = "hello",
                senderUserId = 1,
                createdAtMillis = 1000L,
                retryCount = 0,
                state = QueuedMessageState.PENDING
            ),
            QueuedMessageJob(
                clientMessageId = "client-2",
                roomId = 20,
                text = "second message",
                senderUserId = 2,
                createdAtMillis = 2000L,
                retryCount = 3,
                lastAttemptAtMillis = 2500L,
                state = QueuedMessageState.RETRYING
            )
        )

        storage.save(jobs)

        val result = storage.load()

        assertEquals(2, result.size)

        assertEquals("client-1", result[0].clientMessageId)
        assertEquals(10, result[0].roomId)
        assertEquals("hello", result[0].text)
        assertEquals(QueuedMessageState.PENDING, result[0].state)

        assertEquals("client-2", result[1].clientMessageId)
        assertEquals(20, result[1].roomId)
        assertEquals("second message", result[1].text)
        assertEquals(3, result[1].retryCount)
        assertEquals(2500L, result[1].lastAttemptAtMillis)
        assertEquals(QueuedMessageState.RETRYING, result[1].state)
    }

    @Test
    fun clear_deletesSavedQueue() = runBlocking {
        val jobs = listOf(
            QueuedMessageJob(
                clientMessageId = "client-clear",
                roomId = 99,
                text = "clear me"
            )
        )

        storage.save(jobs)

        assertTrue(queueFile.exists())

        storage.clear()

        assertTrue(!queueFile.exists())

        val result = storage.load()

        assertTrue(result.isEmpty())
    }

    @Test
    fun load_returnsEmptyListWhenJsonIsCorrupt() = runBlocking {
        queueFile.writeText("this is not valid json")

        val result = storage.load()

        assertTrue(result.isEmpty())
    }

    @Test
    fun load_ignoresUnknownJsonKeys() = runBlocking {
        queueFile.writeText(
            """
            [
              {
                "clientMessageId": "client-unknown",
                "roomId": 123,
                "text": "hello",
                "senderUserId": 9,
                "createdAtMillis": 5000,
                "retryCount": 1,
                "lastAttemptAtMillis": 6000,
                "state": "PENDING",
                "someFutureServerField": "ignore me"
              }
            ]
            """.trimIndent()
        )

        val result = storage.load()

        assertEquals(1, result.size)
        assertEquals("client-unknown", result.first().clientMessageId)
        assertEquals(123, result.first().roomId)
        assertEquals("hello", result.first().text)
        assertEquals(9, result.first().senderUserId)
        assertEquals(QueuedMessageState.PENDING, result.first().state)
    }
}