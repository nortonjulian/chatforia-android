package com.chatforia.android.calls

import analytics.FakeAnalyticsTracker
import com.chatforia.android.auth.UserDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AndroidCallManagerAnalyticsTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun outgoingAudioCall_whenConnected_capturesCallStarted() = runTest(testDispatcher) {
        val analytics = FakeAnalyticsTracker()
        val audioClient = FakeCallAudioClient()

        val manager =
            createManager(
                analytics = analytics,
                audioClient = audioClient
            )

        manager.startAudioCall(
            calleeId = 123,
            displayName = "Test Call"
        )

        advanceUntilIdle()

        audioClient.lastListener?.onConnected()

        assertEquals(
            "call started",
            analytics.events.first().name
        )

        assertEquals(
            "audio",
            analytics.events.first().properties["call_type"]
        )

        assertEquals(
            "outbound",
            analytics.events.first().properties["direction"]
        )

        assertTrue(
            manager.state.value is AndroidCallState.Active
        )
    }

    @Test
    fun outgoingAudioCall_whenHungUp_capturesCallEnded() = runTest(testDispatcher) {
        val analytics = FakeAnalyticsTracker()
        val audioClient = FakeCallAudioClient()

        val manager =
            createManager(
                analytics = analytics,
                audioClient = audioClient
            )

        manager.startAudioCall(
            calleeId = 123,
            displayName = "Test Call"
        )

        advanceUntilIdle()

        audioClient.lastListener?.onConnected()

        manager.endCall()

        advanceUntilIdle()

        val endedEvent =
            analytics.events.first { event ->
                event.name == "call ended"
            }

        assertEquals(
            "audio",
            endedEvent.properties["call_type"]
        )

        assertEquals(
            "outbound",
            endedEvent.properties["direction"]
        )

        assertEquals(
            "hangup",
            endedEvent.properties["ended_reason"]
        )

        assertTrue(
            endedEvent.properties.containsKey("duration_bucket")
        )
    }

    @Test
    fun incomingAudioCall_whenAccepted_capturesInboundCallStarted() = runTest(testDispatcher) {
        val analytics = FakeAnalyticsTracker()
        val audioClient =
            FakeCallAudioClient(
                acceptCallResult = true
            )

        val manager =
            createManager(
                analytics = analytics,
                audioClient = audioClient
            )

        manager.restoreIncomingCall(
            IncomingCallPayload(
                callId = 555,
                callerId = 321,
                callerName = "Do Not Track This",
                fromNumber = "+15555555555",
                mode = "AUDIO"
            )
        )

        manager.acceptIncoming(
            UserDto(id = 999)
        )

        val startedEvent =
            analytics.events.first { event ->
                event.name == "call started"
            }

        assertEquals(
            "audio",
            startedEvent.properties["call_type"]
        )

        assertEquals(
            "inbound",
            startedEvent.properties["direction"]
        )

        assertTrue(
            manager.state.value is AndroidCallState.Active
        )
    }

    @Test
    fun activeAudioCall_whenRemoteEnded_capturesRemoteEndedReason() = runTest(testDispatcher) {
        val analytics = FakeAnalyticsTracker()
        val socketEvents = FakeCallRealtimeEvents()
        val audioClient = FakeCallAudioClient()

        val manager =
            createManager(
                analytics = analytics,
                socketEvents = socketEvents,
                audioClient = audioClient
            )

        advanceUntilIdle()

        manager.startAudioCall(
            calleeId = 123,
            displayName = "Test Call"
        )

        advanceUntilIdle()

        audioClient.lastListener?.onConnected()

        socketEvents.callEndedFlow.emit("{}")

        advanceUntilIdle()

        val endedEvent =
            analytics.events.first { event ->
                event.name == "call ended"
            }

        assertEquals(
            "remote_ended",
            endedEvent.properties["ended_reason"]
        )

        assertEquals(
            "audio",
            endedEvent.properties["call_type"]
        )
    }

    private fun createManager(
        analytics: FakeAnalyticsTracker,
        socketEvents: FakeCallRealtimeEvents = FakeCallRealtimeEvents(),
        callService: FakeCallBackendService = FakeCallBackendService(),
        videoBackend: FakeVideoCallBackend = FakeVideoCallBackend(),
        audioClient: FakeCallAudioClient = FakeCallAudioClient(),
        videoClient: FakeCallVideoClient = FakeCallVideoClient(),
        ringtonePlayer: FakeCallRingtonePlayer = FakeCallRingtonePlayer()
    ): AndroidCallManager {
        val context =
            ApplicationProvider.getApplicationContext<Context>()

        return AndroidCallManager(
            context = context,
            socketManager = socketEvents,
            callService = callService,
            videoRepository = videoBackend,
            voiceManager = audioClient,
            videoManager = videoClient,
            ringtonePlayer = ringtonePlayer,
            callDispatcher = testDispatcher,
            analytics = analytics
        )
    }
}

private class FakeCallRealtimeEvents : CallRealtimeEvents {
    val incomingCallsFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val callEndedFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val incomingVideoCallsFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val videoCallEndedFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)

    override val incomingCalls: SharedFlow<String> = incomingCallsFlow
    override val callEnded: SharedFlow<String> = callEndedFlow
    override val incomingVideoCalls: SharedFlow<String> = incomingVideoCallsFlow
    override val videoCallEnded: SharedFlow<String> = videoCallEndedFlow
}

private class FakeCallBackendService : CallBackendService {
    var endCallReason: String? = null

    override fun createAppCall(
        calleeId: Int,
        video: Boolean
    ): Int {
        return 777
    }

    override fun startExternalCall(
        phoneNumber: String
    ): Int {
        return 888
    }

    override fun endCall(
        callId: Int,
        reason: String?,
        durationSec: Int?
    ) {
        endCallReason = reason
    }

    override fun fetchVoiceToken(): VoiceTokenResponse {
        return VoiceTokenResponse(
            token = "fake-voice-token"
        )
    }
}

private class FakeVideoCallBackend : VideoCallBackend {
    override fun startVideo(
        calleeId: Int,
        chatRoomId: Int?
    ): VideoStartResponse {
        return VideoStartResponse(
            ok = true,
            callId = 999,
            roomName = "fake-room"
        )
    }

    override fun fetchVideoToken(
        identity: String,
        roomName: String
    ): VideoTokenResponse {
        return VideoTokenResponse(
            token = "fake-video-token"
        )
    }
}

private class FakeCallAudioClient(
    private val acceptCallResult: Boolean = false
) : CallAudioClient {

    var lastListener: CallAudioClient.Listener? = null
    var endCallWasCalled = false

    override fun startCall(
        accessToken: String,
        to: String,
        listener: CallAudioClient.Listener
    ) {
        lastListener = listener
    }

    override fun acceptCall(): Boolean {
        return acceptCallResult
    }

    override fun endCall() {
        endCallWasCalled = true
    }

    override fun setMuted(
        isMuted: Boolean
    ) {
        // No-op for test.
    }

    override fun setSpeaker(
        enabled: Boolean
    ) {
        // No-op for test.
    }
}

private class FakeCallVideoClient : CallVideoClient {
    var lastListener: CallVideoClient.Listener? = null
    var disconnectWasCalled = false

    override fun connect(
        accessToken: String,
        roomName: String,
        listener: CallVideoClient.Listener
    ) {
        lastListener = listener
    }

    override fun disconnect() {
        disconnectWasCalled = true
    }

    override fun setMuted(
        isMuted: Boolean
    ) {
        // No-op for test.
    }

    override fun setCameraEnabled(
        enabled: Boolean
    ) {
        // No-op for test.
    }

    override fun flipCamera() {
        // No-op for test.
    }
}

private class FakeCallRingtonePlayer : CallRingtonePlayer {
    var played = false
    var stopped = false

    override fun playSavedRingtone() {
        played = true
    }

    override fun stop() {
        stopped = true
    }
}