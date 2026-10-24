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
    val manager = ownerId == currentUserId ||
        selfRole.equals("ADMIN", ignoreCase = true)
    val canModerate = manager ||
        selfRole.equals("MODERATOR", ignoreCase = true)

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
                val targetRole = member.role?.uppercase() ?: "MEMBER"
                val permitted = canModerate && !isOwner && !isSelf &&
                    (ownerId == currentUserId ||
                        (targetRole == "MEMBER" && manager))
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
