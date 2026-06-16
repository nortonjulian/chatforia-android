package com.chatforia.android.messages

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class MessageQueueStorage(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val file = File(context.filesDir, "chatforia_message_queue.json")

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        prettyPrint = true
    }

    suspend fun load(): List<QueuedMessageJob> {
        return withContext(ioDispatcher) {
            try {
                if (!file.exists()) return@withContext emptyList()
                json.decodeFromString<List<QueuedMessageJob>>(file.readText())
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    suspend fun save(jobs: List<QueuedMessageJob>) {
        withContext(ioDispatcher) {
            val tempFile = File(file.parentFile, "${file.name}.tmp")

            tempFile.writeText(json.encodeToString(jobs))

            if (file.exists()) {
                file.delete()
            }

            tempFile.renameTo(file)
        }
    }

    suspend fun clear() {
        withContext(ioDispatcher) {
            if (file.exists()) file.delete()
        }
    }
}