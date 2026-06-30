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
import tvi.webrtc.Camera1Enumerator
import com.twilio.video.RemoteAudioTrack
import com.twilio.video.RemoteAudioTrackPublication
import com.twilio.video.RemoteDataTrack
import com.twilio.video.RemoteDataTrackPublication
import com.twilio.video.RemoteVideoTrack
import com.twilio.video.RemoteVideoTrackPublication

class TwilioVideoManager(
    private val context: Context
) : CallVideoClient {
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
    private var listener: CallVideoClient.Listener? = null

    override fun connect(
        accessToken: String,
        roomName: String,
        listener: CallVideoClient.Listener
    ) {
        try {
            room?.disconnect()
        } catch (_: Exception) {
        }

        cleanup()

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

        listener.onLocalVideoTrack(localVideoTrack)

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

    override fun disconnect() {
        val currentListener = listener
        val hadRoom = room != null

        try {
            room?.disconnect()
        } catch (_: Exception) {
        }

        cleanup()

        if (!hadRoom) {
            listener = null
            currentListener?.onDisconnected()
        }
    }

    override fun setMuted(isMuted: Boolean) {
        localAudioTrack?.enable(!isMuted)
    }

    override fun setCameraEnabled(enabled: Boolean) {
        localVideoTrack?.enable(enabled)
    }

    override fun flipCamera() {
        // TODO: wire camera switching after confirming Twilio Android camera API.
    }

    private fun preferredCameraId(): String {
        val enumerator = Camera1Enumerator(false)

        val frontCamera =
            enumerator.deviceNames.firstOrNull { cameraName ->
                enumerator.isFrontFacing(cameraName)
            }

        return frontCamera
            ?: enumerator.deviceNames.firstOrNull()
            ?: throw IllegalStateException("No camera device found.")
    }

    private fun wireRemoteParticipant(remoteParticipant: RemoteParticipant) {
        remoteParticipant.setListener(remoteParticipantListener)

        val existingVideoTrack =
            remoteParticipant.remoteVideoTracks
                .firstOrNull { publication ->
                    publication.isTrackSubscribed &&
                            publication.remoteVideoTrack != null
                }
                ?.remoteVideoTrack

        if (existingVideoTrack != null) {
            listener?.onRemoteVideoTrack(existingVideoTrack)
        }
    }

    private val remoteParticipantListener =
        object : RemoteParticipant.Listener {
            override fun onAudioTrackPublished(
                remoteParticipant: RemoteParticipant,
                remoteAudioTrackPublication: RemoteAudioTrackPublication
            ) {}

            override fun onAudioTrackUnpublished(
                remoteParticipant: RemoteParticipant,
                remoteAudioTrackPublication: RemoteAudioTrackPublication
            ) {}

            override fun onAudioTrackSubscribed(
                remoteParticipant: RemoteParticipant,
                remoteAudioTrackPublication: RemoteAudioTrackPublication,
                remoteAudioTrack: RemoteAudioTrack
            ) {}

            override fun onAudioTrackUnsubscribed(
                remoteParticipant: RemoteParticipant,
                remoteAudioTrackPublication: RemoteAudioTrackPublication,
                remoteAudioTrack: RemoteAudioTrack
            ) {}

            override fun onAudioTrackSubscriptionFailed(
                remoteParticipant: RemoteParticipant,
                remoteAudioTrackPublication: RemoteAudioTrackPublication,
                twilioException: TwilioException
            ) {}

            override fun onVideoTrackPublished(
                remoteParticipant: RemoteParticipant,
                remoteVideoTrackPublication: RemoteVideoTrackPublication
            ) {}

            override fun onVideoTrackUnpublished(
                remoteParticipant: RemoteParticipant,
                remoteVideoTrackPublication: RemoteVideoTrackPublication
            ) {}

            override fun onVideoTrackSubscribed(
                remoteParticipant: RemoteParticipant,
                remoteVideoTrackPublication: RemoteVideoTrackPublication,
                remoteVideoTrack: RemoteVideoTrack
            ) {
                listener?.onRemoteVideoTrack(remoteVideoTrack)
            }

            override fun onVideoTrackUnsubscribed(
                remoteParticipant: RemoteParticipant,
                remoteVideoTrackPublication: RemoteVideoTrackPublication,
                remoteVideoTrack: RemoteVideoTrack
            ) {
                listener?.onRemoteVideoTrack(null)
            }

            override fun onVideoTrackSubscriptionFailed(
                remoteParticipant: RemoteParticipant,
                remoteVideoTrackPublication: RemoteVideoTrackPublication,
                twilioException: TwilioException
            ) {}

            override fun onDataTrackPublished(
                remoteParticipant: RemoteParticipant,
                remoteDataTrackPublication: RemoteDataTrackPublication
            ) {}

            override fun onDataTrackUnpublished(
                remoteParticipant: RemoteParticipant,
                remoteDataTrackPublication: RemoteDataTrackPublication
            ) {}

            override fun onDataTrackSubscribed(
                remoteParticipant: RemoteParticipant,
                remoteDataTrackPublication: RemoteDataTrackPublication,
                remoteDataTrack: RemoteDataTrack
            ) {}

            override fun onDataTrackUnsubscribed(
                remoteParticipant: RemoteParticipant,
                remoteDataTrackPublication: RemoteDataTrackPublication,
                remoteDataTrack: RemoteDataTrack
            ) {}

            override fun onDataTrackSubscriptionFailed(
                remoteParticipant: RemoteParticipant,
                remoteDataTrackPublication: RemoteDataTrackPublication,
                twilioException: TwilioException
            ) {}

            override fun onAudioTrackEnabled(
                remoteParticipant: RemoteParticipant,
                remoteAudioTrackPublication: RemoteAudioTrackPublication
            ) {}

            override fun onAudioTrackDisabled(
                remoteParticipant: RemoteParticipant,
                remoteAudioTrackPublication: RemoteAudioTrackPublication
            ) {}

            override fun onVideoTrackEnabled(
                remoteParticipant: RemoteParticipant,
                remoteVideoTrackPublication: RemoteVideoTrackPublication
            ) {}

            override fun onVideoTrackDisabled(
                remoteParticipant: RemoteParticipant,
                remoteVideoTrackPublication: RemoteVideoTrackPublication
            ) {}
        }

    private fun cleanup() {
        try {
            localVideoTrack?.enable(false)
        } catch (_: Exception) {
        }

        try {
            localAudioTrack?.enable(false)
        } catch (_: Exception) {
        }

        try {
            cameraCapturer?.stopCapture()
        } catch (_: Exception) {
        }

        try {
            localVideoTrack?.release()
        } catch (_: Exception) {
        }

        try {
            localAudioTrack?.release()
        } catch (_: Exception) {
        }

        listener?.onRemoteVideoTrack(null)
        listener?.onLocalVideoTrack(null)

        room = null
        localVideoTrack = null
        localAudioTrack = null
        cameraCapturer = null
        currentCameraId = null
    }

    private val roomListener =
        object : Room.Listener {
            override fun onConnected(room: Room) {
                this@TwilioVideoManager.room = room

                room.remoteParticipants.forEach { participant ->
                    wireRemoteParticipant(participant)
                }

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
                listener = null

                currentListener?.onFailed(message)
            }

            override fun onDisconnected(
                room: Room,
                twilioException: TwilioException?
            ) {
                val currentListener = listener
                val message = twilioException?.message

                cleanup()
                listener = null

                if (message != null) {
                    currentListener?.onFailed(message)
                } else {
                    currentListener?.onDisconnected()
                }
            }

            override fun onParticipantConnected(
                room: Room,
                remoteParticipant: RemoteParticipant
            ) {
                wireRemoteParticipant(remoteParticipant)
            }

            override fun onParticipantDisconnected(
                room: Room,
                remoteParticipant: RemoteParticipant
            ) {
                listener?.onRemoteVideoTrack(null)
            }

            override fun onRecordingStarted(room: Room) {}

            override fun onRecordingStopped(room: Room) {}
        }
}