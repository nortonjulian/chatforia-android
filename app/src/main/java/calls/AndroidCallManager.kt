package com.chatforia.android.calls

import android.util.Log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatforia.android.auth.UserDto
import com.chatforia.android.notifications.NotificationCoordinator
import com.chatforia.android.notifications.IncomingCallActionEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import analytics.AnalyticsManager
import analytics.AnalyticsTracker
class AndroidCallManager(
    context: Context,
    private val socketManager: CallRealtimeEvents,
    private val callService: CallBackendService,
    private val videoRepository: VideoCallBackend,
    private val voiceManager: CallAudioClient,
    private val videoManager: CallVideoClient,
    private val ringtonePlayer: CallRingtonePlayer = AudioCallRingtonePlayer(context),
    private val callDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val analytics: AnalyticsTracker = AnalyticsManager
) : ViewModel() {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val _state =
        MutableStateFlow<AndroidCallState>(AndroidCallState.Idle)

    val state: StateFlow<AndroidCallState> = _state

    private var activeCallAnalytics: CallAnalyticsContext? = null

    private val appContext = context.applicationContext
    private var incomingRingTimeoutJob: Job? = null

    @Serializable
    private data class CallEndedPayload(
        val callId: Int? = null,
        val status: String? = null,
        val reason: String? = null
    )

    init {
        observeSockets()
        observeNotificationActions()
    }

    private fun observeNotificationActions() {
        viewModelScope.launch {
            IncomingCallActionEvents
                .declinedCallIds
                .collect { declinedCallId ->
                    val ringing =
                        _state.value as?
                            AndroidCallState.Ringing
                            ?: return@collect

                    if (
                        declinedCallId != null &&
                        ringing.payload.callId !=
                            declinedCallId
                    ) {
                        return@collect
                    }

                    cancelIncomingRingTimeout()
                    ringtonePlayer.stop()
                    _state.value = AndroidCallState.Idle
                }
        }
    }

    fun startAudioCall(
        calleeId: Int,
        displayName: String = "Audio Call"
    ) {
        viewModelScope.launch(callDispatcher) {
            try {
                val callId =
                    callService.createAppCall(
                        calleeId = calleeId,
                        video = false
                    )

                val session =
                    CallSession(
                        callId = callId,
                        displayName = displayName,
                        isVideo = false,
                        continuesToVoicemailOnDecline = true
                    )

                _state.value = AndroidCallState.Connecting(session)

                val token = callService.fetchVoiceToken()

                voiceManager.startCall(
                    accessToken = token.token,
                    to = calleeId.toString(),
                    backendCallId = callId,
                    listener = object : CallAudioClient.Listener {

                        override fun onRinging() {
                            ringtonePlayer.playOutgoingRingback()
                        }

                        override fun onConnected() {
                            ringtonePlayer.stop()

                            _state.value =
                                AndroidCallState.Active(session)

                            trackCallStarted(
                                session = session,
                                callType = "audio",
                                direction = "outbound"
                            )
                        }

                        override fun onFailed(message: String) {
                            ringtonePlayer.stop()

                            viewModelScope.launch(callDispatcher) {
                                runCatching {
                                    callService.endCall(
                                        callId = callId,
                                        reason = "failed"
                                    )
                                }
                            }

                            val lower = message.lowercase()

                            if (
                                lower.contains("cancel") ||
                                lower.contains("declin") ||
                                lower.contains("disconnect") ||
                                lower.contains("busy") ||
                                lower.contains("no answer")
                            ) {
                                trackCallEnded("remote_ended")
                                _state.value =
                                    AndroidCallState.Ended()
                            } else {
                                _state.value =
                                    AndroidCallState.Failed(message)
                            }
                        }

                        override fun onDisconnected() {
                            ringtonePlayer.stop()

                            trackCallEnded("disconnected")
                            _state.value =
                                AndroidCallState.Ended()
                        }
                    }
                )

            } catch (e: Exception) {
                ringtonePlayer.stop()

                _state.value =
                    AndroidCallState.Failed(
                        e.message ?: "Failed to start audio call."
                    )
            }
        }
    }

    fun startPhoneCall(phoneNumber: String) {
        viewModelScope.launch(callDispatcher) {
            try {
                val callId =
                    callService.startExternalCall(phoneNumber)

                val session =
                    CallSession(
                        callId = callId,
                        displayName = phoneNumber,
                        isVideo = false
                    )

                _state.value = AndroidCallState.Connecting(session)

                val token = callService.fetchVoiceToken()

                voiceManager.startCall(
                    accessToken = token.token,
                    to = phoneNumber,
                    backendCallId = callId,
                    listener = object : CallAudioClient.Listener {

                        override fun onRinging() {
                            ringtonePlayer.playOutgoingRingback()
                        }

                        override fun onConnected() {
                            ringtonePlayer.stop()

                            _state.value =
                                AndroidCallState.Active(session)

                            trackCallStarted(
                                session = session,
                                callType = "phone",
                                direction = "outbound"
                            )
                        }

                        override fun onFailed(message: String) {
                            ringtonePlayer.stop()

                            viewModelScope.launch(callDispatcher) {
                                runCatching {
                                    callService.endCall(
                                        callId = callId,
                                        reason = "failed"
                                    )
                                }
                            }

                            val lower = message.lowercase()

                            if (
                                lower.contains("cancel") ||
                                lower.contains("declin") ||
                                lower.contains("disconnect") ||
                                lower.contains("busy") ||
                                lower.contains("no answer")
                            ) {
                                trackCallEnded("remote_ended")
                                _state.value =
                                    AndroidCallState.Ended()
                            } else {
                                _state.value =
                                    AndroidCallState.Failed(message)
                            }
                        }

                        override fun onDisconnected() {
                            ringtonePlayer.stop()

                            trackCallEnded("disconnected")
                            _state.value =
                                AndroidCallState.Ended()
                        }
                    }
                )

            } catch (e: Exception) {
                ringtonePlayer.stop()

                _state.value =
                    AndroidCallState.Failed(
                        e.message ?: "Failed to start phone call."
                    )
            }
        }
    }

    fun startVideoCall(
        currentUser: UserDto,
        calleeId: Int,
        displayName: String = "Video Call",
        chatRoomId: Int? = null
    ) {
        viewModelScope.launch(callDispatcher) {
            try {
                val start =
                    videoRepository.startVideo(
                        calleeId = calleeId,
                        chatRoomId = chatRoomId
                    )

                val session =
                    CallSession(
                        callId = start.callId,
                        roomName = start.roomName,
                        displayName = displayName,
                        isVideo = true,
                        speakerEnabled = true
                    )

                _state.value = AndroidCallState.Connecting(session)

                val token =
                    videoRepository.fetchVideoToken(
                        identity = currentUser.id.toString(),
                        roomName = start.roomName
                    )

                videoManager.connect(
                    accessToken = token.token,
                    roomName = start.roomName,
                    listener = object : CallVideoClient.Listener {
                        override fun onConnected() {
                            val connectedSession =
                                when (val current = _state.value) {
                                    is AndroidCallState.Connecting -> current.session
                                    is AndroidCallState.Active -> current.session
                                    else -> session
                                }

                            _state.value = AndroidCallState.Active(connectedSession)

                            trackCallStarted(
                                session = connectedSession,
                                callType = "video",
                                direction = "outbound"
                            )
                        }

                        override fun onLocalVideoTrack(track: com.twilio.video.LocalVideoTrack?) {
                            updateVideoSession {
                                it.copy(localVideoTrack = track)
                            }
                        }

                        override fun onRemoteVideoTrack(track: com.twilio.video.RemoteVideoTrack?) {
                            updateVideoSession {
                                it.copy(remoteVideoTrack = track)
                            }
                        }

                        override fun onFailed(message: String) {
                            videoManager.disconnect()

                            viewModelScope.launch(callDispatcher) {
                                runCatching {
                                    callService.endCall(
                                        callId = start.callId,
                                        reason = "failed"
                                    )
                                }
                            }

                            _state.value = AndroidCallState.Failed(message)
                        }

                        override fun onDisconnected() {
                            trackCallEnded("disconnected")
                            _state.value = AndroidCallState.Ended()
                        }
                    }
                )

            } catch (e: Exception) {
                videoManager.disconnect()

                _state.value =
                    AndroidCallState.Failed(
                        e.message ?: "Failed to start video call."
                    )
            }
        }
    }

    private fun cancelIncomingRingTimeout() {
        incomingRingTimeoutJob?.cancel()
        incomingRingTimeoutJob = null
    }

    private fun beginIncomingRinging(payload: IncomingCallPayload) {
        val currentCallId =
            when (val current = _state.value) {
                is AndroidCallState.Ringing -> current.payload.callId
                is AndroidCallState.Connecting -> current.session.callId
                is AndroidCallState.Active -> current.session.callId
                else -> null
            }

        if (payload.callId != null && payload.callId == currentCallId) {
            Log.d(
                "AndroidCallManager",
                "Ignoring duplicate incoming call event callId=${payload.callId}"
            )
            return
        }

        cancelIncomingRingTimeout()

        ringtonePlayer.playSavedRingtone()
        _state.value = AndroidCallState.Ringing(payload)

        incomingRingTimeoutJob =
            viewModelScope.launch {
                delay(40_000L)

                val ringing =
                    _state.value as? AndroidCallState.Ringing
                        ?: return@launch

                if (ringing.payload.callId != payload.callId) {
                    return@launch
                }

                Log.d(
                    "AndroidCallManager",
                    "Incoming call timed out locally callId=${payload.callId}"
                )

                ringtonePlayer.stop()

                NotificationCoordinator(appContext)
                    .cancelIncomingCallNotification()

                if (
                    payload.mode?.uppercase() == "VIDEO" ||
                    !payload.roomName.isNullOrBlank()
                ) {
                    videoManager.disconnect()
                } else {
                    voiceManager.rejectIncomingCall()
                }

                TwilioIncomingCallStore.clear()
                _state.value = AndroidCallState.Ended()
                incomingRingTimeoutJob = null
            }
    }

    fun restoreIncomingCall(payload: IncomingCallPayload) {
        beginIncomingRinging(payload)
    }


    fun acceptIncoming(currentUser: UserDto) {
        cancelIncomingRingTimeout()
        ringtonePlayer.stop()
        val ringing =
            _state.value as? AndroidCallState.Ringing ?: return

        val payload = ringing.payload

        Log.d(
            "AndroidCallManager",
            "acceptIncoming callId=${payload.callId} mode='${payload.mode}' roomName='${payload.roomName}'"
        )

        if (payload.mode?.uppercase() == "VIDEO" || !payload.roomName.isNullOrBlank()) {
            acceptIncomingVideo(currentUser, payload)
        } else {
            acceptIncomingAudio(payload)
        }
    }

    private fun acceptIncomingAudio(payload: IncomingCallPayload) {
        val session =
            CallSession(
                callId = payload.callId,
                displayName =
                    payload.callerName
                        ?: payload.fromNumber
                        ?: "Incoming Call",
                isVideo = false
            )

        _state.value = AndroidCallState.Connecting(session)

        val listener =
            object : CallAudioClient.Listener {
                override fun onConnected() {
                    _state.value = AndroidCallState.Active(session)

                    trackCallStarted(
                        session = session,
                        callType = "audio",
                        direction = "inbound"
                    )
                }

                override fun onFailed(message: String) {
                    _state.value = AndroidCallState.Failed(message)
                }

                override fun onDisconnected() {
                    trackCallEnded("disconnected")
                    _state.value = AndroidCallState.Ended()
                }
            }

        viewModelScope.launch {
            repeat(60) { attempt ->
                val currentState = _state.value

                if (
                    currentState !is AndroidCallState.Connecting ||
                    currentState.session.callId != session.callId
                ) {
                    return@launch
                }

                if (voiceManager.acceptCall(listener)) {
                    Log.d(
                        "AndroidCallManager",
                        "Twilio invite accepted after ${attempt * 100}ms"
                    )
                    return@launch
                }

                if (attempt == 0) {
                    Log.d(
                        "AndroidCallManager",
                        "Waiting for Twilio invite for callId=${payload.callId}"
                    )
                }

                delay(100)
            }

            val currentState = _state.value
            if (
                currentState is AndroidCallState.Connecting &&
                currentState.session.callId == session.callId
            ) {
                _state.value =
                    AndroidCallState.Failed(
                        "The incoming voice invitation did not arrive."
                    )
            }
        }
    }

    private fun acceptIncomingVideo(
        currentUser: UserDto,
        payload: IncomingCallPayload
    ) {
        viewModelScope.launch(callDispatcher) {
            try {
                val roomName =
                    payload.roomName
                        ?: throw Exception("Missing video room.")

                val token =
                    videoRepository.fetchVideoToken(
                        identity = currentUser.id.toString(),
                        roomName = roomName
                    )

                val session =
                    CallSession(
                        callId = payload.callId,
                        roomName = roomName,
                        displayName = payload.callerName ?: "Video Call",
                        isVideo = true,
                        speakerEnabled = true
                    )

                _state.value = AndroidCallState.Connecting(session)

                videoManager.connect(
                    accessToken = token.token,
                    roomName = roomName,
                    listener = object : CallVideoClient.Listener {
                        override fun onConnected() {
                            val connectedSession =
                                when (val current = _state.value) {
                                    is AndroidCallState.Connecting -> current.session
                                    is AndroidCallState.Active -> current.session
                                    else -> session
                                }

                            _state.value = AndroidCallState.Active(connectedSession)

                            trackCallStarted(
                                session = connectedSession,
                                callType = "video",
                                direction = "inbound"
                            )
                        }

                        override fun onLocalVideoTrack(track: com.twilio.video.LocalVideoTrack?) {
                            updateVideoSession {
                                it.copy(localVideoTrack = track)
                            }
                        }

                        override fun onRemoteVideoTrack(track: com.twilio.video.RemoteVideoTrack?) {
                            updateVideoSession {
                                it.copy(remoteVideoTrack = track)
                            }
                        }

                        override fun onFailed(message: String) {
                            videoManager.disconnect()

                            payload.callId?.let { callId ->
                                viewModelScope.launch(callDispatcher) {
                                    runCatching {
                                        callService.endCall(
                                            callId = callId,
                                            reason = "failed"
                                        )
                                    }
                                }
                            }

                            _state.value = AndroidCallState.Failed(message)
                        }

                        override fun onDisconnected() {
                            trackCallEnded("disconnected")
                            _state.value = AndroidCallState.Ended()
                        }
                    }
                )

            } catch (e: Exception) {
                videoManager.disconnect()

                payload.callId?.let { callId ->
                    viewModelScope.launch(callDispatcher) {
                        runCatching {
                            callService.endCall(
                                callId = callId,
                                reason = "failed"
                            )
                        }
                    }
                }

                _state.value =
                    AndroidCallState.Failed(
                        e.message ?: "Failed to accept video call."
                    )
            }
        }
    }

    fun declineIncoming() {
        cancelIncomingRingTimeout()
        ringtonePlayer.stop()

        val ringing =
            _state.value as? AndroidCallState.Ringing ?: return

        val payload = ringing.payload
        val callId = payload.callId
        val isVideo =
            payload.mode?.uppercase() == "VIDEO" ||
                    !payload.roomName.isNullOrBlank()

        // Dismiss the incoming-call interface immediately.
        _state.value = AndroidCallState.Idle

        viewModelScope.launch(callDispatcher) {
            // Persist the explicit decline before rejecting the Twilio leg.
            // This lets the completion webhook see DECLINED and prevents
            // it from routing the caller into voicemail.
            if (callId != null) {
                runCatching {
                    callService.endCall(
                        callId = callId,
                        reason = "declined"
                    )
                }
            }

            withContext(Dispatchers.Main.immediate) {
                if (isVideo) {
                    videoManager.disconnect()
                } else {
                    voiceManager.rejectIncomingCall()
                }
            }
        }
    }

    fun toggleMute() {
        val active =
            _state.value as? AndroidCallState.Active ?: return

        val updated =
            active.session.copy(
                muted = !active.session.muted
            )

        if (updated.isVideo) {
            videoManager.setMuted(updated.muted)
        } else {
            voiceManager.setMuted(updated.muted)
        }

        _state.value = AndroidCallState.Active(updated)
    }

    fun sendDigit(digit: String) {
        val active =
            _state.value as? AndroidCallState.Active ?: return

        if (active.session.isVideo) return

        val allowedDigits =
            setOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "#")

        if (digit !in allowedDigits) return

        voiceManager.sendDigits(digit)
    }

    fun toggleSpeaker() {
        val active =
            _state.value as? AndroidCallState.Active ?: return

        val updated =
            active.session.copy(
                speakerEnabled = !active.session.speakerEnabled
            )

        voiceManager.setSpeaker(updated.speakerEnabled)

        _state.value = AndroidCallState.Active(updated)
    }

    fun toggleCamera() {
        val active =
            _state.value as? AndroidCallState.Active ?: return

        if (!active.session.isVideo) return

        val updated =
            active.session.copy(
                cameraEnabled = !active.session.cameraEnabled
            )

        videoManager.setCameraEnabled(updated.cameraEnabled)

        _state.value = AndroidCallState.Active(updated)
    }

    fun flipCamera() {
        videoManager.flipCamera()
    }

    fun endCall() {
        cancelIncomingRingTimeout()
        ringtonePlayer.stop()

        val current = _state.value

        val session =
            when (current) {
                is AndroidCallState.Active -> current.session
                is AndroidCallState.Connecting -> current.session
                else -> null
            }

        val callId = session?.callId
        val isVideo = session?.isVideo == true

        trackCallEnded("hangup")

        _state.value = AndroidCallState.Ended()

        if (isVideo) {
            videoManager.disconnect()
        } else {
            voiceManager.endCall()
        }

        if (callId != null) {
            viewModelScope.launch(callDispatcher) {
                runCatching {
                    callService.endCall(
                        callId = callId,
                        reason = "hangup"
                    )
                }
            }
        }
    }

    fun clearEndedState() {
        _state.value = AndroidCallState.Idle
    }


    private data class CallAnalyticsContext(
        val callType: String,
        val direction: String,
        val startedAtMillis: Long
    )

    private fun updateVideoSession(
        update: (CallSession) -> CallSession
    ) {
        val current = _state.value

        _state.value =
            when (current) {
                is AndroidCallState.Connecting ->
                    AndroidCallState.Connecting(update(current.session))

                is AndroidCallState.Active ->
                    AndroidCallState.Active(update(current.session))

                else -> current
            }
    }

    private fun trackCallStarted(
        session: CallSession,
        callType: String,
        direction: String
    ) {
        if (activeCallAnalytics != null) return

        activeCallAnalytics =
            CallAnalyticsContext(
                callType = callType,
                direction = direction,
                startedAtMillis = System.currentTimeMillis()
            )

        analytics.capture(
            "call started",
            mapOf(
                "call_type" to callType,
                "direction" to direction
            )
        )
    }

    private fun trackCallEnded(reason: String) {
        val current = activeCallAnalytics ?: return

        val durationSec =
            ((System.currentTimeMillis() - current.startedAtMillis) / 1000)
                .toInt()
                .coerceAtLeast(0)

        analytics.capture(
            "call ended",
            mapOf(
                "call_type" to current.callType,
                "direction" to current.direction,
                "ended_reason" to reason,
                "duration_bucket" to durationBucket(durationSec)
            )
        )

        activeCallAnalytics = null
    }

    private fun durationBucket(durationSec: Int): String {
        return when {
            durationSec < 10 -> "0-9s"
            durationSec < 60 -> "10-59s"
            durationSec < 300 -> "1-5m"
            durationSec < 900 -> "5-15m"
            else -> "15m+"
        }
    }
    private fun observeSockets() {
        viewModelScope.launch {
            socketManager.incomingCalls.collect { raw ->
                val payload =
                    runCatching {
                        json.decodeFromString<IncomingCallPayload>(raw)
                    }.getOrNull()

                if (payload != null) {
                    beginIncomingRinging(payload)
                }
            }
        }

        viewModelScope.launch {
            TwilioVoiceCallEvents.remoteEnded.collect {
                cancelIncomingRingTimeout()
                ringtonePlayer.stop()
                voiceManager.endCall()
                videoManager.disconnect()
                trackCallEnded("remote_ended")
                _state.value = AndroidCallState.Ended()
            }
        }

        viewModelScope.launch {
            socketManager.incomingVideoCalls.collect { raw ->
                val payload =
                    runCatching {
                        json.decodeFromString<IncomingCallPayload>(raw)
                    }.getOrNull()

                if (payload != null) {
                    beginIncomingRinging(
                        payload.copy(mode = payload.mode ?: "VIDEO")
                    )
                }
            }
        }

        viewModelScope.launch {
            socketManager.callEnded.collect { raw ->
                val payload =
                    runCatching {
                        json.decodeFromString<CallEndedPayload>(
                            raw
                        )
                    }.getOrNull()

                val currentSession =
                    when (val current = _state.value) {
                        is AndroidCallState.Connecting ->
                            current.session

                        is AndroidCallState.Active ->
                            current.session

                        else -> null
                    }

                val shouldContinueCallerToVoicemail =
                    payload?.status?.uppercase() == "DECLINED" &&
                        payload.callId != null &&
                        currentSession?.callId == payload.callId &&
                        currentSession.isVideo == false &&
                        currentSession.continuesToVoicemailOnDecline

                if (shouldContinueCallerToVoicemail) {
                    ringtonePlayer.stop()

                    Log.d(
                        "AndroidCallManager",
                        "Preserving outgoing audio call " +
                            "${currentSession.callId} after decline " +
                            "so voicemail can continue"
                    )

                    return@collect
                }

                cancelIncomingRingTimeout()
                ringtonePlayer.stop()
                voiceManager.endCall()
                videoManager.disconnect()
                trackCallEnded("remote_ended")
                _state.value = AndroidCallState.Ended()
            }
        }

        viewModelScope.launch {
            socketManager.videoCallEnded.collect {
                cancelIncomingRingTimeout()
                ringtonePlayer.stop()
                videoManager.disconnect()
                trackCallEnded("remote_ended")
                _state.value = AndroidCallState.Ended()
            }
        }
    }
}