package com.chatforia.android.calls

import android.util.Log

import androidx.lifecycle.ViewModel
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.chatforia.android.auth.UserDto
import com.chatforia.android.notifications.NotificationCoordinator
import com.chatforia.android.notifications.IncomingCallActionEvents
import com.chatforia.android.network.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import android.content.Context
import android.content.pm.PackageManager
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
    private val analytics: AnalyticsTracker = AnalyticsManager,
    private val permissionChecker: (Context, String) -> Boolean =
        { permissionContext, permission ->
            ContextCompat.checkSelfPermission(
                permissionContext,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
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
    private var outgoingAudioAnswerWatchJob: Job? = null

    private sealed class PendingOutgoingCall {
        data class Audio(
            val calleeId: Int,
            val displayName: String
        ) : PendingOutgoingCall()

        data class Phone(
            val phoneNumber: String
        ) : PendingOutgoingCall()

        data class Video(
            val currentUser: UserDto,
            val calleeId: Int,
            val displayName: String,
            val chatRoomId: Int?
        ) : PendingOutgoingCall()
    }

    private var pendingOutgoingCall: PendingOutgoingCall? = null
    private var outgoingAudioIntentPending = false

    private val outgoingPermissionRequestChannel =
        Channel<Array<String>>(capacity = Channel.BUFFERED)

    val outgoingPermissionRequests: Flow<Array<String>> =
        outgoingPermissionRequestChannel.receiveAsFlow()

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

    private fun startOutgoingAudioAnswerWatch(
        session: CallSession
    ) {
        val callId = session.callId ?: return

        outgoingAudioAnswerWatchJob?.cancel()

        outgoingAudioAnswerWatchJob =
            viewModelScope.launch(callDispatcher) {
                repeat(80) {
                    val current =
                        _state.value as?
                            AndroidCallState.Connecting
                            ?: return@launch

                    if (current.session.callId != callId) {
                        return@launch
                    }

                    val status =
                        runCatching {
                            callService
                                .fetchCallStatus(callId)
                                .call
                                .status
                                ?.uppercase()
                        }.getOrNull()

                    when (status) {
                        "ACTIVE" -> {
                            val latest =
                                _state.value as?
                                    AndroidCallState.Connecting
                                    ?: return@launch

                            if (latest.session.callId != callId) {
                                return@launch
                            }

                            ringtonePlayer.stop()

                            _state.value =
                                AndroidCallState.Active(
                                    latest.session
                                )

                            trackCallStarted(
                                session = latest.session,
                                callType = "audio",
                                direction = "outbound"
                            )

                            return@launch
                        }

                        "DECLINED",
                        "MISSED",
                        "FAILED",
                        "ENDED" -> {
                            return@launch
                        }
                    }

                    delay(250)
                }
            }
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

    private fun missingPermissions(
        requiredPermissions: Array<String>
    ): Array<String> {
        return requiredPermissions
            .filterNot { permission ->
                permissionChecker(appContext, permission)
            }
            .toTypedArray()
    }

    private fun queuePermissionRequestIfNeeded(
        request: PendingOutgoingCall,
        requiredPermissions: Array<String>
    ): Boolean {
        val missing = missingPermissions(requiredPermissions)

        if (missing.isEmpty()) {
            return false
        }

        pendingOutgoingCall = request

        val result =
            outgoingPermissionRequestChannel.trySend(missing)

        if (result.isFailure) {
            pendingOutgoingCall = null

            _state.value =
                AndroidCallState.Failed(
                    "Unable to request call permissions."
                )
        }

        return true
    }

    fun completeOutgoingPermissionRequest(
        granted: Boolean
    ) {
        val pending = pendingOutgoingCall ?: return
        pendingOutgoingCall = null

        if (!granted) {
            if (pending is PendingOutgoingCall.Audio) {
                outgoingAudioIntentPending = false
            }

            _state.value = AndroidCallState.Idle
            return
        }

        when (pending) {
            is PendingOutgoingCall.Audio ->
                startAudioCallAfterPermission(pending)

            is PendingOutgoingCall.Phone ->
                startPhoneCallAfterPermission(pending)

            is PendingOutgoingCall.Video ->
                startVideoCallAfterPermission(pending)
        }
    }

    fun startAudioCall(
        calleeId: Int,
        displayName: String = "Audio Call"
    ) {
        if (outgoingAudioIntentPending) {
            Log.d(
                "AndroidCallManager",
                "Ignoring duplicate outgoing audio call tap"
            )
            return
        }

        outgoingAudioIntentPending = true

        val request =
            PendingOutgoingCall.Audio(
                calleeId = calleeId,
                displayName = displayName
            )

        if (
            queuePermissionRequestIfNeeded(
                request = request,
                requiredPermissions =
                    CallPermissionHelper.audioPermissions()
            )
        ) {
            if (pendingOutgoingCall == null) {
                outgoingAudioIntentPending = false
            }
            return
        }

        startAudioCallAfterPermission(request)
    }

    private fun startAudioCallAfterPermission(
        request: PendingOutgoingCall.Audio
    ) {
        viewModelScope.launch(callDispatcher) {
            var createdCallId: Int? = null

            try {
                val callId =
                    callService.createAppCall(
                        calleeId = request.calleeId,
                        video = false
                    )

                createdCallId = callId

                val session =
                    CallSession(
                        callId = callId,
                        displayName = request.displayName,
                        isVideo = false,
                        isOutgoing = true,
                        continuesToVoicemailOnDecline = false
                    )

                _state.value =
                    AndroidCallState.Connecting(session)

                outgoingAudioIntentPending = false

                val token = callService.fetchVoiceToken()

                voiceManager.startCall(
                    accessToken = token.token,
                    to = request.calleeId.toString(),
                    backendCallId = callId,
                    listener =
                        object : CallAudioClient.Listener {

                            override fun onRinging() {
                                if (!isCurrentSession(callId)) {
                                    Log.d(
                                        "AndroidCallManager",
                                        "Ignoring stale outgoing onRinging " +
                                            "callId=$callId"
                                    )
                                    return
                                }

                                ringtonePlayer
                                    .playOutgoingRingback()
                            }

                            override fun onConnected() {
                                if (!isCurrentSession(callId)) {
                                    Log.d(
                                        "AndroidCallManager",
                                        "Ignoring stale outgoing onConnected " +
                                            "callId=$callId"
                                    )
                                    return
                                }

                                Log.d(
                                    "AndroidCallManager",
                                    "Outgoing app audio media is ready; " +
                                        "waiting for backend ACTIVE."
                                )

                                startOutgoingAudioAnswerWatch(
                                    session
                                )
                            }

                            override fun onFailed(
                                message: String
                            ) {
                                if (!isCurrentSession(callId)) {
                                    Log.d(
                                        "AndroidCallManager",
                                        "Ignoring stale outgoing onFailed " +
                                            "callId=$callId message=$message"
                                    )
                                    return
                                }

                                ringtonePlayer.stop()

                                viewModelScope.launch(
                                    callDispatcher
                                ) {
                                    runCatching {
                                        callService.endCall(
                                            callId = callId,
                                            reason = "failed"
                                        )
                                    }
                                }

                                val lower =
                                    message.lowercase()

                                if (
                                    lower.contains("cancel") ||
                                    lower.contains("declin") ||
                                    lower.contains(
                                        "disconnect"
                                    ) ||
                                    lower.contains("busy") ||
                                    lower.contains(
                                        "no answer"
                                    )
                                ) {
                                    trackCallEnded(
                                        "remote_ended"
                                    )

                                    _state.value =
                                        AndroidCallState.Ended()
                                } else {
                                    _state.value =
                                        AndroidCallState.Failed(
                                            message
                                        )
                                }
                            }

                            override fun onDisconnected() {
                                if (!isCurrentSession(callId)) {
                                    Log.d(
                                        "AndroidCallManager",
                                        "Ignoring stale outgoing " +
                                            "onDisconnected callId=$callId"
                                    )
                                    return
                                }

                                ringtonePlayer.stop()

                                trackCallEnded(
                                    "disconnected"
                                )

                                _state.value =
                                    AndroidCallState.Ended()
                            }
                        }
                )
            } catch (e: Exception) {
                outgoingAudioIntentPending = false
                ringtonePlayer.stop()

                if (
                    e is ApiException &&
                    e.statusCode == 409
                ) {
                    Log.d(
                        "AndroidCallManager",
                        "Outgoing cross-call lost backend arbitration; " +
                            "waiting for canonical incoming call."
                    )

                    outgoingAudioAnswerWatchJob?.cancel()
                    outgoingAudioAnswerWatchJob = null

                    /*
                     * Disconnect only a partial outgoing Twilio leg.
                     * Do not clear the stored incoming invitation.
                     */
                    voiceManager.disconnectActiveCall()

                    /*
                     * Preserve the canonical incoming state if its event
                     * arrived before the 409. Otherwise, wait in Idle for
                     * the incoming socket, FCM, or Twilio event.
                     */
                    if (
                        _state.value !is
                            AndroidCallState.Ringing
                    ) {
                        _state.value =
                            AndroidCallState.Idle
                    }

                    return@launch
                }

                createdCallId?.let { callId ->
                    runCatching {
                        callService.endCall(
                            callId = callId,
                            reason = "failed"
                        )
                    }
                }

                _state.value =
                    AndroidCallState.Failed(
                        e.message
                            ?: "Failed to start audio call."
                    )
            }
        }
    }

    fun startPhoneCall(
        phoneNumber: String
    ) {
        val request =
            PendingOutgoingCall.Phone(
                phoneNumber = phoneNumber
            )

        if (
            queuePermissionRequestIfNeeded(
                request = request,
                requiredPermissions =
                    CallPermissionHelper.audioPermissions()
            )
        ) {
            return
        }

        startPhoneCallAfterPermission(request)
    }

    private fun startPhoneCallAfterPermission(
        request: PendingOutgoingCall.Phone
    ) {
        viewModelScope.launch(callDispatcher) {
            var createdCallId: Int? = null

            try {
                val callId =
                    callService.startExternalCall(
                        request.phoneNumber
                    )

                createdCallId = callId

                val session =
                    CallSession(
                        callId = callId,
                        displayName = request.phoneNumber,
                        isVideo = false,
                        isOutgoing = true
                    )

                _state.value =
                    AndroidCallState.Connecting(session)

                val token = callService.fetchVoiceToken()

                voiceManager.startCall(
                    accessToken = token.token,
                    to = request.phoneNumber,
                    backendCallId = callId,
                    listener =
                        object : CallAudioClient.Listener {

                            override fun onRinging() {
                                ringtonePlayer
                                    .playOutgoingRingback()
                            }

                            override fun onConnected() {
                                ringtonePlayer.stop()

                                _state.value =
                                    AndroidCallState.Active(
                                        session
                                    )

                                trackCallStarted(
                                    session = session,
                                    callType = "phone",
                                    direction = "outbound"
                                )
                            }

                            override fun onFailed(
                                message: String
                            ) {
                                ringtonePlayer.stop()

                                viewModelScope.launch(
                                    callDispatcher
                                ) {
                                    runCatching {
                                        callService.endCall(
                                            callId = callId,
                                            reason = "failed"
                                        )
                                    }
                                }

                                val lower =
                                    message.lowercase()

                                if (
                                    lower.contains("cancel") ||
                                    lower.contains("declin") ||
                                    lower.contains(
                                        "disconnect"
                                    ) ||
                                    lower.contains("busy") ||
                                    lower.contains(
                                        "no answer"
                                    )
                                ) {
                                    trackCallEnded(
                                        "remote_ended"
                                    )

                                    _state.value =
                                        AndroidCallState.Ended()
                                } else {
                                    _state.value =
                                        AndroidCallState.Failed(
                                            message
                                        )
                                }
                            }

                            override fun onDisconnected() {
                                ringtonePlayer.stop()

                                trackCallEnded(
                                    "disconnected"
                                )

                                _state.value =
                                    AndroidCallState.Ended()
                            }
                        }
                )
            } catch (e: Exception) {
                ringtonePlayer.stop()

                createdCallId?.let { callId ->
                    runCatching {
                        callService.endCall(
                            callId = callId,
                            reason = "failed"
                        )
                    }
                }

                _state.value =
                    AndroidCallState.Failed(
                        e.message
                            ?: "Failed to start phone call."
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
        val request =
            PendingOutgoingCall.Video(
                currentUser = currentUser,
                calleeId = calleeId,
                displayName = displayName,
                chatRoomId = chatRoomId
            )

        if (
            queuePermissionRequestIfNeeded(
                request = request,
                requiredPermissions =
                    CallPermissionHelper.videoPermissions()
            )
        ) {
            return
        }

        startVideoCallAfterPermission(request)
    }

    private fun startVideoCallAfterPermission(
        request: PendingOutgoingCall.Video
    ) {
        viewModelScope.launch(callDispatcher) {
            var createdCallId: Int? = null

            try {
                val callId =
                    callService.createAppCall(
                        calleeId = request.calleeId,
                        video = true
                    )

                createdCallId = callId

                val roomName = "call_$callId"

                val session =
                    CallSession(
                        callId = callId,
                        roomName = roomName,
                        displayName = request.displayName,
                        isVideo = true,
                        isOutgoing = true,
                        speakerEnabled = true
                    )

                _state.value =
                    AndroidCallState.Connecting(session)

                val token =
                    videoRepository.fetchVideoToken(
                        identity =
                            request.currentUser.id.toString(),
                        roomName = roomName
                    )

                videoManager.connect(
                    accessToken = token.token,
                    roomName = roomName,
                    listener =
                        object : CallVideoClient.Listener {

                            override fun onConnected() {
                                // This device has joined the Twilio room,
                                // but the recipient has not necessarily
                                // answered yet. Remain Connecting.
                                Log.d(
                                    "AndroidCallManager",
                                    "Outgoing video media is ready; " +
                                        "waiting for remote participant."
                                )
                            }

                            override fun onRemoteParticipantConnected() {
                                val current = _state.value

                                if (
                                    current is AndroidCallState.Active
                                ) {
                                    return
                                }

                                val connectedSession =
                                    when (current) {
                                        is AndroidCallState.Connecting ->
                                            current.session

                                        else -> return
                                    }

                                _state.value =
                                    AndroidCallState.Active(
                                        connectedSession
                                    )

                                trackCallStarted(
                                    session = connectedSession,
                                    callType = "video",
                                    direction = "outbound"
                                )
                            }

                            override fun onLocalVideoTrack(
                                track:
                                    com.twilio.video
                                        .LocalVideoTrack?
                            ) {
                                updateVideoSession {
                                    it.copy(
                                        localVideoTrack = track
                                    )
                                }
                            }

                            override fun onRemoteVideoTrack(
                                track:
                                    com.twilio.video
                                        .RemoteVideoTrack?
                            ) {
                                updateVideoSession {
                                    it.copy(
                                        remoteVideoTrack = track
                                    )
                                }
                            }

                            override fun onFailed(
                                message: String
                            ) {
                                videoManager.disconnect()

                                viewModelScope.launch(
                                    callDispatcher
                                ) {
                                    runCatching {
                                        callService.endCall(
                                            callId =
                                                callId,
                                            reason = "failed"
                                        )
                                    }
                                }

                                _state.value =
                                    AndroidCallState.Failed(
                                        message
                                    )
                            }

                            override fun onDisconnected() {
                                trackCallEnded(
                                    "disconnected"
                                )

                                _state.value =
                                    AndroidCallState.Ended()
                            }
                        }
                )
            } catch (e: Exception) {
                videoManager.disconnect()

                if (
                    e is ApiException &&
                    e.statusCode == 409
                ) {
                    Log.d(
                        "AndroidCallManager",
                        "Outgoing video glare lost backend arbitration; " +
                            "waiting for canonical incoming call."
                    )

                    if (
                        _state.value !is
                            AndroidCallState.Ringing
                    ) {
                        _state.value =
                            AndroidCallState.Idle
                    }

                    return@launch
                }

                createdCallId?.let { callId ->
                    runCatching {
                        callService.endCall(
                            callId = callId,
                            reason = "failed"
                        )
                    }
                }

                _state.value =
                    AndroidCallState.Failed(
                        e.message
                            ?: "Failed to start video call."
                    )
            }
        }
    }

    private fun cancelIncomingRingTimeout() {
        incomingRingTimeoutJob?.cancel()
        incomingRingTimeoutJob = null
    }

    private fun isCurrentSession(callId: Int): Boolean {
        return when (val current = _state.value) {
            is AndroidCallState.Connecting ->
                current.session.callId == callId

            is AndroidCallState.Active ->
                current.session.callId == callId

            else -> false
        }
    }

    private fun terminateOutgoingForIncomingCollision(
        outgoingSession: CallSession,
        incomingCallId: Int?
    ) {
        val outgoingCallId = outgoingSession.callId

        Log.d(
            "AndroidCallManager",
            "Cross-call collision: cancelling outgoing callId=" +
                "$outgoingCallId before showing incoming callId=" +
                "$incomingCallId"
        )

        outgoingAudioAnswerWatchJob?.cancel()
        outgoingAudioAnswerWatchJob = null
        ringtonePlayer.stop()

        /*
         * Disconnect only the active outgoing Twilio leg.
         * Do not clear TwilioIncomingCallStore because the competing
         * incoming invitation may already have arrived.
         */
        voiceManager.disconnectActiveCall()

        if (outgoingCallId != null) {
            viewModelScope.launch(callDispatcher) {
                runCatching {
                    callService.endCall(
                        callId = outgoingCallId,
                        reason = "caller_canceled"
                    )
                }.onSuccess {
                    CallLifecyclePushEvents
                        .notifyHistoryRefresh()
                }.onFailure { error ->
                    Log.w(
                        "AndroidCallManager",
                        "Failed to finalize losing cross-call " +
                            "callId=$outgoingCallId",
                        error
                    )
                }
            }
        }
    }

    private fun beginIncomingRinging(payload: IncomingCallPayload) {
        val currentState = _state.value

        val currentCallId =
            when (currentState) {
                is AndroidCallState.Ringing ->
                    currentState.payload.callId

                is AndroidCallState.Connecting ->
                    currentState.session.callId

                is AndroidCallState.Active ->
                    currentState.session.callId

                else -> null
            }

        if (payload.callId != null && payload.callId == currentCallId) {
            Log.d(
                "AndroidCallManager",
                "Ignoring duplicate incoming call event callId=${payload.callId}"
            )
            return
        }

        val outgoingCollisionSession =
            (currentState as? AndroidCallState.Connecting)
                ?.session
                ?.takeIf { session ->
                    session.isOutgoing &&
                        session.callId != payload.callId
                }

        cancelIncomingRingTimeout()

        /*
         * Change the visible state first. A late callback from the
         * losing outgoing Twilio leg must not be allowed to replace
         * this incoming ringing state.
         */
        _state.value = AndroidCallState.Ringing(payload)

        outgoingCollisionSession?.let { session ->
            terminateOutgoingForIncomingCollision(
                outgoingSession = session,
                incomingCallId = payload.callId
            )
        }

        ringtonePlayer.playSavedRingtone()

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
                    _state.value =
                        AndroidCallState.Active(session)

                    payload.callId?.let { callId ->
                        viewModelScope.launch(
                            callDispatcher
                        ) {
                            runCatching {
                                callService.markCallActive(
                                    callId
                                )
                            }.onFailure { error ->
                                Log.w(
                                    "AndroidCallManager",
                                    "Failed to mark accepted audio call active",
                                    error
                                )
                            }
                        }
                    }

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

                    payload.callId?.let { callId ->
                        viewModelScope.launch(
                            callDispatcher
                        ) {
                            runCatching {
                                callService.markCallActive(
                                    callId
                                )
                            }.onFailure { error ->
                                Log.w(
                                    "AndroidCallManager",
                                    "Failed to mark accepted audio call active",
                                    error
                                )
                            }
                        }
                    }

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

                            payload.callId?.let { callId ->
                                viewModelScope.launch(callDispatcher) {
                                    runCatching {
                                        callService.markCallActive(callId)
                                    }.onFailure { error ->
                                        Log.w(
                                            "AndroidCallManager",
                                            "Failed to mark accepted video call active",
                                            error
                                        )
                                    }
                                }
                            }

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

        val callerCanceledBeforeAnswer =
            current is AndroidCallState.Connecting &&
                session?.isOutgoing == true

        val endReason =
            if (callerCanceledBeforeAnswer) {
                "caller_canceled"
            } else {
                "hangup"
            }

        trackCallEnded(endReason)

        _state.value =
            AndroidCallState.Ended(
                reason = endReason
            )

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
                        reason = endReason
                    )
                }.onSuccess {
                    CallLifecyclePushEvents
                        .notifyHistoryRefresh()
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
            CallLifecyclePushEvents
                .terminalEvents
                .collect { event ->
                    val currentState = _state.value

                    val currentSession =
                        when (currentState) {
                            is AndroidCallState.Connecting ->
                                currentState.session

                            is AndroidCallState.Active ->
                                currentState.session

                            else -> null
                        }

                    val ringingCallId =
                        (
                            currentState
                                as? AndroidCallState.Ringing
                        )
                            ?.payload
                            ?.callId

                    val currentCallId =
                        currentSession?.callId
                            ?: ringingCallId

                    if (
                        event.callId == null ||
                        currentCallId == null ||
                        event.callId != currentCallId
                    ) {
                        return@collect
                    }

                    cancelIncomingRingTimeout()
                    ringtonePlayer.stop()
                    voiceManager.endCall()
                    videoManager.disconnect()

                    trackCallEnded(
                        event.reason ?: "remote_ended"
                    )

                    _state.value =
                        AndroidCallState.Ended(
                            reason =
                                event.reason
                                    ?: "remote_ended"
                        )
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

                val currentCallId =
                    when (val current = _state.value) {
                        is AndroidCallState.Connecting ->
                            current.session.callId

                        is AndroidCallState.Active ->
                            current.session.callId

                        is AndroidCallState.Ringing ->
                            current.payload.callId

                        else -> null
                    }

                val endedCallId = payload?.callId

                if (
                    endedCallId == null ||
                    currentCallId == null ||
                    endedCallId != currentCallId
                ) {
                    Log.d(
                        "AndroidCallManager",
                        "Ignoring call:ended for non-current call: " +
                            "endedCallId=$endedCallId " +
                            "currentCallId=$currentCallId"
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