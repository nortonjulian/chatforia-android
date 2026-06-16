package com.chatforia.android.messages


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.min
import kotlin.math.pow
import com.chatforia.android.crypto.MessageEncryptor
import kotlinx.coroutines.CoroutineDispatcher
import analytics.AnalyticsManager
import analytics.AnalyticsTracker
class MessageQueueManager(
    private val repository: MessageQueueRepository,
    private val messageStore: MessageStore,
    private val queueStorage: MessageQueueStorage,
    private val scope: CoroutineScope,
    private val messageEncryptorFactory: () -> MessageEncryptor = { MessageEncryptor() },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val delayProvider: suspend (Long) -> Unit = { delay(it) },
    private val analytics: AnalyticsTracker = AnalyticsManager
) {

    private val mutex = Mutex()

    private var jobs: List<QueuedMessageJob> = emptyList()
    private var workerJob: Job? = null

    init {
        scope.launch(ioDispatcher) {
            val restoredJobs =
                queueStorage.load()
                    .map { job ->
                        if (
                            job.state == QueuedMessageState.SENDING ||
                            job.state == QueuedMessageState.RETRYING
                        ) {
                            job.copy(state = QueuedMessageState.PENDING)
                        } else {
                            job
                        }
                    }

            mutex.withLock {
                jobs = restoredJobs
            }

            if (restoredJobs.isNotEmpty()) {
                queueStorage.save(restoredJobs)
                startIfNeeded()
            }
        }
    }

    fun enqueue(job: QueuedMessageJob) {
        scope.launch {
            mutex.withLock {
                jobs =
                    jobs
                        .filterNot { it.clientMessageId == job.clientMessageId } +
                            job.copy(state = QueuedMessageState.PENDING)

                queueStorage.save(jobs)

                messageStore.setDeliveryState(
                    clientMessageId = job.clientMessageId,
                    state = MessageDeliveryState.PENDING
                )
            }

            startIfNeeded()
        }
    }

    fun retry(clientMessageId: String) {
        scope.launch {
            mutex.withLock {
                jobs = jobs.map { job ->
                    if (job.clientMessageId == clientMessageId) {
                        job.copy(state = QueuedMessageState.PENDING)
                    } else {
                        job
                    }
                }

                queueStorage.save(jobs)

                messageStore.setDeliveryState(
                    clientMessageId = clientMessageId,
                    state = MessageDeliveryState.PENDING
                )
            }

            startIfNeeded()
        }
    }

    fun startIfNeeded() {
        if (workerJob?.isActive == true) return

        workerJob =
            scope.launch(ioDispatcher) {
                processLoop()
            }
    }

    fun stop() {
        workerJob?.cancel()
        workerJob = null
    }

    private suspend fun processLoop() {
        while (true) {
            val nextJob =
                mutex.withLock {
                    jobs
                        .filter {
                            it.state == QueuedMessageState.PENDING ||
                                    it.state == QueuedMessageState.RETRYING
                        }
                        .minByOrNull { it.createdAtMillis }
                } ?: break

            processJob(nextJob)
        }
    }

    private suspend fun processJob(job: QueuedMessageJob) {
        markSending(job)

        try {
            val participants = repository.loadRoomParticipants(job.roomId)

            val plaintextForEncryption =
                job.text?.takeIf { it.isNotBlank() }
                    ?: fallbackTextForAttachments(job.attachmentsInline)

            val encryptedPayloads =
                if (!plaintextForEncryption.isNullOrBlank()) {
                    val targetLangs =
                        participants
                            .filter { job.senderUserId == null || it.userId != job.senderUserId }
                            .filter { it.user?.autoTranslate != false }
                            .mapNotNull { it.user?.preferredLanguage?.trim()?.lowercase()?.takeIf { lang -> lang.isNotBlank() } }
                            .distinct()

                    val translations =
                        repository.translateMessagePreview(
                            roomId = job.roomId,
                            text = plaintextForEncryption,
                            targetLangs = targetLangs
                        )

                    participants.mapNotNull { participant ->
                        val user = participant.user ?: return@mapNotNull null
                        val publicKey = user.publicKey ?: return@mapNotNull null

                        if (publicKey.isBlank()) return@mapNotNull null

                        val preferredLang =
                            user.preferredLanguage
                                ?.trim()
                                ?.lowercase()
                                ?.takeIf { it.isNotBlank() }

                        val isSender =
                            job.senderUserId != null &&
                                    participant.userId == job.senderUserId

                        val plaintextForUser =
                            if (isSender || user.autoTranslate == false || preferredLang == null) {
                                plaintextForEncryption
                            } else {
                                val baseLang = preferredLang?.substringBefore("-")?.takeIf { it.isNotBlank() }

                                translations[preferredLang] ?: baseLang?.let { translations[it] } ?: plaintextForEncryption
                            }

                        participant.userId.toString() to messageEncryptorFactory().encryptForSingleUser(
                            plaintext = plaintextForUser,
                            recipientUserId = participant.userId,
                            recipientPublicKeyB64 = publicKey,
                            language = preferredLang,
                            sourceLanguage = null
                        )
                    }.toMap()
                } else {
                    emptyMap()
                }

            val saved =
                repository.sendQueuedMessage(
                    roomId = job.roomId,
                    clientMessageId = job.clientMessageId,
                    attachmentsInline = if (encryptedPayloads.isNotEmpty()) {
                        job.attachmentsInline.map { it.copy(caption = null) }
                    } else {
                        job.attachmentsInline
                    },
                    encryptedPayloads = encryptedPayloads.takeIf { it.isNotEmpty() }
                )

            if (saved != null) {
                messageStore.upsert(
                    saved.copy(
                        optimistic = false,
                        failed = false
                    )
                )
            }

            markSucceeded(job.clientMessageId)

            analytics.capture(
                "message sent",
                messageSentProperties(
                    job = job,
                    wasEncrypted = encryptedPayloads.isNotEmpty()
                )
            )
        } catch (e: Exception) {
            println("❌ MessageQueueManager failed: ${e::class.simpleName}: ${e.message}")
            markTemporaryFailure(job)
        }
    }

    private suspend fun markSending(job: QueuedMessageJob) {
        mutex.withLock {
            jobs = jobs.map { queued ->
                if (queued.clientMessageId == job.clientMessageId) {
                    queued.copy(
                        state = QueuedMessageState.SENDING,
                        lastAttemptAtMillis = System.currentTimeMillis()
                    )
                } else {
                    queued
                }
            }

            queueStorage.save(jobs)

            messageStore.setDeliveryState(
                clientMessageId = job.clientMessageId,
                state = MessageDeliveryState.SENDING
            )
        }
    }

    private suspend fun markSucceeded(clientMessageId: String) {
        mutex.withLock {
            jobs = jobs.filterNot { it.clientMessageId == clientMessageId }

            queueStorage.save(jobs)

            messageStore.setDeliveryState(
                clientMessageId = clientMessageId,
                state = MessageDeliveryState.SENT
            )
        }
    }

    private suspend fun markTemporaryFailure(job: QueuedMessageJob) {
        val nextRetryCount = job.retryCount + 1

        if (nextRetryCount > MAX_RETRY_COUNT) {
            mutex.withLock {
                jobs = jobs.map { queued ->
                    if (queued.clientMessageId == job.clientMessageId) {
                        queued.copy(
                            state = QueuedMessageState.FAILED,
                            retryCount = nextRetryCount
                        )
                    } else {
                        queued
                    }
                }

                queueStorage.save(jobs)

                messageStore.markFailed(job.clientMessageId)
            }

            return
        }

        val backoffMillis = backoffMillis(nextRetryCount)

        mutex.withLock {
            jobs = jobs.map { queued ->
                if (queued.clientMessageId == job.clientMessageId) {
                    queued.copy(
                        state = QueuedMessageState.RETRYING,
                        retryCount = nextRetryCount
                    )
                } else {
                    queued
                }
            }

            queueStorage.save(jobs)
        }

        delayProvider(backoffMillis)

        mutex.withLock {
            jobs = jobs.map { queued ->
                if (
                    queued.clientMessageId == job.clientMessageId &&
                    queued.state == QueuedMessageState.RETRYING
                ) {
                    queued.copy(state = QueuedMessageState.PENDING)
                } else {
                    queued
                }
            }

            queueStorage.save(jobs)
        }
    }

    private fun backoffMillis(retryCount: Int): Long {
        val seconds = min(
            2.0.pow(retryCount.toDouble()),
            60.0
        )

        return (seconds * 1_000).toLong()
    }

    private fun fallbackTextForAttachments(attachments: List<AttachmentDto>): String? {
        return when (attachments.firstOrNull()?.kind?.uppercase()) {
            "IMAGE" -> "[image]"
            "VIDEO" -> "[video]"
            "GIF" -> "[gif]"
            "AUDIO" -> "[voice note]"
            else -> if (attachments.isNotEmpty()) "[attachment]" else null
        }
    }

    private fun messageSentProperties(
        job: QueuedMessageJob,
        wasEncrypted: Boolean
    ): Map<String, Any> {
        return mapOf(
            "message_type" to "chat",
            "delivery_path" to "queue",
            "has_text" to !job.text.isNullOrBlank(),
            "has_attachment" to job.attachmentsInline.isNotEmpty(),
            "attachment_type" to attachmentType(job.attachmentsInline),
            "attachment_count_bucket" to attachmentCountBucket(job.attachmentsInline.size),
            "message_length_bucket" to messageLengthBucket(job.text),
            "was_encrypted" to wasEncrypted,
            "retry_count" to job.retryCount
        )
    }

    private fun messageLengthBucket(text: String?): String {
        val length = text?.trim()?.length ?: 0

        return when {
            length == 0 -> "empty"
            length < 20 -> "short"
            length < 100 -> "medium"
            else -> "long"
        }
    }

    private fun attachmentType(
        attachments: List<AttachmentDto>
    ): String {
        if (attachments.isEmpty()) return "none"

        val kinds =
            attachments
                .map { attachment ->
                    attachment.kind.trim().lowercase()
                }
                .filter { it.isNotBlank() }
                .distinct()

        return when {
            kinds.isEmpty() -> "unknown"
            kinds.size == 1 -> kinds.first()
            else -> "mixed"
        }
    }

    private fun attachmentCountBucket(count: Int): String {
        return when {
            count <= 0 -> "0"
            count == 1 -> "1"
            count <= 4 -> "2-4"
            else -> "5+"
        }
    }

    companion object {
        private const val MAX_RETRY_COUNT = 10
    }
}