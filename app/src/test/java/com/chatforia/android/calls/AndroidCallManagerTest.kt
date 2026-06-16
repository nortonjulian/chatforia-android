package com.chatforia.android.calls

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.chatforia.android.auth.UserDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AndroidCallManagerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun restoreIncomingCallPlaysRingtoneAndSetsRingingState() =
        runTest(mainDispatcherRule.testDispatcher) {
            val ringtone = FakeCallRingtonePlayer()
            val manager = createManager(ringtonePlayer = ringtone)

            val payload =
                IncomingCallPayload(
                    callId = 10,
                    callerId = 20,
                    callerName = "Julian",
                    fromNumber = "+15551234567",
                    mode = "AUDIO"
                )

            manager.restoreIncomingCall(payload)

            assertEquals(1, ringtone.playCount)

            val state = manager.state.value

            assertTrue(state is AndroidCallState.Ringing)
            assertEquals(payload, (state as AndroidCallState.Ringing).payload)
        }

    @Test
    fun acceptIncomingAudioStopsRingtoneAndSetsActiveState() =
        runTest(mainDispatcherRule.testDispatcher) {
            val ringtone = FakeCallRingtonePlayer()
            val voice = FakeCallAudioClient()
            voice.acceptCallResult = true

            val manager =
                createManager(
                    ringtonePlayer = ringtone,
                    voiceManager = voice
                )

            manager.restoreIncomingCall(
                IncomingCallPayload(
                    callId = 11,
                    callerName = "Audio Caller",
                    mode = "AUDIO"
                )
            )

            manager.acceptIncoming(currentUser())

            val state = manager.state.value

            assertEquals(1, ringtone.stopCount)
            assertEquals(1, voice.acceptCallCount)
            assertTrue(state is AndroidCallState.Active)

            val session = (state as AndroidCallState.Active).session

            assertEquals(11, session.callId)
            assertEquals("Audio Caller", session.displayName)
            assertEquals(false, session.isVideo)
        }

    @Test
    fun acceptIncomingAudioFailsWhenVoiceClientCannotAccept() =
        runTest(mainDispatcherRule.testDispatcher) {
            val voice = FakeCallAudioClient()
            voice.acceptCallResult = false

            val manager = createManager(voiceManager = voice)

            manager.restoreIncomingCall(
                IncomingCallPayload(
                    callId = 12,
                    callerName = "Audio Caller",
                    mode = "AUDIO"
                )
            )

            manager.acceptIncoming(currentUser())

            val state = manager.state.value

            assertTrue(state is AndroidCallState.Failed)
            assertEquals(
                "Incoming audio calls are not available yet on this device.",
                (state as AndroidCallState.Failed).message
            )
        }

    @Test
    fun acceptIncomingVideoStopsRingtoneFetchesTokenAndConnectsVideo() =
        runTest(mainDispatcherRule.testDispatcher) {
            val ringtone = FakeCallRingtonePlayer()
            val videoBackend = FakeVideoCallBackend()
            val videoClient = FakeCallVideoClient()

            val manager =
                createManager(
                    ringtonePlayer = ringtone,
                    videoRepository = videoBackend,
                    videoManager = videoClient
                )

            manager.restoreIncomingCall(
                IncomingCallPayload(
                    callId = 13,
                    callerName = "Video Caller",
                    mode = "VIDEO",
                    roomName = "room-13"
                )
            )

            manager.acceptIncoming(currentUser(id = 99))

            advanceUntilIdle()

            assertEquals(1, ringtone.stopCount)
            assertEquals(listOf("99" to "room-13"), videoBackend.videoTokenRequests)

            val connectRequest = videoClient.connectRequests.single()

            assertEquals("video-token", connectRequest.accessToken)
            assertEquals("room-13", connectRequest.roomName)

            connectRequest.listener.onConnected()

            val state = manager.state.value

            assertTrue(state is AndroidCallState.Active)

            val session = (state as AndroidCallState.Active).session

            assertEquals(13, session.callId)
            assertEquals("room-13", session.roomName)
            assertEquals("Video Caller", session.displayName)
            assertEquals(true, session.isVideo)
            assertEquals(true, session.speakerEnabled)
        }

    @Test
    fun declineIncomingStopsRingtoneEndsCallAndReturnsIdle() =
        runTest(mainDispatcherRule.testDispatcher) {
            val ringtone = FakeCallRingtonePlayer()
            val callBackend = FakeCallBackendService()

            val manager =
                createManager(
                    ringtonePlayer = ringtone,
                    callService = callBackend
                )

            manager.restoreIncomingCall(
                IncomingCallPayload(
                    callId = 14,
                    callerName = "Decline Caller"
                )
            )

            manager.declineIncoming()

            advanceUntilIdle()

            assertEquals(1, ringtone.stopCount)
            assertEquals(
                listOf(EndCallRecord(callId = 14, reason = "declined", durationSec = null)),
                callBackend.endCallRecords
            )
            assertEquals(AndroidCallState.Idle, manager.state.value)
        }

    @Test
    fun incomingAudioSocketPayloadStartsRinging() =
        runTest(mainDispatcherRule.testDispatcher) {
            val socket = FakeCallRealtimeEvents()
            val ringtone = FakeCallRingtonePlayer()

            val manager =
                createManager(
                    socketManager = socket,
                    ringtonePlayer = ringtone
                )

            advanceUntilIdle()

            socket.emitIncomingCall(
                """
                {
                  "callId": 15,
                  "callerId": 25,
                  "callerName": "Socket Caller",
                  "fromNumber": "+15550001111",
                  "mode": "AUDIO"
                }
                """.trimIndent()
            )

            advanceUntilIdle()

            assertEquals(1, ringtone.playCount)

            val state = manager.state.value

            assertTrue(state is AndroidCallState.Ringing)

            val payload = (state as AndroidCallState.Ringing).payload

            assertEquals(15, payload.callId)
            assertEquals(25, payload.callerId)
            assertEquals("Socket Caller", payload.callerName)
            assertEquals("AUDIO", payload.mode)
        }

    @Test
    fun incomingVideoSocketPayloadDefaultsModeToVideo() =
        runTest(mainDispatcherRule.testDispatcher) {
            val socket = FakeCallRealtimeEvents()
            val ringtone = FakeCallRingtonePlayer()

            val manager =
                createManager(
                    socketManager = socket,
                    ringtonePlayer = ringtone
                )

            advanceUntilIdle()

            socket.emitIncomingVideoCall(
                """
                {
                  "callId": 16,
                  "callerId": 26,
                  "callerName": "Video Socket Caller",
                  "roomName": "video-room-16"
                }
                """.trimIndent()
            )

            advanceUntilIdle()

            assertEquals(1, ringtone.playCount)

            val state = manager.state.value

            assertTrue(state is AndroidCallState.Ringing)

            val payload = (state as AndroidCallState.Ringing).payload

            assertEquals(16, payload.callId)
            assertEquals("VIDEO", payload.mode)
            assertEquals("video-room-16", payload.roomName)
        }

    @Test
    fun malformedIncomingSocketPayloadDoesNotCrashOrRing() =
        runTest(mainDispatcherRule.testDispatcher) {
            val socket = FakeCallRealtimeEvents()
            val ringtone = FakeCallRingtonePlayer()

            val manager =
                createManager(
                    socketManager = socket,
                    ringtonePlayer = ringtone
                )

            advanceUntilIdle()

            socket.emitIncomingCall("not-json")

            advanceUntilIdle()

            assertEquals(0, ringtone.playCount)
            assertEquals(AndroidCallState.Idle, manager.state.value)
        }

    @Test
    fun callEndedSocketStopsEverythingAndSetsEnded() =
        runTest(mainDispatcherRule.testDispatcher) {
            val socket = FakeCallRealtimeEvents()
            val ringtone = FakeCallRingtonePlayer()
            val voice = FakeCallAudioClient()
            val video = FakeCallVideoClient()

            val manager =
                createManager(
                    socketManager = socket,
                    ringtonePlayer = ringtone,
                    voiceManager = voice,
                    videoManager = video
                )

            advanceUntilIdle()

            socket.emitCallEnded("{}")

            advanceUntilIdle()

            assertEquals(1, ringtone.stopCount)
            assertEquals(1, voice.endCallCount)
            assertEquals(1, video.disconnectCount)
            assertTrue(manager.state.value is AndroidCallState.Ended)
        }

    @Test
    fun startAudioCallCreatesCallFetchesTokenAndStartsVoiceClient() =
        runTest(mainDispatcherRule.testDispatcher) {
            val callBackend = FakeCallBackendService()
            val voice = FakeCallAudioClient()

            val manager =
                createManager(
                    callService = callBackend,
                    voiceManager = voice
                )

            manager.startAudioCall(
                calleeId = 44,
                displayName = "Audio Friend"
            )

            advanceUntilIdle()

            assertEquals(listOf(44 to false), callBackend.createAppCallRequests)
            assertEquals(1, callBackend.fetchVoiceTokenCount)

            val request = voice.startCallRequests.single()

            assertEquals("voice-token", request.accessToken)
            assertEquals("44", request.to)

            assertTrue(manager.state.value is AndroidCallState.Connecting)

            request.listener.onConnected()

            val state = manager.state.value

            assertTrue(state is AndroidCallState.Active)
            assertEquals("Audio Friend", (state as AndroidCallState.Active).session.displayName)
        }

    @Test
    fun startVideoCallStartsVideoFetchesTokenAndConnectsVideoClient() =
        runTest(mainDispatcherRule.testDispatcher) {
            val videoBackend = FakeVideoCallBackend()
            val videoClient = FakeCallVideoClient()

            val manager =
                createManager(
                    videoRepository = videoBackend,
                    videoManager = videoClient
                )

            manager.startVideoCall(
                currentUser = currentUser(id = 77),
                calleeId = 88,
                displayName = "Video Friend",
                chatRoomId = 99
            )

            advanceUntilIdle()

            assertEquals(listOf(88 to 99), videoBackend.startVideoRequests)
            assertEquals(listOf("77" to "video-room"), videoBackend.videoTokenRequests)

            val connectRequest = videoClient.connectRequests.single()

            assertEquals("video-token", connectRequest.accessToken)
            assertEquals("video-room", connectRequest.roomName)

            connectRequest.listener.onConnected()

            val state = manager.state.value

            assertTrue(state is AndroidCallState.Active)

            val session = (state as AndroidCallState.Active).session

            assertEquals(222, session.callId)
            assertEquals("Video Friend", session.displayName)
            assertEquals(true, session.isVideo)
        }

    private fun createManager(
        socketManager: FakeCallRealtimeEvents = FakeCallRealtimeEvents(),
        callService: FakeCallBackendService = FakeCallBackendService(),
        videoRepository: FakeVideoCallBackend = FakeVideoCallBackend(),
        voiceManager: FakeCallAudioClient = FakeCallAudioClient(),
        videoManager: FakeCallVideoClient = FakeCallVideoClient(),
        ringtonePlayer: FakeCallRingtonePlayer = FakeCallRingtonePlayer()
    ): AndroidCallManager {
        return AndroidCallManager(
            context = ApplicationProvider.getApplicationContext<Context>(),
            socketManager = socketManager,
            callService = callService,
            videoRepository = videoRepository,
            voiceManager = voiceManager,
            videoManager = videoManager,
            ringtonePlayer = ringtonePlayer,
            callDispatcher = mainDispatcherRule.testDispatcher
        )
    }

    private fun currentUser(
        id: Int = 1
    ): UserDto {
        return UserDto(
            id = id,
            email = "user$id@example.com",
            username = "user_$id",
            preferredLanguage = "en",
            uiLanguage = "en"
        )
    }

    private class FakeCallRealtimeEvents : CallRealtimeEvents {
        private val _incomingCalls =
            MutableSharedFlow<String>(extraBufferCapacity = 64)

        override val incomingCalls: SharedFlow<String> =
            _incomingCalls.asSharedFlow()

        private val _callEnded =
            MutableSharedFlow<String>(extraBufferCapacity = 64)

        override val callEnded: SharedFlow<String> =
            _callEnded.asSharedFlow()

        private val _incomingVideoCalls =
            MutableSharedFlow<String>(extraBufferCapacity = 64)

        override val incomingVideoCalls: SharedFlow<String> =
            _incomingVideoCalls.asSharedFlow()

        private val _videoCallEnded =
            MutableSharedFlow<String>(extraBufferCapacity = 64)

        override val videoCallEnded: SharedFlow<String> =
            _videoCallEnded.asSharedFlow()

        fun emitIncomingCall(raw: String) {
            _incomingCalls.tryEmit(raw)
        }

        fun emitCallEnded(raw: String) {
            _callEnded.tryEmit(raw)
        }

        fun emitIncomingVideoCall(raw: String) {
            _incomingVideoCalls.tryEmit(raw)
        }

        fun emitVideoCallEnded(raw: String) {
            _videoCallEnded.tryEmit(raw)
        }
    }

    private data class EndCallRecord(
        val callId: Int,
        val reason: String?,
        val durationSec: Int?
    )

    private class FakeCallBackendService : CallBackendService {
        val createAppCallRequests = mutableListOf<Pair<Int, Boolean>>()
        val externalCallRequests = mutableListOf<String>()
        val endCallRecords = mutableListOf<EndCallRecord>()

        var createAppCallResult = 111
        var startExternalCallResult = 112
        var fetchVoiceTokenCount = 0

        override fun createAppCall(
            calleeId: Int,
            video: Boolean
        ): Int {
            createAppCallRequests.add(calleeId to video)
            return createAppCallResult
        }

        override fun startExternalCall(
            phoneNumber: String
        ): Int {
            externalCallRequests.add(phoneNumber)
            return startExternalCallResult
        }

        override fun endCall(
            callId: Int,
            reason: String?,
            durationSec: Int?
        ) {
            endCallRecords.add(
                EndCallRecord(
                    callId = callId,
                    reason = reason,
                    durationSec = durationSec
                )
            )
        }

        override fun fetchVoiceToken(): VoiceTokenResponse {
            fetchVoiceTokenCount++
            return VoiceTokenResponse(token = "voice-token")
        }
    }

    private class FakeVideoCallBackend : VideoCallBackend {
        val startVideoRequests = mutableListOf<Pair<Int, Int?>>()
        val videoTokenRequests = mutableListOf<Pair<String, String>>()

        override fun startVideo(
            calleeId: Int,
            chatRoomId: Int?
        ): VideoStartResponse {
            startVideoRequests.add(calleeId to chatRoomId)

            return VideoStartResponse(
                ok = true,
                callId = 222,
                roomName = "video-room"
            )
        }

        override fun fetchVideoToken(
            identity: String,
            roomName: String
        ): VideoTokenResponse {
            videoTokenRequests.add(identity to roomName)
            return VideoTokenResponse(token = "video-token")
        }
    }

    private data class VoiceStartCallRequest(
        val accessToken: String,
        val to: String,
        val listener: CallAudioClient.Listener
    )

    private class FakeCallAudioClient : CallAudioClient {
        val startCallRequests = mutableListOf<VoiceStartCallRequest>()

        var acceptCallResult = true
        var acceptCallCount = 0
        var endCallCount = 0
        val mutedValues = mutableListOf<Boolean>()
        val speakerValues = mutableListOf<Boolean>()

        override fun startCall(
            accessToken: String,
            to: String,
            listener: CallAudioClient.Listener
        ) {
            startCallRequests.add(
                VoiceStartCallRequest(
                    accessToken = accessToken,
                    to = to,
                    listener = listener
                )
            )
        }

        override fun acceptCall(): Boolean {
            acceptCallCount++
            return acceptCallResult
        }

        override fun endCall() {
            endCallCount++
        }

        override fun setMuted(
            isMuted: Boolean
        ) {
            mutedValues.add(isMuted)
        }

        override fun setSpeaker(
            enabled: Boolean
        ) {
            speakerValues.add(enabled)
        }
    }

    private data class VideoConnectRequest(
        val accessToken: String,
        val roomName: String,
        val listener: CallVideoClient.Listener
    )

    private class FakeCallVideoClient : CallVideoClient {
        val connectRequests = mutableListOf<VideoConnectRequest>()

        var disconnectCount = 0
        val mutedValues = mutableListOf<Boolean>()
        val cameraValues = mutableListOf<Boolean>()
        var flipCameraCount = 0

        override fun connect(
            accessToken: String,
            roomName: String,
            listener: CallVideoClient.Listener
        ) {
            connectRequests.add(
                VideoConnectRequest(
                    accessToken = accessToken,
                    roomName = roomName,
                    listener = listener
                )
            )
        }

        override fun disconnect() {
            disconnectCount++
        }

        override fun setMuted(
            isMuted: Boolean
        ) {
            mutedValues.add(isMuted)
        }

        override fun setCameraEnabled(
            enabled: Boolean
        ) {
            cameraValues.add(enabled)
        }

        override fun flipCamera() {
            flipCameraCount++
        }
    }

    private class FakeCallRingtonePlayer : CallRingtonePlayer {
        var playCount = 0
        var stopCount = 0

        override fun playSavedRingtone() {
            playCount++
        }

        override fun stop() {
            stopCount++
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {

    override fun starting(
        description: Description
    ) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(
        description: Description
    ) {
        Dispatchers.resetMain()
    }
}