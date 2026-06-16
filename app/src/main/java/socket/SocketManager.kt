package com.chatforia.android.socket

import android.util.Log
import com.chatforia.android.network.Environment
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

class SocketManager : ChatRealtimeEvents {
    private var socket: Socket? = null
    private val joinedRoomIds = mutableSetOf<Int>()

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
    val incomingCalls: SharedFlow<String> = _incomingCalls.asSharedFlow()

    private val _callEnded = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val callEnded: SharedFlow<String> = _callEnded.asSharedFlow()

    private val _incomingVideoCalls = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val incomingVideoCalls: SharedFlow<String> = _incomingVideoCalls.asSharedFlow()

    private val _videoCallEnded = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val videoCallEnded: SharedFlow<String> = _videoCallEnded.asSharedFlow()

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

        val options = IO.Options().apply {
            path = "/socket.io"
            transports = arrayOf("websocket", "polling")
            reconnection = true
            reconnectionAttempts = Int.MAX_VALUE
            reconnectionDelay = 500
            auth = mapOf("token" to token)
        }

        socket?.off()
        socket?.disconnect()

        socket = IO.socket(URI.create(Environment.API_BASE_URL), options)

        socket?.on(Socket.EVENT_CONNECT) {
            Log.d("ChatforiaSocket", "✅ Android socket connected ${socket?.id()}")
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
            Log.e("ChatforiaSocket", "❌ connect error: ${args.joinToString()}")
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
        socket?.off()
        socket?.disconnect()
        socket = null
        joinedRoomIds.clear()
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