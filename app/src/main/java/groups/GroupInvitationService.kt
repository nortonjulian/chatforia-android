package com.chatforia.android.groups

import java.net.URI
import com.chatforia.android.network.ApiClient
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.HttpMethod
import kotlinx.serialization.Serializable

object GroupInvitationLink {
    private val validCode = Regex("^[A-Za-z0-9_-]{8,128}$")

    fun extract(link: String?): String? {
        if (link.isNullOrBlank()) return null
        val uri = runCatching { URI(link) }.getOrNull() ?: return null
        val parts = uri.path?.split("/")?.filter { it.isNotEmpty() } ?: return null
        val code = when {
            uri.scheme.equals("https", ignoreCase = true) &&
                (uri.host.equals("chatforia.com", ignoreCase = true) ||
                 uri.host.equals("www.chatforia.com", ignoreCase = true)) &&
                parts.size == 2 && parts[0] == "join" -> parts[1]
            uri.scheme.equals("chatforia", ignoreCase = true) &&
                uri.host.equals("join", ignoreCase = true) &&
                parts.size == 1 -> parts[0]
            else -> null
        }
        return code?.takeIf { validCode.matches(it) }
    }
}

@Serializable
data class GroupInvitationPreview(
    val roomId: Int,
    val roomName: String? = null,
    val status: String
)

@Serializable
data class GroupInvitationJoinResult(
    val ok: Boolean,
    val roomId: Int,
    val alreadyMember: Boolean = false
)

class GroupInvitationService(private val client: ApiClient) {
    fun preview(code: String): GroupInvitationPreview =
        client.send(ApiRequest(
            path = "invites/$code",
            method = HttpMethod.GET,
            requiresAuth = false
        ))

    fun accept(code: String): GroupInvitationJoinResult =
        client.send(ApiRequest(
            path = "invites/$code/accept",
            method = HttpMethod.POST,
            requiresAuth = true
        ))
}
