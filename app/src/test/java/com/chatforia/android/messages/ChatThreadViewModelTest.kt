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

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ChatThreadViewModelTest {

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
            }
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

    private class FakeChatThreadRepository : ChatThreadRepository {
        var messagesToLoad: List<MessageDto> = emptyList()
        var deltasToLoad: List<MessageDto> = emptyList()

        val markReadCalls = mutableListOf<List<Int>>()
        val loadDeltasCalls = mutableListOf<Pair<Int, Int>>()
        val deleteCalls = mutableListOf<Pair<Int, Boolean>>()

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
            return null
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
            throw AssertionError("sendSms should not be called in this test.")
        }

        override suspend fun editMessage(
            messageId: Int,
            text: String,
            attachments: List<AttachmentDto>,
            encryptedPayloads: Map<String, EncryptedMessagePayloadForUser>?
        ): MessageDto? {
            throw AssertionError("editMessage should not be called in this test.")
        }

        override suspend fun reportMessage(
            messageId: Int,
            reason: String,
            details: String?,
            contextCount: Int,
            blockAfterReport: Boolean
        ): ReportMessageResponse {
            throw AssertionError("reportMessage should not be called in this test.")
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