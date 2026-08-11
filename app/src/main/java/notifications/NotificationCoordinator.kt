package com.chatforia.android.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import com.chatforia.android.ChatforiaAppState
import com.chatforia.android.MainActivity
import com.chatforia.android.R
import com.chatforia.android.sounds.AudioPlayerService

class NotificationCoordinator(
    private val context: Context
) {
    companion object {
        const val CALLS_CHANNEL_ID = "chatforia_calls_v3_custom_ringtone"
        const val MISSED_CALLS_CHANNEL_ID = "chatforia_missed_calls"
        const val MESSAGES_CHANNEL_ID = "chatforia_messages_v4_default_tone"

        private const val INCOMING_CALL_NOTIFICATION_ID = 1001
        private const val INCOMING_CALL_TIMEOUT_MS = 40_000L

        private val incomingCallTimeoutHandler =
            Handler(Looper.getMainLooper())

        private val incomingCallTimeoutLock = Any()
        private var incomingCallTimeoutRunnable: Runnable? = null
        private var incomingCallTimeoutGeneration = 0L

        private fun scheduleIncomingCallTimeout(context: Context) {
            val appContext = context.applicationContext

            synchronized(incomingCallTimeoutLock) {
                incomingCallTimeoutRunnable?.let {
                    incomingCallTimeoutHandler.removeCallbacks(it)
                }

                incomingCallTimeoutGeneration += 1L
                val generation = incomingCallTimeoutGeneration

                val timeoutRunnable =
                    Runnable {
                        val shouldRun =
                            synchronized(incomingCallTimeoutLock) {
                                if (
                                    generation !=
                                    incomingCallTimeoutGeneration
                                ) {
                                    false
                                } else {
                                    incomingCallTimeoutRunnable = null
                                    true
                                }
                            }

                        if (!shouldRun) {
                            return@Runnable
                        }

                        Log.d(
                            "ChatforiaNotifications",
                            "Incoming call timed out locally"
                        )

                        AudioPlayerService.stopSavedRingtoneShared()

                        NotificationManagerCompat
                            .from(appContext)
                            .cancel(INCOMING_CALL_NOTIFICATION_ID)
                    }

                incomingCallTimeoutRunnable = timeoutRunnable

                incomingCallTimeoutHandler.postDelayed(
                    timeoutRunnable,
                    INCOMING_CALL_TIMEOUT_MS
                )
            }
        }

        private fun cancelIncomingCallTimeout() {
            synchronized(incomingCallTimeoutLock) {
                incomingCallTimeoutGeneration += 1L

                incomingCallTimeoutRunnable?.let {
                    incomingCallTimeoutHandler.removeCallbacks(it)
                }

                incomingCallTimeoutRunnable = null
            }
        }
    }

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val callsChannel = NotificationChannel(
            CALLS_CHANNEL_ID,
            "Incoming Calls",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Incoming Chatforia call alerts"
            setSound(null, null)
            enableVibration(true)
            setShowBadge(false)
            lockscreenVisibility =
                android.app.Notification.VISIBILITY_PUBLIC
        }

        val missedCallsChannel = NotificationChannel(
            MISSED_CALLS_CHANNEL_ID,
            "Missed Calls",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Missed Chatforia call alerts"
            enableVibration(true)
        }

        val messagesChannel =
            createMessageChannel(
                channelId = MESSAGES_CHANNEL_ID,
                filename = "Default.mp3",
                channelName = "Messages"
            )


        manager.createNotificationChannel(callsChannel)
        manager.createNotificationChannel(missedCallsChannel)
        manager.createNotificationChannel(messagesChannel)

        ensureSelectedMessageChannel(manager)
    }

    private fun ensureSelectedMessageChannel(
        existingManager: NotificationManager? = null
    ): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return MESSAGES_CHANNEL_ID
        }

        val manager =
            existingManager
                ?: context.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

        val filename =
            AudioPlayerService.savedMessageTone(context)

        val rawName =
            messageToneRawResourceName(filename)

        val channelId =
            "chatforia_messages_v4_$rawName"

        if (manager.getNotificationChannel(channelId) == null) {
            val displayName =
                filename
                    .substringBeforeLast(".")
                    .ifBlank { "Default" }

            manager.createNotificationChannel(
                createMessageChannel(
                    channelId = channelId,
                    filename = filename,
                    channelName = "Messages – $displayName"
                )
            )
        }

        return channelId
    }

    private fun createMessageChannel(
        channelId: String,
        filename: String,
        channelName: String
    ): NotificationChannel {
        val rawName =
            messageToneRawResourceName(filename)

        val channel =
            NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "New Chatforia message alerts"
                enableVibration(true)
                setShowBadge(true)
            }

        if (rawName == "vibrate") {
            channel.setSound(null, null)
            channel.enableVibration(true)
            channel.setVibrationPattern(
                longArrayOf(
                    0L,
                    500L,
                    200L,
                    500L
                )
            )
            return channel
        }

        val requestedResourceId =
            context.resources.getIdentifier(
                rawName,
                "raw",
                context.packageName
            )

        val soundResourceId =
            if (requestedResourceId != 0) {
                requestedResourceId
            } else {
                Log.w(
                    "ChatforiaNotifications",
                    "Message tone resource missing: " +
                            "$filename -> $rawName; using Default.mp3"
                )

                R.raw.default_tone
            }

        val soundUri =
            Uri.parse(
                "android.resource://" +
                        "${context.packageName}/$soundResourceId"
            )

        val audioAttributes =
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(
                    AudioAttributes.CONTENT_TYPE_SONIFICATION
                )
                .build()

        channel.setSound(
            soundUri,
            audioAttributes
        )

        return channel
    }

    private fun messageToneRawResourceName(
        filename: String
    ): String {
        val base =
            filename
                .substringBeforeLast(".")
                .lowercase()
                .replace(" ", "_")
                .replace("-", "_")

        return when (base) {
            "default" -> "default_tone"
            else -> base
        }
    }

    private fun incomingCallIntent(
        data: Map<String, String>,
        callAction: String? = null
    ): Intent {
        val fromNumber =
            data["fromNumber"] ?: "Unknown caller"

        val callerName =
            data["callerName"] ?: fromNumber

        return Intent(context, MainActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP

            putExtra("type", "call_incoming")
            putExtra("callId", data["callId"])
            putExtra("callerId", data["callerId"])
            putExtra("callerName", callerName)
            putExtra("fromNumber", fromNumber)
            putExtra("mode", data["mode"])
            putExtra("roomName", data["roomName"])

            callAction?.let {
                putExtra("callAction", it)
            }
        }
    }

    fun showIncomingCallNotification(data: Map<String, String>) {
        val fromNumber =
            data["fromNumber"] ?: "Unknown caller"

        val callerName =
            data["callerName"] ?: fromNumber

        val isVideo =
            data["mode"]
                ?.equals("VIDEO", ignoreCase = true) == true ||
                    !data["roomName"].isNullOrBlank()

        IncomingCallDisplayStore.save(
            context.applicationContext,
            data
        )

        val contentIntent =
            incomingCallIntent(data)

        if (ChatforiaAppState.isInForeground) {
            Log.d(
                "ChatforiaNotifications",
                "App is foreground; opening incoming-call interface directly"
            )

            context.startActivity(contentIntent)
            return
        }

        if (!canPostNotifications()) return

        AudioPlayerService.playSavedRingtoneShared(
            context.applicationContext
        )

        scheduleIncomingCallTimeout(context)

        val callRequestCode =
            data["callId"]
                ?.toIntOrNull()
                ?: INCOMING_CALL_NOTIFICATION_ID

        val contentPendingIntent =
            PendingIntent.getActivity(
                context,
                callRequestCode,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val answerPendingIntent =
            PendingIntent.getActivity(
                context,
                callRequestCode + 1,
                incomingCallIntent(
                    data = data,
                    callAction = "answer"
                ),
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val declineIntent =
            Intent(
                context,
                IncomingCallActionReceiver::class.java
            ).apply {
                action =
                    IncomingCallActionReceiver
                        .ACTION_DECLINE_CALL

                putExtra("callId", data["callId"])
                putExtra("mode", data["mode"])
                putExtra("roomName", data["roomName"])
            }

        val declinePendingIntent =
            PendingIntent.getBroadcast(
                context,
                callRequestCode + 2,
                declineIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val caller =
            Person.Builder()
                .setName(callerName)
                .setImportant(true)
                .build()

        val callStyle =
            NotificationCompat.CallStyle
                .forIncomingCall(
                    caller,
                    declinePendingIntent,
                    answerPendingIntent
                )
                .setIsVideo(isVideo)

        if (
            Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        ) {
            val manager =
                context.getSystemService(
                    NotificationManager::class.java
                )

            Log.d(
                "ChatforiaNotifications",
                "Full-screen call permission available: " +
                        manager.canUseFullScreenIntent()
            )
        }

        val notification =
            NotificationCompat.Builder(
                context,
                CALLS_CHANNEL_ID
            )
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(
                    if (isVideo) {
                        "Incoming video call"
                    } else {
                        "Incoming call"
                    }
                )
                .setContentText(callerName)
                .setStyle(callStyle)
                .addPerson(caller)
                .setPriority(
                    NotificationCompat.PRIORITY_MAX
                )
                .setCategory(
                    NotificationCompat.CATEGORY_CALL
                )
                .setDefaults(
                    NotificationCompat.DEFAULT_VIBRATE
                )
                .setSound(null)
                .setVisibility(
                    NotificationCompat.VISIBILITY_PUBLIC
                )
                .setContentIntent(contentPendingIntent)
                .setFullScreenIntent(
                    contentPendingIntent,
                    true
                )
                .setOngoing(true)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .build()

        NotificationManagerCompat
            .from(context)
            .notify(
                INCOMING_CALL_NOTIFICATION_ID,
                notification
            )
    }

    fun showMissedCallNotification(data: Map<String, String>) {
        if (!canPostNotifications()) return

        val fromNumber = data["fromNumber"] ?: "Unknown caller"

        val notification =
            NotificationCompat.Builder(context, MISSED_CALLS_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Missed call")
                .setContentText("Missed call from $fromNumber")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

        NotificationManagerCompat.from(context)
            .notify(1002, notification)
    }

    fun showMessageNotification(data: Map<String, String>) {
        if (!canPostNotifications()) return

        val title =
            data["title"]
                ?: data["senderName"]
                ?: "Chatforia"

        val body =
            data["body"]
                ?: "New encrypted message"

        val pushType =
            data["type"]
                ?.takeIf { it.isNotBlank() }
                ?: "message_new"

        val messageId = data["messageId"]
        val chatRoomId = data["chatRoomId"]
        val smsThreadId = data["threadId"]

        val destinationKey =
            if (pushType == "sms_message") {
                "sms_thread_" +
                    (
                        smsThreadId
                            ?: messageId
                            ?: System.currentTimeMillis()
                                .toString()
                    )
            } else {
                "chat_room_" +
                    (
                        chatRoomId
                            ?: messageId
                            ?: System.currentTimeMillis()
                                .toString()
                    )
            }

        val notificationId =
            destinationKey.hashCode()

        val intent =
            Intent(
                context,
                MainActivity::class.java
            ).apply {
                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP

                putExtra("type", pushType)
                putExtra("chatRoomId", chatRoomId)
                putExtra("threadId", smsThreadId)
                putExtra("messageId", messageId)
                putExtra("senderId", data["senderId"])
            }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val messageChannelId =
            ensureSelectedMessageChannel()

        val notification =
            NotificationCompat.Builder(
                context,
                messageChannelId
            )
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setOnlyAlertOnce(false)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

        val notificationManager =
            NotificationManagerCompat.from(context)

        // Repost so a new message alerts even when this room
        // already has an existing notification.
        notificationManager.cancel(notificationId)
        notificationManager.notify(notificationId, notification)
    }

    fun cancelIncomingCallNotification() {
        cancelIncomingCallTimeout()
        AudioPlayerService.stopSavedRingtoneShared()

        NotificationManagerCompat.from(context)
            .cancel(INCOMING_CALL_NOTIFICATION_ID)

        IncomingCallDisplayStore.clear(
            context.applicationContext
        )
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true

        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}