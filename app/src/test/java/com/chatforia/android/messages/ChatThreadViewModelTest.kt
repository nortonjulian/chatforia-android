package com.chatforia.android.messages

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.chatforia.android.chats.ConversationDto
import com.chatforia.android.crypto.DisplayMessageDecryptor
import com.chatforia.android.crypto.EncryptedMessagePayloadForUser
import com.chatforia.android.crypto.PrivateKeyReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runCurrent
import com.chatforia.android.socket.ChatRealtimeEvents
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ChatThreadViewModelTest {

    private val realtimeJson = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadMessages_storesMessagesAndMarksOtherUserMessagesRead() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeChatThreadRepository()

            repository.messagesToLoad =
                listOf(
                    message(
                        id = 2,
                        senderId = 1,
                        text = "mine",
                        createdAt = "2026-01-01T00:00:02Z"
                    ),
                    message(
                        id = 1,
                        senderId = 99,
                        text = "theirs",
                        createdAt = "2026-01-01T00:00:01Z"
                    )
                )

            repository.deltasToLoad =
                listOf(
                    message(
                        id = 3,
                        senderId = 99,
                        text = "delta",
                        createdAt = "2026-01-01T00:00:03Z"
                    )
                )

            val viewModel = createViewModel(repository)

            viewModel.loadMessages(
                roomId = 10,
                currentUserId = 1
            )

            advanceUntilIdle()

            assertFalse(viewModel.isLoading.value)
            assertEquals(
                listOf(1, 2, 3),
                viewModel.messages.value.map { it.id }
            )
            assertEquals(
                listOf(listOf(1)),
                repository.markReadCalls
            )
            assertEquals(
                listOf(10 to 2),
                repository.loadDeltasCalls
            )
        }

    @Test
    fun sendMessage_blankMessageDoesNothing() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeChatThreadRepository()
            val viewModel = createViewModel(repository)

            val conversation =
                ConversationDto(
                    kind = "chat",
                    id = 10,
                    title = "Test Room"
                )

            viewModel.sendMessage(
                conversation = conversation,
                text = "   ",
                currentUserId = 1,
                currentUsername = "julian"
            )

            advanceUntilIdle()

            assertTrue(viewModel.messages.value.isEmpty())
            assertFalse(viewModel.isSending.value)
            assertEquals(0, repository.queuedSendCount)
        }

    @Test
    fun deleteMessage_deleteForEveryoneMarksMessageDeleted() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeChatThreadRepository()
            repository.messagesToLoad =
                listOf(
                    message(
                        id = 25,
                        senderId = 1,
                        text = "delete me"
                    )
                )

            val viewModel = createViewModel(repository)

            viewModel.loadMessages(
                roomId = 10,
                currentUserId = 1
            )

            advanceUntilIdle()

            val loaded = viewModel.messages.value.single()

            viewModel.deleteMessage(
                message = loaded,
                deleteForEveryone = true
            )

            advanceUntilIdle()

            assertEquals(
                listOf(25 to true),
                repository.deleteCalls
            )
            assertEquals(25, viewModel.messages.value.single().id)
            assertNotNull(viewModel.messages.value.single().deletedAt)
        }

    @Test
    fun deleteMessage_deleteForMeRemovesMessageLocally() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeChatThreadRepository()
            repository.messagesToLoad =
                listOf(
                    message(
                        id = 30,
                        senderId = 1,
                        text = "remove me"
                    )
                )

            val viewModel = createViewModel(repository)

            viewModel.loadMessages(
                roomId = 10,
                currentUserId = 1
            )

            advanceUntilIdle()

            val loaded = viewModel.messages.value.single()

            viewModel.deleteMessage(
                message = loaded,
                deleteForEveryone = false
            )

            advanceUntilIdle()

            assertEquals(
                listOf(30 to false),
                repository.deleteCalls
            )
            assertTrue(viewModel.messages.value.isEmpty())
        }

    @Test
    fun sendSmsMessage_successCreatesOneNonFailedSmsAndCallsRepository() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeChatThreadRepository()
            val viewModel = createViewModel(repository)

            val conversation =
                ConversationDto(
                    kind = "sms",
                    id = 20,
                    title = "SMS",
                    phone = "+15551234567"
                )

            viewModel.sendMessage(
                conversation = conversation,
                text = "  hello sms  "
            )

            advanceUntilIdle()

            assertEquals(
                listOf(
                    SendSmsCall(
                        to = "+15551234567",
                        body = "hello sms",
                        mediaUrls = emptyList()
                    )
                ),
                repository.sendSmsCalls
            )

            assertEquals(1, viewModel.smsMessages.value.size)

            val sms = viewModel.smsMessages.value.single()

            assertEquals("hello sms", sms.body)
            assertEquals("+15551234567", sms.toNumber)
            assertFalse(sms.optimistic)
            assertFalse(sms.failed)
            assertFalse(viewModel.isSending.value)
            assertEquals(null, viewModel.error.value)
        }

    @Test
    fun sendSmsMessage_failureMarksOptimisticSmsFailed() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeChatThreadRepository()
            repository.sendSmsShouldFail = true

            val viewModel = createViewModel(repository)

            val conversation =
                ConversationDto(
                    kind = "sms",
                    id = 21,
                    title = "SMS",
                    phone = "+15551234567"
                )

            viewModel.sendMessage(
                conversation = conversation,
                text = "failure test"
            )

            advanceUntilIdle()

            assertEquals(1, repository.sendSmsCalls.size)
            assertEquals(1, viewModel.smsMessages.value.size)

            val sms = viewModel.smsMessages.value.single()

            assertEquals("failure test", sms.body)
            assertFalse(sms.optimistic)
            assertTrue(sms.failed)
            assertFalse(viewModel.isSending.value)
            assertEquals("SMS boom", viewModel.error.value)
        }

    @Test
    fun sendSmsMessage_missingPhoneSetsErrorAndDoesNotCallRepository() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeChatThreadRepository()
            val viewModel = createViewModel(repository)

            val conversation =
                ConversationDto(
                    kind = "sms",
                    id = 22,
                    title = "SMS",
                    phone = "   "
                )

            viewModel.sendMessage(
                conversation = conversation,
                text = "hello"
            )

            advanceUntilIdle()

            assertTrue(repository.sendSmsCalls.isEmpty())
            assertTrue(viewModel.smsMessages.value.isEmpty())
            assertFalse(viewModel.isSending.value)
            assertEquals("Missing SMS phone number.", viewModel.error.value)
        }

    @Test
    fun sendSmsMedia_successCreatesOneNonFailedSmsAndCallsRepository() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeChatThreadRepository()
            val viewModel = createViewModel(repository)

            val conversation =
                ConversationDto(
                    kind = "sms",
                    id = 23,
                    title = "SMS",
                    phone = "+15551234567"
                )

            viewModel.sendMedia(
                conversation = conversation,
                mediaUrls = listOf("https://example.com/photo.jpg")
            )

            advanceUntilIdle()

            assertEquals(
                listOf(
                    SendSmsCall(
                        to = "+15551234567",
                        body = null,
                        mediaUrls = listOf("https://example.com/photo.jpg")
                    )
                ),
                repository.sendSmsCalls
            )

            assertEquals(1, viewModel.smsMessages.value.size)

            val sms = viewModel.smsMessages.value.single()

            assertEquals("+15551234567", sms.toNumber)
            assertTrue(sms.media.isNotEmpty())
            assertFalse(sms.optimistic)
            assertFalse(sms.failed)
            assertFalse(viewModel.isSending.value)
            assertEquals(null, viewModel.error.value)
        }

    @Test
    fun sendSmsMedia_failureMarksOptimisticSmsFailed() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeChatThreadRepository()
            repository.sendSmsShouldFail = true

            val viewModel = createViewModel(repository)

            val conversation =
                ConversationDto(
                    kind = "sms",
                    id = 24,
                    title = "SMS",
                    phone = "+15551234567"
                )

            viewModel.sendMedia(
                conversation = conversation,
                mediaUrls = listOf("https://example.com/photo.jpg")
            )

            advanceUntilIdle()

            assertEquals(1, repository.sendSmsCalls.size)
            assertEquals(1, viewModel.smsMessages.value.size)

            val sms = viewModel.smsMessages.value.single()

            assertTrue(sms.media.isNotEmpty())
            assertFalse(sms.optimistic)
            assertTrue(sms.failed)
            assertFalse(viewModel.isSending.value)
            assertEquals("SMS boom", viewModel.error.value)
        }

    @Test
    fun sendSmsMedia_emptyMediaDoesNothing() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeChatThreadRepository()
            val viewModel = createViewModel(repository)

            val conversation =
                ConversationDto(
                    kind = "sms",
                    id = 25,
                    title = "SMS",
                    phone = "+15551234567"
                )

            viewModel.sendMedia(
                conversation = conversation,
                mediaUrls = emptyList()
            )

            advanceUntilIdle()

            assertTrue(repository.sendSmsCalls.isEmpty())
            assertTrue(viewModel.smsMessages.value.isEmpty())
            assertFalse(viewModel.isSending.value)
            assertEquals(null, viewModel.error.value)
        }

    @Test
    fun sendChatMessage_showsOptimisticMessageWhileQueuedSendIsPending() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeChatThreadRepository()
            repository.queuedSendGate = CompletableDeferred()

            val viewModel = createViewModel(repository)

            val conversation =
                ConversationDto(
                    kind = "chat",
                    id = 10,
                    title = "Test Room"
                )

            viewModel.sendMessage(
                conversation = conversation,
                text = "  hello chat  ",
                currentUserId = 1,
                currentUsername = "julian"
            )

            runCurrent()

            assertEquals(1, viewModel.messages.value.size)

            val optimistic = viewModel.messages.value.single()

            assertEquals("hello chat", optimistic.rawContent)
            assertEquals("hello chat", optimistic.decryptedContent)
            assertEquals(10, optimistic.chatRoomId)
            assertEquals(1, optimistic.sender.id)
            assertTrue(optimistic.optimistic)
            assertFalse(optimistic.failed)
            assertFalse(viewModel.isSending.value)

            assertEquals(1, repository.queuedSendCalls.size)
            assertEquals(10, repository.queuedSendCalls.single().roomId)

            repository.queuedSendGate?.complete(Unit)

            advanceUntilIdle()

            val finalMessage = viewModel.messages.value.single()

            assertEquals(999, finalMessage.id)
            assertEquals("server message", finalMessage.rawContent)
            assertFalse(finalMessage.optimistic)
            assertFalse(finalMessage.failed)
        }

    @Test
    fun sendChatMessage_queueFailureMarksOptimisticMessageFailed() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeChatThreadRepository()
            repository.sendQueuedShouldFail = true

            val viewModel = createViewModel(repository)

            val conversation =
                ConversationDto(
                    kind = "chat",
                    id = 10,
                    title = "Test Room"
                )

            viewModel.sendMessage(
                conversation = conversation,
                text = "failure chat",
                currentUserId = 1,
                currentUsername = "julian"
            )

            advanceUntilIdle()

            assertTrue(
                "Expected queued send to be attempted at least once.",
                repository.queuedSendCalls.isNotEmpty()
            )

            assertEquals(1, viewModel.messages.value.size)

            val failedMessage = viewModel.messages.value.single()

            assertEquals("failure chat", failedMessage.rawContent)

            assertTrue(
                "Expected message to be marked failed, but message was: $failedMessage",
                failedMessage.failed
            )
        }

    @Test
    fun sendChatMedia_enqueuesAttachmentAndShowsOptimisticMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeChatThreadRepository()
            repository.queuedSendGate = CompletableDeferred()

            val viewModel = createViewModel(repository)

            val conversation =
                ConversationDto(
                    kind = "chat",
                    id = 10,
                    title = "Test Room"
                )

            viewModel.sendMedia(
                conversation = conversation,
                mediaUrls = listOf("https://example.com/cat.gif"),
                text = "cat gif",
                currentUserId = 1,
                currentUsername = "julian"
            )

            runCurrent()

            assertEquals(1, viewModel.messages.value.size)

            val optimistic = viewModel.messages.value.single()

            assertEquals("cat gif", optimistic.rawContent)
            assertEquals(1, optimistic.attachmentsInline.size)
            assertEquals("GIF", optimistic.attachmentsInline.single().kind)
            assertEquals("image/gif", optimistic.attachmentsInline.single().mimeType)
            assertEquals("cat gif", optimistic.attachmentsInline.single().caption)
            assertTrue(optimistic.optimistic)
            assertFalse(optimistic.failed)

            assertEquals(1, repository.queuedSendCalls.size)
            assertEquals(1, repository.queuedSendCalls.single().attachmentsInline.size)

            repository.queuedSendGate?.complete(Unit)

            advanceUntilIdle()

            val finalMessage = viewModel.messages.value.single()

            assertEquals(999, finalMessage.id)
            assertFalse(finalMessage.optimistic)
            assertFalse(finalMessage.failed)
        }

    @Test
    fun editMessage_plainMessageUpdatesLocalMessageAndCallsRepository() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeChatThreadRepository()
            val viewModel = createViewModel(repository)

            val original =
                message(
                    id = 60,
                    senderId = 1,
                    text = "old text"
                )

            viewModel.editMessage(
                message = original,
                text = "  edited text  "
            )

            advanceUntilIdle()

            assertEquals(1, repository.editMessageCalls.size)

            val call = repository.editMessageCalls.single()

            assertEquals(60, call.messageId)
            assertEquals("edited text", call.text)
            assertTrue(call.attachments.isEmpty())
            assertEquals(null, call.encryptedPayloads)

            assertEquals(1, viewModel.messages.value.size)

            val updated = viewModel.messages.value.single()

            assertEquals(60, updated.id)
            assertEquals("edited text", updated.rawContent)
            assertEquals("edited text", updated.decryptedContent)
            assertNotNull(updated.editedAt)
            assertEquals(null, viewModel.error.value)
        }

    @Test
    fun editMessage_blankTextWithNoAttachmentsDoesNothing() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeChatThreadRepository()
            val viewModel = createViewModel(repository)

            val original =
                message(
                    id = 61,
                    senderId = 1,
                    text = "old text"
                )

            viewModel.editMessage(
                message = original,
                text = "   "
            )

            advanceUntilIdle()

            assertTrue(repository.editMessageCalls.isEmpty())
            assertTrue(viewModel.messages.value.isEmpty())
            assertEquals(null, viewModel.error.value)
        }

    @Test
    fun editMessage_failureSetsError() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeChatThreadRepository()
            repository.editMessageShouldFail = true

            val viewModel = createViewModel(repository)

            val original =
                message(
                    id = 62,
                    senderId = 1,
                    text = "old text"
                )

            viewModel.editMessage(
                message = original,
                text = "new text"
            )

            advanceUntilIdle()

            assertEquals(1, repository.editMessageCalls.size)
            assertEquals("Edit boom", viewModel.error.value)
        }

    @Test
    fun reportMessage_callsRepository() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeChatThreadRepository()
            val viewModel = createViewModel(repository)

            val reported =
                message(
                    id = 70,
                    senderId = 99,
                    text = "bad message"
                )

            viewModel.reportMessage(
                message = reported,
                reason = "spam",
                details = "posting junk",
                contextCount = 12,
                blockAfterReport = true
            )

            advanceUntilIdle()

            assertEquals(1, repository.reportMessageCalls.size)

            val call = repository.reportMessageCalls.single()

            assertEquals(70, call.messageId)
            assertEquals("spam", call.reason)
            assertEquals("posting junk", call.details)
            assertEquals(12, call.contextCount)
            assertTrue(call.blockAfterReport)
            assertEquals(null, viewModel.error.value)
        }

    @Test
    fun reportMessage_failureSetsError() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeChatThreadRepository()
            repository.reportMessageShouldFail = true

            val viewModel = createViewModel(repository)

            val reported =
                message(
                    id = 71,
                    senderId = 99,
                    text = "bad message"
                )

            viewModel.reportMessage(
                message = reported,
                reason = "harassment",
                details = "bad stuff",
                contextCount = 10,
                blockAfterReport = false
            )

            advanceUntilIdle()

            assertEquals(1, repository.reportMessageCalls.size)
            assertEquals("Report boom", viewModel.error.value)
        }

    @Test
    fun realtimeMessageUpsert_addsIncomingMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeChatThreadRepository()
            val realtime = FakeRealtimeEvents()
            val viewModel = createViewModel(repository)

            viewModel.connectRealtime(
                roomId = 10,
                socketManager = realtime,
                currentUserId = 1
            )

            runCurrent()

            realtime.emitMessageUpsert(
                realtimeJson.encodeToString(
                    message(
                        id = 80,
                        senderId = 99,
                        text = "incoming realtime",
                        createdAt = "2026-01-01T00:00:01Z"
                    )
                )
            )

            advanceUntilIdle()

            assertEquals(listOf(10), realtime.joinedRooms)
            assertEquals(1, viewModel.messages.value.size)
            assertEquals(80, viewModel.messages.value.single().id)
            assertEquals("incoming realtime", viewModel.messages.value.single().rawContent)
        }

    @Test
    fun realtimeMessageUpsert_ignoresDifferentRoom() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeChatThreadRepository()
            val realtime = FakeRealtimeEvents()
            val viewModel = createViewModel(repository)

            viewModel.connectRealtime(
                roomId = 10,
                socketManager = realtime,
                currentUserId = 1
            )

            runCurrent()

            realtime.emitMessageUpsert(
                realtimeJson.encodeToString(
                    message(
                        id = 81,
                        senderId = 99,
                        text = "wrong room"
                    ).copy(
                        chatRoomId = 99
                    )
                )
            )

            advanceUntilIdle()

            assertTrue(viewModel.messages.value.isEmpty())
        }

    @Test
    fun realtimeAck_updatesOptimisticMessageWithServerId() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeChatThreadRepository()
            repository.queuedSendGate = CompletableDeferred()

            val realtime = FakeRealtimeEvents()
            val viewModel = createViewModel(repository)

            viewModel.connectRealtime(
                roomId = 10,
                socketManager = realtime,
                currentUserId = 1
            )

            runCurrent()

            val conversation =
                ConversationDto(
                    kind = "chat",
                    id = 10,
                    title = "Test Room"
                )

            viewModel.sendMessage(
                conversation = conversation,
                text = "ack me",
                currentUserId = 1,
                currentUsername = "julian"
            )

            runCurrent()

            val optimistic = viewModel.messages.value.single()
            val clientMessageId = optimistic.clientMessageId!!

            realtime.emitMessageAck(
                """
            {
              "clientMessageId": "$clientMessageId",
              "id": 777,
              "chatRoomId": 10,
              "createdAt": "2026-01-01T00:00:20Z"
            }
            """.trimIndent()
            )

            advanceUntilIdle()

            val acked = viewModel.messages.value.single()

            assertEquals(777, acked.id)
            assertEquals(clientMessageId, acked.clientMessageId)
            assertFalse(acked.optimistic)

            repository.queuedSendGate?.complete(Unit)
            advanceUntilIdle()
        }

    @Test
    fun realtimeEdited_updatesExistingMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeChatThreadRepository()
            val realtime = FakeRealtimeEvents()
            val viewModel = createViewModel(repository)

            viewModel.connectRealtime(
                roomId = 10,
                socketManager = realtime,
                currentUserId = 1
            )

            runCurrent()

            realtime.emitMessageUpsert(
                realtimeJson.encodeToString(
                    message(
                        id = 90,
                        senderId = 99,
                        text = "before edit"
                    )
                )
            )

            advanceUntilIdle()

            realtime.emitMessageEdited(
                realtimeJson.encodeToString(
                    message(
                        id = 90,
                        senderId = 99,
                        text = "after edit"
                    ).copy(
                        editedAt = "2026-01-01T00:01:00Z"
                    )
                )
            )

            advanceUntilIdle()

            assertEquals(1, viewModel.messages.value.size)

            val edited = viewModel.messages.value.single()

            assertEquals(90, edited.id)
            assertEquals("after edit", edited.rawContent)
            assertEquals("2026-01-01T00:01:00Z", edited.editedAt)
        }

    @Test
    fun realtimeDeleted_marksMessageDeleted() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeChatThreadRepository()
            val realtime = FakeRealtimeEvents()
            val viewModel = createViewModel(repository)

            viewModel.connectRealtime(
                roomId = 10,
                socketManager = realtime,
                currentUserId = 1
            )

            runCurrent()

            realtime.emitMessageUpsert(
                realtimeJson.encodeToString(
                    message(
                        id = 100,
                        senderId = 99,
                        text = "delete realtime"
                    )
                )
            )

            advanceUntilIdle()

            realtime.emitMessageDeleted(
                """
            {
              "messageId": 100,
              "chatRoomId": 10,
              "deletedAt": "2026-01-01T00:02:00Z"
            }
            """.trimIndent()
            )

            advanceUntilIdle()

            val deleted = viewModel.messages.value.single()

            assertEquals(100, deleted.id)
            assertEquals("2026-01-01T00:02:00Z", deleted.deletedAt)
        }

    @Test
    fun realtimeExpired_marksMessageDeleted() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeChatThreadRepository()
            val realtime = FakeRealtimeEvents()
            val viewModel = createViewModel(repository)

            viewModel.connectRealtime(
                roomId = 10,
                socketManager = realtime,
                currentUserId = 1
            )

            runCurrent()

            realtime.emitMessageUpsert(
                realtimeJson.encodeToString(
                    message(
                        id = 101,
                        senderId = 99,
                        text = "expire realtime"
                    )
                )
            )

            advanceUntilIdle()

            realtime.emitMessageExpired(
                """
            {
              "messageId": 101,
              "chatRoomId": 10,
              "deletedAt": "2026-01-01T00:03:00Z"
            }
            """.trimIndent()
            )

            advanceUntilIdle()

            val expired = viewModel.messages.value.single()

            assertEquals(101, expired.id)
            assertEquals("2026-01-01T00:03:00Z", expired.deletedAt)
        }

    @Test
    fun realtimeReadReceipt_addsReaderToMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeChatThreadRepository()
            val realtime = FakeRealtimeEvents()
            val viewModel = createViewModel(repository)

            viewModel.connectRealtime(
                roomId = 10,
                socketManager = realtime,
                currentUserId = 1
            )

            runCurrent()

            realtime.emitMessageUpsert(
                realtimeJson.encodeToString(
                    message(
                        id = 120,
                        senderId = 1,
                        text = "read me"
                    )
                )
            )

            advanceUntilIdle()

            realtime.emitMessageRead(
                """
            {
              "messageId": 120,
              "reader": {
                "id": 99,
                "username": "reader"
              },
              "readAt": "2026-01-01T00:04:00Z"
            }
            """.trimIndent()
            )

            advanceUntilIdle()

            val updated = viewModel.messages.value.single()

            assertEquals(120, updated.id)
            assertEquals(1, updated.readBy.size)
            assertEquals(99, updated.readBy.single().id)
            assertEquals("reader", updated.readBy.single().username)
        }

    @Test
    fun realtimeReadReceipt_doesNotDuplicateSameReader() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeChatThreadRepository()
            val realtime = FakeRealtimeEvents()
            val viewModel = createViewModel(repository)

            viewModel.connectRealtime(
                roomId = 10,
                socketManager = realtime,
                currentUserId = 1
            )

            runCurrent()

            realtime.emitMessageUpsert(
                realtimeJson.encodeToString(
                    message(
                        id = 121,
                        senderId = 1,
                        text = "read twice"
                    )
                )
            )

            advanceUntilIdle()

            val payload =
                """
            {
              "messageId": 121,
              "reader": {
                "id": 99,
                "username": "reader"
              },
              "readAt": "2026-01-01T00:04:00Z"
            }
            """.trimIndent()

            realtime.emitMessageRead(payload)
            realtime.emitMessageRead(payload)

            advanceUntilIdle()

            val updated = viewModel.messages.value.single()

            assertEquals(121, updated.id)
            assertEquals(1, updated.readBy.size)
            assertEquals(99, updated.readBy.single().id)
        }

    @Test
    fun realtimeReadReceipt_missingMessageIdDoesNothing() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeChatThreadRepository()
            val realtime = FakeRealtimeEvents()
            val viewModel = createViewModel(repository)

            viewModel.connectRealtime(
                roomId = 10,
                socketManager = realtime,
                currentUserId = 1
            )

            runCurrent()

            realtime.emitMessageUpsert(
                realtimeJson.encodeToString(
                    message(
                        id = 122,
                        senderId = 1,
                        text = "no message id"
                    )
                )
            )

            advanceUntilIdle()

            realtime.emitMessageRead(
                """
            {
              "reader": {
                "id": 99,
                "username": "reader"
              },
              "readAt": "2026-01-01T00:04:00Z"
            }
            """.trimIndent()
            )

            advanceUntilIdle()

            val updated = viewModel.messages.value.single()

            assertEquals(122, updated.id)
            assertTrue(updated.readBy.isEmpty())
        }

    @Test
    fun smsRealtime_incomingMessageIsAdded() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeChatThreadRepository()
            val realtime = FakeRealtimeEvents()
            val viewModel = createViewModel(repository)

            viewModel.connectSmsRealtime(realtime)

            runCurrent()

            realtime.emitSmsMessage(
                """
            {
              "id": 200,
              "threadId": 20,
              "direction": "in",
              "fromNumber": "+15550001111",
              "toNumber": "+15550002222",
              "body": "hello from sms realtime",
              "provider": "twilio",
              "providerMessageId": "SM200",
              "createdAt": "2026-01-01T00:05:00Z"
            }
            """.trimIndent()
            )

            advanceUntilIdle()

            assertEquals(1, viewModel.smsMessages.value.size)

            val sms = viewModel.smsMessages.value.single()

            assertEquals(200, sms.id)
            assertEquals(20, sms.threadId)
            assertEquals("in", sms.direction)
            assertEquals("+15550001111", sms.fromNumber)
            assertEquals("hello from sms realtime", sms.body)
            assertFalse(sms.optimistic)
            assertFalse(sms.failed)
        }

    @Test
    fun smsRealtime_duplicateMessageUpdatesExistingSms() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeChatThreadRepository()
            val realtime = FakeRealtimeEvents()
            val viewModel = createViewModel(repository)

            viewModel.connectSmsRealtime(realtime)

            runCurrent()

            realtime.emitSmsMessage(
                """
            {
              "id": 201,
              "threadId": 20,
              "direction": "in",
              "fromNumber": "+15550001111",
              "toNumber": "+15550002222",
              "body": "before update",
              "provider": "twilio",
              "providerMessageId": "SM201",
              "createdAt": "2026-01-01T00:05:00Z"
            }
            """.trimIndent()
            )

            advanceUntilIdle()

            realtime.emitSmsMessage(
                """
            {
              "id": 201,
              "threadId": 20,
              "direction": "in",
              "fromNumber": "+15550001111",
              "toNumber": "+15550002222",
              "body": "after update",
              "provider": "twilio",
              "providerMessageId": "SM201",
              "createdAt": "2026-01-01T00:05:00Z",
              "editedAt": "2026-01-01T00:06:00Z"
            }
            """.trimIndent()
            )

            advanceUntilIdle()

            assertEquals(1, viewModel.smsMessages.value.size)

            val sms = viewModel.smsMessages.value.single()

            assertEquals(201, sms.id)
            assertEquals("after update", sms.body)
            assertEquals("2026-01-01T00:06:00Z", sms.editedAt)
        }

    @Test
    fun smsRealtime_malformedPayloadDoesNotCrash() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeChatThreadRepository()
            val realtime = FakeRealtimeEvents()
            val viewModel = createViewModel(repository)

            viewModel.connectSmsRealtime(realtime)

            runCurrent()

            realtime.emitSmsMessage("{ this is not valid json")

            advanceUntilIdle()

            assertTrue(viewModel.smsMessages.value.isEmpty())
            assertEquals(null, viewModel.error.value)
        }


    private suspend fun createViewModel(
        repository: FakeChatThreadRepository
    ): ChatThreadViewModel {
        val context =
            ApplicationProvider.getApplicationContext<Context>()

        val storage =
            MessageQueueStorage(
                context = context,
                ioDispatcher = mainDispatcherRule.testDispatcher
            )

        storage.clear()

        return ChatThreadViewModel(
            repository = repository,
            keyStorage = FakePrivateKeyReader(),
            queueStorage = storage,
            messageDecryptorFactory = {
                NoOpDisplayMessageDecryptor()
            },
            queueDispatcher = mainDispatcherRule.testDispatcher,
            queueDelayProvider = { }
        )
    }

    private fun message(
        id: Int,
        senderId: Int,
        text: String,
        createdAt: String = "2026-01-01T00:00:00Z"
    ): MessageDto {
        return MessageDto(
            id = id,
            rawContent = text,
            content = text,
            translatedForMe = null,
            decryptedContent = text,
            createdAt = createdAt,
            sender =
                SenderDto(
                    id = senderId,
                    username = "user$senderId"
                ),
            chatRoomId = 10,
            clientMessageId = "client-$id"
        )
    }

    private class FakePrivateKeyReader : PrivateKeyReader {
        override fun readPrivateKey(): String? {
            return null
        }
    }

    private class NoOpDisplayMessageDecryptor : DisplayMessageDecryptor {
        override fun decryptMessageOrNull(
            message: MessageDto,
            currentUserPrivateKeyB64: String?,
            currentUserId: Int
        ): String? {
            return null
        }
    }

    private data class QueuedSendCall(
        val roomId: Int,
        val clientMessageId: String,
        val attachmentsInline: List<AttachmentDto>
    )

    private data class SendSmsCall(
        val to: String,
        val body: String?,
        val mediaUrls: List<String>
    )

    private data class EditMessageCall(
        val messageId: Int,
        val text: String,
        val attachments: List<AttachmentDto>,
        val encryptedPayloads: Map<String, EncryptedMessagePayloadForUser>?
    )

    private data class ReportMessageCall(
        val messageId: Int,
        val reason: String,
        val details: String?,
        val contextCount: Int,
        val blockAfterReport: Boolean
    )

    private class FakeChatThreadRepository : ChatThreadRepository {
        var messagesToLoad: List<MessageDto> = emptyList()
        var deltasToLoad: List<MessageDto> = emptyList()

        val sendSmsCalls = mutableListOf<SendSmsCall>()

        var sendSmsShouldFail = false

        val markReadCalls = mutableListOf<List<Int>>()
        val loadDeltasCalls = mutableListOf<Pair<Int, Int>>()
        val deleteCalls = mutableListOf<Pair<Int, Boolean>>()

        val queuedSendCalls = mutableListOf<QueuedSendCall>()

        val editMessageCalls = mutableListOf<EditMessageCall>()
        val reportMessageCalls = mutableListOf<ReportMessageCall>()

        var editMessageShouldFail = false
        var reportMessageShouldFail = false

        var sendQueuedShouldFail = false

        var queuedSendGate: CompletableDeferred<Unit>? = null

        var queuedSendCount = 0

        override suspend fun loadMessages(
            roomId: Int
        ): List<MessageDto> {
            return messagesToLoad
        }

        override suspend fun markReadBulk(
            ids: List<Int>
        ) {
            markReadCalls.add(ids)
        }

        override suspend fun loadDeltas(
            roomId: Int,
            sinceId: Int
        ): List<MessageDto> {
            loadDeltasCalls.add(roomId to sinceId)
            return deltasToLoad
        }

        override suspend fun deleteMessage(
            messageId: Int,
            deleteForEveryone: Boolean
        ) {
            deleteCalls.add(messageId to deleteForEveryone)
        }

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
            queuedSendCount++

            queuedSendCalls.add(
                QueuedSendCall(
                    roomId = roomId,
                    clientMessageId = clientMessageId,
                    attachmentsInline = attachmentsInline
                )
            )

            queuedSendGate?.await()

            if (sendQueuedShouldFail) {
                throw Exception("Queue boom")
            }

            return MessageDto(
                id = 999,
                rawContent = "server message",
                content = "server message",
                translatedForMe = null,
                decryptedContent = "server message",
                createdAt = "2026-01-01T00:00:10Z",
                sender = SenderDto(
                    id = 1,
                    username = "julian"
                ),
                chatRoomId = roomId,
                clientMessageId = clientMessageId,
                optimistic = false,
                failed = false,
                attachmentsInline = attachmentsInline
            )
        }

        override suspend fun loadSmsThread(
            threadId: Int
        ): SmsThreadDto {
            throw AssertionError("loadSmsThread should not be called in this test.")
        }

        override suspend fun sendSms(
            to: String,
            body: String?,
            mediaUrls: List<String>
        ): SendSmsResponse {
            sendSmsCalls.add(
                SendSmsCall(
                    to = to,
                    body = body,
                    mediaUrls = mediaUrls
                )
            )

            if (sendSmsShouldFail) {
                throw Exception("SMS boom")
            }

            return SendSmsResponse(
                ok = true,
                threadId = 20,
                provider = "twilio",
                messageSid = "SM123",
                clientRef = "client-ref"
            )
        }

        override suspend fun editMessage(
            messageId: Int,
            text: String,
            attachments: List<AttachmentDto>,
            encryptedPayloads: Map<String, EncryptedMessagePayloadForUser>?
        ): MessageDto? {
            editMessageCalls.add(
                EditMessageCall(
                    messageId = messageId,
                    text = text,
                    attachments = attachments,
                    encryptedPayloads = encryptedPayloads
                )
            )

            if (editMessageShouldFail) {
                throw Exception("Edit boom")
            }

            return MessageDto(
                id = messageId,
                rawContent = text,
                content = text,
                translatedForMe = null,
                decryptedContent = text,
                createdAt = "2026-01-01T00:00:00Z",
                sender = SenderDto(
                    id = 1,
                    username = "julian"
                ),
                chatRoomId = 10,
                clientMessageId = "edited-$messageId",
                attachments = attachments,
                attachmentsInline = attachments,
                optimistic = false,
                failed = false,
                editedAt = "2026-01-01T00:01:00Z"
            )
        }

        override suspend fun reportMessage(
            messageId: Int,
            reason: String,
            details: String?,
            contextCount: Int,
            blockAfterReport: Boolean
        ): ReportMessageResponse {
            reportMessageCalls.add(
                ReportMessageCall(
                    messageId = messageId,
                    reason = reason,
                    details = details,
                    contextCount = contextCount,
                    blockAfterReport = blockAfterReport
                )
            )

            if (reportMessageShouldFail) {
                throw Exception("Report boom")
            }

            return ReportMessageResponse(success = true)
        }
    }

    private class FakeRealtimeEvents : ChatRealtimeEvents {
        val joinedRooms = mutableListOf<Int>()

        private val _messageUpserts =
            MutableSharedFlow<String>(extraBufferCapacity = 64)

        override val messageUpserts: SharedFlow<String> =
            _messageUpserts.asSharedFlow()

        private val _messageAcks =
            MutableSharedFlow<String>(extraBufferCapacity = 64)

        override val messageAcks: SharedFlow<String> =
            _messageAcks.asSharedFlow()

        private val _messageEdited =
            MutableSharedFlow<String>(extraBufferCapacity = 64)

        override val messageEdited: SharedFlow<String> =
            _messageEdited.asSharedFlow()

        private val _messageDeleted =
            MutableSharedFlow<String>(extraBufferCapacity = 64)

        override val messageDeleted: SharedFlow<String> =
            _messageDeleted.asSharedFlow()

        private val _messageExpired =
            MutableSharedFlow<String>(extraBufferCapacity = 64)

        override val messageExpired: SharedFlow<String> =
            _messageExpired.asSharedFlow()

        private val _messageReads =
            MutableSharedFlow<String>(extraBufferCapacity = 64)

        override val messageReads: SharedFlow<String> =
            _messageReads.asSharedFlow()

        private val _socketConnected =
            MutableSharedFlow<Unit>(extraBufferCapacity = 16)

        override val socketConnected: SharedFlow<Unit> =
            _socketConnected.asSharedFlow()

        private val _smsMessages =
            MutableSharedFlow<String>(extraBufferCapacity = 64)

        override val smsMessages: SharedFlow<String> =
            _smsMessages.asSharedFlow()

        override fun joinRoom(roomId: Int) {
            joinedRooms.add(roomId)
        }

        suspend fun emitMessageUpsert(payload: String) {
            _messageUpserts.emit(payload)
        }

        suspend fun emitMessageAck(payload: String) {
            _messageAcks.emit(payload)
        }

        suspend fun emitMessageEdited(payload: String) {
            _messageEdited.emit(payload)
        }

        suspend fun emitMessageDeleted(payload: String) {
            _messageDeleted.emit(payload)
        }

        suspend fun emitMessageExpired(payload: String) {
            _messageExpired.emit(payload)
        }

        suspend fun emitMessageRead(payload: String) {
            _messageReads.emit(payload)
        }

        suspend fun emitSmsMessage(payload: String) {
            _smsMessages.emit(payload)
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {

    override fun starting(
        description: Description
    ) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(
        description: Description
    ) {
        Dispatchers.resetMain()
    }
}