package com.chatforia.android.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.chatforia.android.MainActivity
import com.chatforia.android.R

class NotificationCoordinator(
    private val context: Context
) {
    companion object {
        const val CALLS_CHANNEL_ID = "chatforia_calls_v2"
        const val MISSED_CALLS_CHANNEL_ID = "chatforia_missed_calls"
        const val MESSAGES_CHANNEL_ID = "chatforia_messages"
    }

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val callSoundUri =
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        val callAudioAttributes =
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

        val callsChannel = NotificationChannel(
            CALLS_CHANNEL_ID,
            "Incoming Calls",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Incoming Chatforia call alerts"
            setSound(callSoundUri, callAudioAttributes)
            enableVibration(true)
        }

        val missedCallsChannel = NotificationChannel(
            MISSED_CALLS_CHANNEL_ID,
            "Missed Calls",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Missed Chatforia call alerts"
            enableVibration(true)
        }

        val messagesChannel = NotificationChannel(
            MESSAGES_CHANNEL_ID,
            "Messages",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "New Chatforia message alerts"
            enableVibration(true)
            setShowBadge(true)
        }

        manager.createNotificationChannel(callsChannel)
        manager.createNotificationChannel(missedCallsChannel)
        manager.createNotificationChannel(messagesChannel)
    }

    fun showIncomingCallNotification(data: Map<String, String>) {
        if (!canPostNotifications()) return

        val fromNumber = data["fromNumber"] ?: "Unknown caller"

        val callerName =
            data["callerName"] ?: fromNumber

        val callSoundUri =
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

            putExtra("type", "call_incoming")
            putExtra("callId", data["callId"])
            putExtra("callerId", data["callerId"])
            putExtra("callerName", callerName)
            putExtra("fromNumber", fromNumber)
            putExtra("mode", data["mode"])
            putExtra("roomName", data["roomName"])
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification =
            NotificationCompat.Builder(context, CALLS_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Incoming call")
                .setContentText(callerName)
                .setStyle(NotificationCompat.BigTextStyle().bigText("Call from $callerName"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

        NotificationManagerCompat.from(context)
            .notify(1001, notification)
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

        val messageId = data["messageId"]
        val chatRoomId = data["chatRoomId"]

        val notificationId =
            ("chat_room_${chatRoomId ?: messageId ?: System.currentTimeMillis().toString()}").hashCode()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

            putExtra("type", "message_new")
            putExtra("chatRoomId", data["chatRoomId"])
            putExtra("messageId", messageId)
            putExtra("senderId", data["senderId"])
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification =
            NotificationCompat.Builder(context, MESSAGES_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

        NotificationManagerCompat.from(context)
            .notify(notificationId, notification)
    }

    fun cancelIncomingCallNotification() {
        NotificationManagerCompat.from(context)
            .cancel(1001)
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true

        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}