package com.chatforia.android.notifications

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.chatforia.android.auth.TokenStorage
import com.chatforia.android.calls.CallService
import com.chatforia.android.calls.TwilioVoicePushRegistrar
import com.chatforia.android.crypto.DeviceIdentityStorage
import com.chatforia.android.crypto.LinkedDevicesRepository
import com.chatforia.android.network.ApiClient
import java.util.concurrent.TimeUnit

class PushReconciliationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(
    appContext,
    params
) {

    override suspend fun doWork(): Result {
        val tokenStorage =
            TokenStorage(applicationContext)

        val authToken =
            tokenStorage.read()

        if (authToken.isNullOrBlank()) {
            Log.d(
                "ChatforiaFCM",
                "Push reconciliation skipped because user is logged out"
            )

            return Result.success()
        }

        val apiClient =
            ApiClient(tokenStorage)

        val registrar =
            PushTokenRegistrar(
                deviceIdentityStorage =
                    DeviceIdentityStorage(
                        applicationContext
                    ),
                linkedDevicesRepository =
                    LinkedDevicesRepository(
                        apiClient
                    ),
                pendingFcmTokenStore =
                    PendingFcmTokenStorage(
                        applicationContext
                    ),
                twilioVoicePushRegistrar =
                    TwilioVoicePushRegistrar(
                        callService =
                            CallService(apiClient)
                    )
            )

        return when (
            val registration =
                registrar.registerCurrentFcmToken()
        ) {
            is PushRegistrationResult.Success -> {
                if (
                    registration
                        .twilioVoiceRegistered
                ) {
                    Log.d(
                        "ChatforiaFCM",
                        "Durable push reconciliation completed"
                    )

                    Result.success()
                } else {
                    retryOrStop(
                        reason =
                            "Twilio Voice registration is still pending"
                    )
                }
            }

            is PushRegistrationResult.ReplacementRequired -> {
                /*
                 * A Free-account replacement requires explicit
                 * user choice. Background work must not choose
                 * a device on the user's behalf.
                 */
                Log.w(
                    "ChatforiaFCM",
                    "Push reconciliation requires device replacement confirmation"
                )

                Result.success()
            }

            is PushRegistrationResult.Failed -> {
                retryOrStop(
                    reason =
                        registration.message
                )
            }
        }
    }

    private fun retryOrStop(
        reason: String
    ): Result {
        Log.w(
            "ChatforiaFCM",
            "Push reconciliation attempt " +
                "${runAttemptCount + 1} failed: $reason"
        )

        return if (
            runAttemptCount <
            MAX_RETRY_ATTEMPTS - 1
        ) {
            Result.retry()
        } else {
            Log.e(
                "ChatforiaFCM",
                "Push reconciliation exhausted its retry budget"
            )

            Result.failure()
        }
    }

    private companion object {
        const val MAX_RETRY_ATTEMPTS = 8
    }
}

object PushReconciliationScheduler {

    private const val UNIQUE_WORK_NAME =
        "chatforia_push_reconciliation"

    fun enqueue(
        context: Context
    ) {
        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(
                    NetworkType.CONNECTED
                )
                .build()

        val request =
            OneTimeWorkRequestBuilder<
                PushReconciliationWorker
            >()
                .setConstraints(constraints)
                .setInitialDelay(
                    15,
                    TimeUnit.SECONDS
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .build()

        WorkManager
            .getInstance(
                context.applicationContext
            )
            .enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )

        Log.d(
            "ChatforiaFCM",
            "Durable push reconciliation scheduled"
        )
    }
}
