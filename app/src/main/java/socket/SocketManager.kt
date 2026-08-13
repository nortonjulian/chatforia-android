package com.chatforia.android.socket

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.chatforia.android.network.Environment
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import okhttp3.OkHttpClient
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.concurrent.TimeUnit
import com.chatforia.android.calls.CallRealtimeEvents

class SocketManager(
    context: Context
) : ChatRealtimeEvents, CallRealtimeEvents {
    private val connectivityManager =
        context.applicationContext.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager

    private val mainHandler =
        Handler(Looper.getMainLooper())

    private val recoveryPolicy =
        SocketRecoveryPolicy()

    private var socket: Socket? = null
    private var transportClient: OkHttpClient? = null
    private var currentToken: String? = null
    private var networkCallbackRegistered = false
    private var recoveryRunnable: Runnable? = null

    private val joinedRoomIds = mutableSetOf<Int>()

    private val networkCallback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                recoveryPolicy.markNetworkLost()
                cancelScheduledRecovery()

                Log.d(
                    "ChatforiaSocket",
                    "Network lost; socket recovery armed"
                )
            }

            override fun onAvailable(network: Network) {
                recoverForValidatedNetwork(network)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                if (
                    capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_VALIDATED
                    )
                ) {
                    recoverForValidatedNetwork(network)
                }
            }
        }

    private val _messageUpserts = MutableSharedFlow<String>(extraBufferCapacity = 64)
    override val messageUpserts: SharedFlow<String> = _messageUpserts.asSharedFlow()

    private val _messageAcks = MutableSharedFlow<String>(extraBufferCapacity = 64)
    override val messageAcks: SharedFlow<String> = _messageAcks.asSharedFlow()

    private val _messageEdited = MutableSharedFlow<String>(extraBufferCapacity = 64)
    override val messageEdited: SharedFlow<String> = _messageEdited.asSharedFlow()

    private val _messageDeleted = MutableSharedFlow<String>(extraBufferCapacity = 64)
    override val messageDeleted: SharedFlow<String> = _messageDeleted.asSharedFlow()

    private val _messageExpired = MutableSharedFlow<String>(extraBufferCapacity = 64)
    override val messageExpired: SharedFlow<String> = _messageExpired.asSharedFlow()

    private val _socketConnected = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
    override val socketConnected: SharedFlow<Unit> = _socketConnected.asSharedFlow()

    private val _incomingCalls = MutableSharedFlow<String>(extraBufferCapacity = 64)
    override val incomingCalls: SharedFlow<String> = _incomingCalls.asSharedFlow()

    private val _callEnded = MutableSharedFlow<String>(extraBufferCapacity = 64)
    override val callEnded: SharedFlow<String> = _callEnded.asSharedFlow()

    private val _incomingVideoCalls = MutableSharedFlow<String>(extraBufferCapacity = 64)
    override val incomingVideoCalls: SharedFlow<String> = _incomingVideoCalls.asSharedFlow()

    private val _videoCallEnded = MutableSharedFlow<String>(extraBufferCapacity = 64)
    override val videoCallEnded: SharedFlow<String> = _videoCallEnded.asSharedFlow()

    private val _voicemailEvents = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val voicemailEvents: SharedFlow<String> = _voicemailEvents.asSharedFlow()

    private val _messageReads =
        MutableSharedFlow<String>(extraBufferCapacity = 64)

    override val messageReads: SharedFlow<String> =
        _messageReads.asSharedFlow()

    private val _smsMessages =
        MutableSharedFlow<String>(extraBufferCapacity = 64)

    override val smsMessages: SharedFlow<String> =
        _smsMessages.asSharedFlow()

    private val _randomChatMatched =
        MutableSharedFlow<String>(extraBufferCapacity = 64)

    val randomChatMatched: SharedFlow<String> =
        _randomChatMatched.asSharedFlow()

    private val _randomChatWaiting =
        MutableSharedFlow<String>(extraBufferCapacity = 64)

    val randomChatWaiting: SharedFlow<String> =
        _randomChatWaiting.asSharedFlow()

    private val _randomChatError =
        MutableSharedFlow<String>(extraBufferCapacity = 64)

    val randomChatError: SharedFlow<String> =
        _randomChatError.asSharedFlow()

    private val _randomChatMessages =
        MutableSharedFlow<String>(extraBufferCapacity = 64)

    val randomChatMessages: SharedFlow<String> =
        _randomChatMessages.asSharedFlow()

    private val _randomChatEnded =
        MutableSharedFlow<String>(extraBufferCapacity = 64)

    val randomChatEnded: SharedFlow<String> =
        _randomChatEnded.asSharedFlow()

    private val _randomFriendAccepted =
        MutableSharedFlow<String>(extraBufferCapacity = 64)

    val randomFriendAccepted: SharedFlow<String> =
        _randomFriendAccepted.asSharedFlow()

    fun connect(token: String) {
        if (token.isBlank()) return

        currentToken = token
        registerNetworkCallback()

        if (!hasValidatedNetwork()) {
            recoveryPolicy.markNetworkLost()
        }

        connectSocket(token)
    }

    private fun connectSocket(token: String) {
        if (token.isBlank()) return

        val previousSocket = socket
        socket = null

        previousSocket?.off()
        previousSocket?.disconnect()

        closeTransportClient()

        val freshTransportClient =
            OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .readTimeout(1, TimeUnit.MINUTES)
                .build()

        transportClient = freshTransportClient

        val options = IO.Options().apply {
            path = "/socket.io"

            /*
             * Use Socket.IO's default polling-first negotiation.
             * Polling provides a reliable reconnection path after
             * Android suspends or replaces a WebSocket. The client
             * upgrades to WebSocket when the network supports it.
             */
            reconnection = true
            reconnectionAttempts = Int.MAX_VALUE
            reconnectionDelay = 1_000
            reconnectionDelayMax = 10_000
            randomizationFactor = 0.5
            timeout = 20_000
            auth = mapOf("token" to token)

            callFactory = freshTransportClient
            webSocketFactory = freshTransportClient
        }

        socket = IO.socket(
            URI.create(Environment.API_BASE_URL),
            options
        )

        socket?.on(Socket.EVENT_CONNECT) {
            recoveryPolicy.markConnected()
            cancelScheduledRecovery()

            Log.d(
                "ChatforiaSocket",
                "✅ Android socket connected ${socket?.id()}"
            )

            emitJoinRooms()
            _socketConnected.tryEmit(Unit)
        }

        socket?.on("message_read") { args ->
            args.firstOrNull()?.let {
                _messageReads.tryEmit(it.toString())
            }
        }

        socket?.on(Socket.EVENT_DISCONNECT) { args ->
            Log.d("ChatforiaSocket", "⚠️ Android socket disconnected ${args.joinToString()}")
        }

        socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
            val error = args.firstOrNull()

            val details =
                args.joinToString {
                    val type =
                        it?.javaClass?.name
                            ?: "null"

                    "$type: $it"
                }

            if (error is Throwable) {
                Log.e(
                    "ChatforiaSocket",
                    "❌ Socket.IO connection failed: $details",
                    error
                )
            } else {
                Log.e(
                    "ChatforiaSocket",
                    "❌ Socket.IO connection failed: $details"
                )
            }

            if (
                recoveryPolicy.shouldRecoverAfterFailure(
                    networkValidated =
                        hasValidatedNetwork()
                )
            ) {
                scheduleSocketRecovery(
                    reason =
                        "repeated failures on validated network",
                    delayMs = 500
                )
            }
        }

        socket?.on("message:upsert") { args ->
            Log.d("ChatforiaSocket", "📨 message:upsert ${args.joinToString()}")

            extractMessageJson(args)
                ?.let { _messageUpserts.tryEmit(it) }
        }

        socket?.on("message:ack") { args ->
            Log.d("ChatforiaSocket", "✅ message:ack ${args.joinToString()}")

            args.firstOrNull()
                ?.let { _messageAcks.tryEmit(it.toString()) }
        }

        socket?.on("message:edited") { args ->
            Log.d("ChatforiaSocket", "✏️ message:edited ${args.joinToString()}")

            extractMessageJson(args)
                ?.let { _messageEdited.tryEmit(it) }
        }

        socket?.on("message:deleted") { args ->
            Log.d("ChatforiaSocket", "🗑️ message:deleted ${args.joinToString()}")

            args.firstOrNull()
                ?.let { _messageDeleted.tryEmit(it.toString()) }
        }

        socket?.on("message:expired") { args ->
            Log.d("ChatforiaSocket", "⏳ message:expired ${args.joinToString()}")

            args.firstOrNull()
                ?.let { _messageExpired.tryEmit(it.toString()) }
        }

        socket?.on("sms:message:new") { args ->
            Log.d(
                "ChatforiaSocket",
                "📱 sms:message:new ${args.joinToString()}"
            )

            extractSmsJson(args)
                ?.let { _smsMessages.tryEmit(it) }
        }

        socket?.onAnyIncoming { args ->
            Log.d("ChatforiaSocket", "📥 incoming ${args.joinToString()}")
        }

        socket?.on("call:incoming") { args ->
            args.firstOrNull()?.let { _incomingCalls.tryEmit(it.toString()) }
        }

        socket?.on("call:ended") { args ->
            args.firstOrNull()?.let { _callEnded.tryEmit(it.toString()) }
        }

        socket?.on("video:incoming") { args ->
            args.firstOrNull()?.let { _incomingVideoCalls.tryEmit(it.toString()) }
        }

        socket?.on("video:ended") { args ->
            args.firstOrNull()?.let { _videoCallEnded.tryEmit(it.toString()) }
        }

        socket?.on("voicemail:new") { args ->
            args.firstOrNull()?.let { _voicemailEvents.tryEmit(it.toString()) }
        }

        socket?.on("voicemail:updated") { args ->
            args.firstOrNull()?.let { _voicemailEvents.tryEmit(it.toString()) }
        }

        socket?.on("voicemail:deleted") { args ->
            args.firstOrNull()?.let { _voicemailEvents.tryEmit(it.toString()) }
        }

        socket?.on("random:waiting") { args ->
            args.firstOrNull()?.let {
                _randomChatWaiting.tryEmit(it.toString())
            }
        }

        socket?.on("random:message") { args ->
            args.firstOrNull()?.let {
                _randomChatMessages.tryEmit(it.toString())
            }
        }

        socket?.on("random:ended") { args ->
            args.firstOrNull()?.let {
                _randomChatEnded.tryEmit(it.toString())
            }
        }

        socket?.on("random:friend_accepted") { args ->
            args.firstOrNull()?.let {
                _randomFriendAccepted.tryEmit(it.toString())
            }
        }

        socket?.on("random:matched") { args ->
            args.firstOrNull()?.let {
                _randomChatMatched.tryEmit(it.toString())
            }
        }

        socket?.on("random:error") { args ->
            args.firstOrNull()?.let {
                _randomChatError.tryEmit(it.toString())
            }
        }

        socket?.connect()
    }

    fun disconnect() {
        currentToken = null
        cancelScheduledRecovery()
        unregisterNetworkCallback()
        recoveryPolicy.reset()

        socket?.off()
        socket?.disconnect()
        socket = null

        closeTransportClient()
        joinedRoomIds.clear()
    }

    private fun closeTransportClient() {
        val client = transportClient
        transportClient = null

        if (client == null) return

        client.dispatcher.cancelAll()
        client.connectionPool.evictAll()

        try {
            client.dispatcher.executorService.shutdown()
        } catch (error: RuntimeException) {
            Log.w(
                "ChatforiaSocket",
                "Could not shut down socket transport",
                error
            )
        }
    }

    private fun recoverForValidatedNetwork(
        network: Network
    ) {
        if (!isValidated(network)) return

        if (
            recoveryPolicy
                .shouldRecoverForValidatedNetwork()
        ) {
            scheduleSocketRecovery(
                reason = "validated network restored",
                delayMs = 1_500
            )
        }
    }

    private fun scheduleSocketRecovery(
        reason: String,
        delayMs: Long
    ) {
        if (recoveryRunnable != null) return

        val runnable =
            Runnable {
                recoveryRunnable = null

                val token = currentToken
                val networkValidated =
                    hasValidatedNetwork()
                val socketConnected =
                    socket?.connected() == true

                if (
                    !shouldRunScheduledSocketRecovery(
                        hasToken =
                            !token.isNullOrBlank(),
                        networkValidated =
                            networkValidated,
                        socketConnected =
                            socketConnected
                    )
                ) {
                    if (socketConnected) {
                        Log.d(
                            "ChatforiaSocket",
                            "Skipping scheduled socket rebuild; socket already connected"
                        )
                    }

                    return@Runnable
                }

                Log.d(
                    "ChatforiaSocket",
                    "Rebuilding socket after $reason"
                )

                connectSocket(
                    checkNotNull(token)
                )
            }

        recoveryRunnable = runnable
        mainHandler.postDelayed(runnable, delayMs)
    }

    private fun cancelScheduledRecovery() {
        recoveryRunnable?.let {
            mainHandler.removeCallbacks(it)
        }

        recoveryRunnable = null
    }

    private fun registerNetworkCallback() {
        if (networkCallbackRegistered) return

        try {
            connectivityManager.registerDefaultNetworkCallback(
                networkCallback
            )

            networkCallbackRegistered = true
        } catch (error: RuntimeException) {
            Log.e(
                "ChatforiaSocket",
                "Could not register network callback",
                error
            )
        }
    }

    private fun unregisterNetworkCallback() {
        if (!networkCallbackRegistered) return

        try {
            connectivityManager.unregisterNetworkCallback(
                networkCallback
            )
        } catch (error: RuntimeException) {
            Log.w(
                "ChatforiaSocket",
                "Could not unregister network callback",
                error
            )
        } finally {
            networkCallbackRegistered = false
        }
    }

    private fun hasValidatedNetwork(): Boolean {
        val network =
            connectivityManager.activeNetwork
                ?: return false

        return isValidated(network)
    }

    private fun isValidated(
        network: Network
    ): Boolean {
        val capabilities =
            connectivityManager
                .getNetworkCapabilities(network)
                ?: return false

        return capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_VALIDATED
        )
    }

    override fun joinRoom(roomId: Int) {
        joinedRoomIds.add(roomId)
        emitJoinRooms()
    }

    fun joinRooms(roomIds: List<Int>) {
        joinedRoomIds.addAll(roomIds)
        emitJoinRooms()
    }

    fun startRandomChat() {
        socket?.emit("random:join")
    }

    fun cancelRandomChat() {
        socket?.emit("random:leave")
    }

    fun sendRandomMessage(roomId: Int, text: String) {
        socket?.emit("random:message", JSONObject().apply {
            put("roomId", roomId)
            put("content", text)
        })
    }

    fun skipRandomChat() {
        socket?.emit("random:skip")
    }

    fun requestRandomFriend(roomId: Int) {
        socket?.emit("random:add_friend", JSONObject().apply {
            put("roomId", roomId)
        })
    }

    private fun emitJoinRooms() {
        if (joinedRoomIds.isEmpty()) return

        val roomIds = joinedRoomIds.toList()
        val ids = JSONArray(roomIds.map { it.toString() })

        Log.d("ChatforiaSocket", "📡 joining rooms $roomIds")

        socket?.emit("join:rooms", ids)
    }

    private fun extractMessageJson(args: Array<Any>): String? {
        val first = args.firstOrNull()

        return when (first) {
            is JSONObject -> {
                val messageObject =
                    first.optJSONObject("item")
                        ?: first.optJSONObject("shaped")
                        ?: first.optJSONObject("message")
                        ?: first

                messageObject.toString()
            }

            is JSONArray -> {
                first.optJSONObject(0)?.toString()
            }

            else -> {
                Log.w("ChatforiaSocket", "⚠️ Unsupported socket payload: $first")
                null
            }
        }
    }

    private fun extractSmsJson(
        args: Array<Any>
    ): String? {
        val first = args.firstOrNull()

        return when (first) {

            is JSONObject -> {
                first.toString()
            }

            is JSONArray -> {
                first.optJSONObject(0)?.toString()
            }

            else -> {
                Log.w(
                    "ChatforiaSocket",
                    "⚠️ Unsupported SMS payload: $first"
                )
                null
            }
        }
    }
}