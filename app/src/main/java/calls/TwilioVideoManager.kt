package com.chatforia.android.calls

import android.content.Context
import com.twilio.video.CameraCapturer
import com.twilio.video.ConnectOptions
import com.twilio.video.LocalAudioTrack
import com.twilio.video.LocalVideoTrack
import com.twilio.video.RemoteParticipant
import com.twilio.video.Room
import com.twilio.video.TwilioException
import com.twilio.video.Video

class TwilioVideoManager(
    private val context: Context
) {
    interface Listener {
        fun onConnected() {}
        fun onFailed(message: String) {}
        fun onDisconnected() {}
    }

    private var room: Room? = null
    private var cameraCapturer: CameraCapturer? = null
    private var currentCameraId: String? = null
    private var localAudioTrack: LocalAudioTrack? = null
    private var localVideoTrack: LocalVideoTrack? = null
    private var listener: Listener? = null

    fun connect(
        accessToken: String,
        roomName: String,
        listener: Listener
    ) {
        this.listener = listener

        localAudioTrack =
            LocalAudioTrack.create(
                context,
                true,
                "microphone"
            )

        val cameraId = preferredCameraId()
        currentCameraId = cameraId

        val capturer =
            CameraCapturer(
                context,
                cameraId
            )

        cameraCapturer = capturer

        localVideoTrack =
            LocalVideoTrack.create(
                context,
                true,
                capturer,
                "camera"
            )

        val optionsBuilder =
            ConnectOptions.Builder(accessToken)
                .roomName(roomName)

        localAudioTrack?.let {
            optionsBuilder.audioTracks(listOf(it))
        }

        localVideoTrack?.let {
            optionsBuilder.videoTracks(listOf(it))
        }

        room =
            Video.connect(
                context,
                optionsBuilder.build(),
                roomListener
            )
    }

    fun disconnect() {
        room?.disconnect()
        cleanup()
    }

    fun setMuted(isMuted: Boolean) {
        localAudioTrack?.enable(!isMuted)
    }

    fun setCameraEnabled(enabled: Boolean) {
        localVideoTrack?.enable(enabled)
    }

    fun flipCamera() {
        // TODO: wire camera switching after confirming Twilio Android camera API.
    }

    private fun preferredCameraId(): String {
        return "front"
    }

    private fun cleanup() {
        room = null

        localVideoTrack?.release()
        localVideoTrack = null

        localAudioTrack?.release()
        localAudioTrack = null

        cameraCapturer = null
        currentCameraId = null
        listener = null
    }

    private val roomListener =
        object : Room.Listener {
            override fun onConnected(room: Room) {
                this@TwilioVideoManager.room = room
                listener?.onConnected()
            }

            override fun onReconnected(room: Room) {}

            override fun onReconnecting(
                room: Room,
                twilioException: TwilioException
            ) {}

            override fun onConnectFailure(
                room: Room,
                twilioException: TwilioException
            ) {
                val currentListener = listener
                val message = twilioException.message ?: "Video call failed."

                cleanup()

                currentListener?.onFailed(message)
            }

            override fun onDisconnected(
                room: Room,
                twilioException: TwilioException?
            ) {
                val currentListener = listener
                val message = twilioException?.message

                cleanup()

                if (message != null) {
                    currentListener?.onFailed(message)
                } else {
                    currentListener?.onDisconnected()
                }
            }

            override fun onParticipantConnected(
                room: Room,
                remoteParticipant: RemoteParticipant
            ) {}

            override fun onParticipantDisconnected(
                room: Room,
                remoteParticipant: RemoteParticipant
            ) {}

            override fun onRecordingStarted(room: Room) {}

            override fun onRecordingStopped(room: Room) {}
        }
}