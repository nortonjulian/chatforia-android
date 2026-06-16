package com.chatforia.android.messages

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.chatforia.android.crypto.EncryptedMessagePayloadForUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MessageQueueManagerTest {

    @Test
    fun enqueue_successfulSend_removesJobAndMarksSent() {
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val context = ApplicationProvider.getApplicationContext<Context>()
            val storage = cleanStorage(context, dispatcher)
            val store = MessageStore()
            val repository = FakeQueueRepository()

            val manager = MessageQueueManager(
                repository = repository,
                messageStore = store,
                queueStorage = storage,
                scope = this,
                ioDispatcher = dispatcher,
                delayProvider = { _: Long -> }
            )

            advanceUntilIdle()

            manager.enqueue(
                QueuedMessageJob(
                    clientMessageId = "client-success",
                    roomId = 10,
                    text = "hello",
                    senderUserId = 1
                )
            )

            advanceUntilIdle()

            assertEquals(
                MessageDeliveryState.SENT,
                store.deliveryStateFor("client-success")
            )

            assertTrue(storage.load().isEmpty())

            assertEquals(1, repository.sendCalls.size)
            assertEquals("client-success", repository.sendCalls.first().clientMessageId)
            assertEquals(10, repository.sendCalls.first().roomId)

            manager.stop()
        }
    }

    @Test
    fun enqueue_failedPastMaxRetries_marksFailedAndKeepsFailedJob() {
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val context = ApplicationProvider.getApplicationContext<Context>()
            val storage = cleanStorage(context, dispatcher)
            val store = MessageStore()
            val repository = FakeQueueRepository()

            repository.shouldFailSend = true

            val manager = MessageQueueManager(
                repository = repository,
                messageStore = store,
                queueStorage = storage,
                scope = this,
                ioDispatcher = dispatcher,
                delayProvider = { _: Long -> }
            )

            advanceUntilIdle()

            manager.enqueue(
                QueuedMessageJob(
                    clientMessageId = "client-fail",
                    roomId = 20,
                    text = "this will fail",
                    senderUserId = 1
                )
            )

            advanceUntilIdle()

            assertEquals(
                MessageDeliveryState.FAILED,
                store.deliveryStateFor("client-fail")
            )

            val savedJobs = storage.load()

            assertEquals(1, savedJobs.size)
            assertEquals("client-fail", savedJobs.first().clientMessageId)
            assertEquals(QueuedMessageState.FAILED, savedJobs.first().state)
            assertTrue(savedJobs.first().retryCount > 10)

            manager.stop()
        }
    }

    @Test
    fun retry_failedJob_sendsAgainAndClearsQueue() {
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val context = ApplicationProvider.getApplicationContext<Context>()
            val storage = cleanStorage(context, dispatcher)
            val store = MessageStore()
            val repository = FakeQueueRepository()

            repository.shouldFailSend = true

            val manager = MessageQueueManager(
                repository = repository,
                messageStore = store,
                queueStorage = storage,
                scope = this,
                ioDispatcher = dispatcher,
                delayProvider = { _: Long -> }
            )

            advanceUntilIdle()

            manager.enqueue(
                QueuedMessageJob(
                    clientMessageId = "client-retry",
                    roomId = 30,
                    text = "retry me",
                    senderUserId = 1
                )
            )

            advanceUntilIdle()

            assertEquals(
                MessageDeliveryState.FAILED,
                store.deliveryStateFor("client-retry")
            )

            repository.shouldFailSend = false

            manager.retry("client-retry")

            advanceUntilIdle()

            assertEquals(
                MessageDeliveryState.SENT,
                store.deliveryStateFor("client-retry")
            )

            assertTrue(storage.load().isEmpty())

            manager.stop()
        }
    }

    @Test
    fun restoredSendingAndRetryingJobs_areResetAndProcessed() {
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val context = ApplicationProvider.getApplicationContext<Context>()
            val storage = cleanStorage(context, dispatcher)
            val store = MessageStore()
            val repository = FakeQueueRepository()

            storage.save(
                listOf(
                    QueuedMessageJob(
                        clientMessageId = "client-restored-sending",
                        roomId = 40,
                        text = "was sending",
                        state = QueuedMessageState.SENDING
                    ),
                    QueuedMessageJob(
                        clientMessageId = "client-restored-retrying",
                        roomId = 40,
                        text = "was retrying",
                        state = QueuedMessageState.RETRYING
                    )
                )
            )

            val manager = MessageQueueManager(
                repository = repository,
                messageStore = store,
                queueStorage = storage,
                scope = this,
                ioDispatcher = dispatcher,
                delayProvider = { _: Long -> }
            )

            advanceUntilIdle()

            assertTrue(storage.load().isEmpty())

            assertEquals(
                MessageDeliveryState.SENT,
                store.deliveryStateFor("client-restored-sending")
            )

            assertEquals(
                MessageDeliveryState.SENT,
                store.deliveryStateFor("client-restored-retrying")
            )

            assertEquals(2, repository.sendCalls.size)

            manager.stop()
        }
    }

    private fun cleanStorage(
        context: Context,
        dispatcher: CoroutineDispatcher
    ): MessageQueueStorage {
        val queueFile = File(
            context.filesDir,
            "chatforia_message_queue.json"
        )

        queueFile.delete()

        return MessageQueueStorage(
            context = context,
            ioDispatcher = dispatcher
        )
    }

    private class FakeQueueRepository : MessageQueueRepository {
        var shouldFailSend = false

        val sendCalls =
            mutableListOf<SendCall>()

        override suspend fun loadRoomParticipants(
            roomId: Int
        ): List<RoomParticipantDto> {
            return emptyList()
        }

        override suspend fun translateMessagePreview(
            roomId: Int,
            text: String,
            targetLangs: List<String>
        ): Map<String, String> {
            return emptyMap()
        }

        override suspend fun sendQueuedMessage(
            roomId: Int,
            clientMessageId: String,
            attachmentsInline: List<AttachmentDto>,
            encryptedPayloads: Map<String, EncryptedMessagePayloadForUser>?
        ): MessageDto? {
            sendCalls.add(
                SendCall(
                    roomId = roomId,
                    clientMessageId = clientMessageId,
                    attachmentsInline = attachmentsInline,
                    encryptedPayloads = encryptedPayloads
                )
            )

            if (shouldFailSend) {
                throw RuntimeException("Network failed.")
            }

            return message(
                id = 9000 + sendCalls.size,
                roomId = roomId,
                clientMessageId = clientMessageId,
                text = "saved"
            )
        }

        private fun message(
            id: Int,
            roomId: Int,
            clientMessageId: String,
            text: String
        ): MessageDto {
            return MessageDto(
                id = id,
                rawContent = text,
                content = text,
                translatedForMe = null,
                decryptedContent = text,
                createdAt = "2026-01-01T00:00:00Z",
                sender = SenderDto(
                    id = 1,
                    username = "sender"
                ),
                chatRoomId = roomId,
                clientMessageId = clientMessageId,
                optimistic = false,
                failed = false
            )
        }
    }

    private data class SendCall(
        val roomId: Int,
        val clientMessageId: String,
        val attachmentsInline: List<AttachmentDto>,
        val encryptedPayloads: Map<String, EncryptedMessagePayloadForUser>?
    )
}