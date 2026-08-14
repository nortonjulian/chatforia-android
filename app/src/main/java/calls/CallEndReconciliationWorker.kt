package com.chatforia.android.calls

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.chatforia.android.auth.TokenStorage
import com.chatforia.android.network.ApiClient
import java.util.concurrent.TimeUnit

interface CallEndReconciliationScheduler {
    fun enqueue(
        callId: Int,
        reason: String,
        deviceId: String?
    )
}

class WorkManagerCallEndReconciliationScheduler(
    private val context: Context
) : CallEndReconciliationScheduler {

    override fun enqueue(
        callId: Int,
        reason: String,
        deviceId: String?
    ) {
        CallEndReconciliationWorker.enqueue(
            context = context,
            callId = callId,
            reason = reason,
            deviceId = deviceId
        )
    }
}

class CallEndReconciliationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(
    appContext,
    params
) {

    override suspend fun doWork(): Result {
        val callId =
            inputData.getInt(KEY_CALL_ID, -1)

        val reason =
            inputData.getString(KEY_REASON)
                ?: "media_disconnected"

        val deviceId =
            inputData.getString(KEY_DEVICE_ID)

        if (callId <= 0) {
            return Result.failure()
        }

        val tokenStorage =
            TokenStorage(applicationContext)

        if (tokenStorage.read().isNullOrBlank()) {
            Log.w(
                TAG,
                "Call-end reconciliation stopped because " +
                    "the user is logged out callId=$callId"
            )

            return Result.success()
        }

        return try {
            CallService(
                ApiClient(tokenStorage)
            ).endCall(
                callId = callId,
                reason = reason,
                deviceId = deviceId
            )

            Log.d(
                TAG,
                "Durable call end completed " +
                    "callId=$callId reason=$reason"
            )

            Result.success()
        } catch (error: Exception) {
            Log.w(
                TAG,
                "Durable call end attempt " +
                    "${runAttemptCount + 1} failed " +
                    "callId=$callId",
                error
            )

            if (
                runAttemptCount <
                MAX_RETRY_ATTEMPTS - 1
            ) {
                Result.retry()
            } else {
                Log.e(
                    TAG,
                    "Durable call end exhausted retries " +
                        "callId=$callId"
                )

                Result.failure()
            }
        }
    }

    companion object {
        private const val TAG =
            "ChatforiaCallEnd"

        private const val KEY_CALL_ID =
            "call_id"

        private const val KEY_REASON =
            "reason"

        private const val KEY_DEVICE_ID =
            "device_id"

        private const val MAX_RETRY_ATTEMPTS =
            8

        fun enqueue(
            context: Context,
            callId: Int,
            reason: String,
            deviceId: String?
        ) {
            val input =
                Data.Builder()
                    .putInt(
                        KEY_CALL_ID,
                        callId
                    )
                    .putString(
                        KEY_REASON,
                        reason
                    )
                    .putString(
                        KEY_DEVICE_ID,
                        deviceId
                    )
                    .build()

            val constraints =
                Constraints.Builder()
                    .setRequiredNetworkType(
                        NetworkType.CONNECTED
                    )
                    .build()

            val request =
                OneTimeWorkRequestBuilder<
                    CallEndReconciliationWorker
                >()
                    .setInputData(input)
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        10,
                        TimeUnit.SECONDS
                    )
                    .build()

            WorkManager
                .getInstance(
                    context.applicationContext
                )
                .enqueueUniqueWork(
                    "chatforia_call_end_$callId",
                    ExistingWorkPolicy.KEEP,
                    request
                )

            Log.d(
                TAG,
                "Durable call end scheduled " +
                    "callId=$callId reason=$reason"
            )
        }
    }
}
