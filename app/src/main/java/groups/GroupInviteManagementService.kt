package com.chatforia.android.groups

import com.chatforia.android.messages.RoomParticipantsResponse
import com.chatforia.android.network.ApiClient
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class GroupInviteRecord(
    val code: String,
    val expiresAt: String? = null,
    val maxUses: Int = 0,
    val uses: Int = 0,
    val createdAt: String? = null
) {
    val isExhausted: Boolean get() = maxUses > 0 && uses >= maxUses
}

@Serializable
data class GroupInviteCreated(
    val code: String,
    val url: String,
    val expiresAt: String? = null,
    val maxUses: Int = 0,
    val uses: Int = 0
)

// The server remains authoritative for ownership, admin permission, and usage limits.
// Keep network requests off the Compose main thread.
class GroupInviteManagementService(private val client: ApiClient) {
    suspend fun canManage(roomId: Int, userId: Int): Boolean =
        withContext(Dispatchers.IO) {
            val room: RoomParticipantsResponse = client.send(
                ApiRequest(
                    path = "chatrooms/$roomId/participants?_=" + System.currentTimeMillis(),
                    method = HttpMethod.GET
                )
            )
            room.ownerId == userId || room.participants.any {
                it.userId == userId && it.role.equals("ADMIN", ignoreCase = true)
            }
        }

    suspend fun loadMembers(roomId: Int): RoomParticipantsResponse =
        withContext(Dispatchers.IO) {
            client.send(
                ApiRequest(
                    path = "chatrooms/$roomId/participants?_=" + System.currentTimeMillis(),
                    method = HttpMethod.GET
                )
            )
        }

    // The API enforces the rank ladder. UI additionally protects the owner
    // and prevents deleting one's own membership by mistake.
    suspend fun removeMember(roomId: Int, targetUserId: Int) =
        withContext(Dispatchers.IO) {
            require(roomId > 0 && targetUserId > 0)
            client.sendRaw(
                ApiRequest(
                    path = "chatrooms/$roomId/participants/$targetUserId",
                    method = HttpMethod.DELETE
                )
            )
            Unit
        }

    suspend fun create(roomId: Int, expiresInMinutes: Int, maxUses: Int): GroupInviteCreated =
        withContext(Dispatchers.IO) {
            require(expiresInMinutes in 0..10080 && maxUses in 0..10000) {
                "Invalid invitation limits"
            }
            client.send(
                ApiRequest(
                    path = "chatrooms/$roomId/invites",
                    method = HttpMethod.POST,
                    bodyJson = """{"expiresInMinutes":$expiresInMinutes,"maxUses":$maxUses}"""
                )
            )
        }

    suspend fun list(roomId: Int): List<GroupInviteRecord> =
        withContext(Dispatchers.IO) {
            client.send(ApiRequest(path = "chatrooms/$roomId/invites", method = HttpMethod.GET))
        }

    suspend fun revoke(roomId: Int, code: String) =
        withContext(Dispatchers.IO) {
            client.sendRaw(
                ApiRequest(
                    path = "chatrooms/$roomId/invites/$code",
                    method = HttpMethod.DELETE
                )
            )
            Unit
        }
}
