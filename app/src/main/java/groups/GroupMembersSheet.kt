package com.chatforia.android.groups

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chatforia.android.messages.RoomParticipantDto
import com.chatforia.android.messages.RoomParticipantsResponse
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupMembersSheet(
    roomId: Int,
    currentUserId: Int,
    service: GroupInviteManagementService,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var room by remember(roomId) { mutableStateOf<RoomParticipantsResponse?>(null) }
    var loading by remember(roomId) { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf<RoomParticipantDto?>(null) }

    suspend fun refresh() {
        room = service.loadMembers(roomId)
        error = null
    }

    LaunchedEffect(roomId) {
        try {
            refresh()
        } catch (e: Exception) {
            error = e.message ?: "Could not load group members."
        } finally {
            loading = false
        }
    }

    val ownerId = room?.ownerId
    val selfRole = room?.participants?.firstOrNull { it.userId == currentUserId }?.role
    // Match the backend rank ladder (OWNER > ADMIN > MODERATOR > MEMBER).
    // The server is authoritative; these checks only keep inappropriate
    // Remove buttons out of the UI.
    val actorRank = when {
        ownerId == currentUserId -> 3
        selfRole.equals("ADMIN", ignoreCase = true) -> 2
        selfRole.equals("MODERATOR", ignoreCase = true) -> 1
        else -> 0
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text("Group members", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            if (loading) CircularProgressIndicator()
            room?.participants?.forEach { member ->
                val isOwner = member.userId == ownerId
                val isSelf = member.userId == currentUserId
                val role = if (isOwner) "Owner" else member.role ?: "Member"
                val name = member.user?.username?.takeIf { it.isNotBlank() }
                    ?: "User #" + member.userId
                val targetRank = when (member.role?.uppercase()) {
                    "OWNER" -> 3
                    "ADMIN" -> 2
                    "MODERATOR" -> 1
                    else -> 0
                }
                val permitted = !isOwner && !isSelf &&
                    actorRank >= 1 && targetRank < actorRank
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleMedium)
                        Text(role, style = MaterialTheme.typography.bodySmall)
                    }
                    if (permitted) {
                        TextButton(
                            enabled = !busy,
                            onClick = { confirm = member }
                        ) { Text("Remove") }
                    }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(24.dp))
        }
    }

    confirm?.let { member ->
        val name = member.user?.username ?: "this member"
        AlertDialog(
            onDismissRequest = { if (!busy) confirm = null },
            title = { Text("Remove member?") },
            text = { Text("Remove " + name + " from this group? They will lose access until invited again.") },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    busy = true
                    scope.launch {
                        try {
                            service.removeMember(roomId, member.userId)
                            confirm = null
                            refresh()
                        } catch (e: Exception) {
                            error = e.message ?: "Could not remove member."
                            confirm = null
                        } finally {
                            busy = false
                        }
                    }
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { confirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
