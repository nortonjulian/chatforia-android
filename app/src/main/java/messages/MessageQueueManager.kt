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
class MessageQueueManager(
    private val repository: MessagesRepository,
    private val messageStore: MessageStore,
    private val queueStorage: MessageQueueStorage,
    private val scope: CoroutineScope,
    private val messageEncryptor: MessageEncryptor = MessageEncryptor()
) {

    private val mutex = Mutex()

    private var jobs: List<QueuedMessageJob> = emptyList()
    private var workerJob: Job? = null

    init {
        scope.launch(Dispatchers.IO) {
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
            scope.launch(Dispatchers.IO) {
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
            val participants =
                repository.loadRoomParticipants(job.roomId)

            val encrypted =
                if (!job.text.isNullOrBlank()) {
                    messageEncryptor.encryptMessage(
                        plaintext = job.text,
                        participants = participants
                    )
                } else {
                    null
                }

            val saved =
                repository.sendMessage(
                    roomId = job.roomId,
                    text = "",
                    clientMessageId = job.clientMessageId,
                    attachmentsInline = job.attachmentsInline,
                    contentCiphertext = encrypted?.contentCiphertext,
                    encryptedKeys = encrypted?.encryptedKeys,
                    encryptionVersion = encrypted?.encryptionVersion
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
        } catch (_: Exception) {
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

        delay(backoffMillis)

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

    companion object {
        private const val MAX_RETRY_COUNT = 10
    }
}