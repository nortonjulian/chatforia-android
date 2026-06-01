package com.chatforia.android.socket

import com.chatforia.android.network.Environment
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

class SocketManager {

    private var socket: Socket? = null
    private val joinedRoomIds = mutableSetOf<Int>()

    private val _messageUpserts =
        MutableSharedFlow<String>(
            replay = 0,
            extraBufferCapacity = 64
        )

    val messageUpserts: SharedFlow<String> =
        _messageUpserts.asSharedFlow()

    fun connect(token: String) {
        if (token.isBlank()) return

        val options = IO.Options().apply {
            path = "/socket.io"
            transports = arrayOf("websocket")
            reconnection = true
            reconnectionAttempts = Int.MAX_VALUE
            reconnectionDelay = 1000
            query = "token=$token"
        }

        socket?.disconnect()

        socket = IO.socket(
            URI.create(Environment.API_BASE_URL),
            options
        )

        socket?.on(Socket.EVENT_CONNECT) {
            println("✅ Android socket connected")

            joinedRoomIds.forEach { roomId ->
                emitJoinRoom(roomId)
            }
        }

        socket?.on(Socket.EVENT_DISCONNECT) {
            println("⚠️ Android socket disconnected")
        }

        socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
            println("❌ Android socket connect error: ${args.joinToString()}")
        }

        socket?.on("message:upsert") { args ->
            val messageJson = extractMessageJson(args)

            if (messageJson != null) {
                _messageUpserts.tryEmit(messageJson)
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

    fun joinRoom(roomId: Int) {
        joinedRoomIds.add(roomId)
        emitJoinRoom(roomId)
    }

    fun leaveRoom(roomId: Int) {
        joinedRoomIds.remove(roomId)

        val payload = JSONObject()
            .put("roomId", roomId)

        socket?.emit("leaveRoom", payload)
    }

    private fun emitJoinRoom(roomId: Int) {
        val payload = JSONObject()
            .put("roomId", roomId)

        socket?.emit("joinRoom", payload)

        println("📡 Android joined room $roomId")
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
                println("⚠️ Unsupported message:upsert payload: $first")
                null
            }
        }
    }
}