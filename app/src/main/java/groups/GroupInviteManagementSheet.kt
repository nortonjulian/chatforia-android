package com.chatforia.android.groups

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInviteManagementSheet(
    roomId: Int,
    userId: Int,
    service: GroupInviteManagementService,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var permitted by remember(roomId, userId) { mutableStateOf(false) }
    var loading by remember(roomId, userId) { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var expires by remember { mutableStateOf(1440) }
    var maxUsesText by remember { mutableStateOf("0") }
    var created by remember { mutableStateOf<GroupInviteCreated?>(null) }
    var existing by remember { mutableStateOf<List<GroupInviteRecord>>(emptyList()) }

    suspend fun refresh() {
        try {
            existing = service.list(roomId)
            error = null
        } catch (e: Exception) {
            error = e.message ?: "Could not load invitations."
        }
    }

    LaunchedEffect(roomId, userId) {
        try {
            permitted = service.canManage(roomId, userId)
            if (permitted) refresh()
        } catch (e: Exception) {
            error = e.message ?: "Could not verify group permissions."
        } finally {
            loading = false
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text("Invite people", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            when {
                loading -> CircularProgressIndicator()
                !permitted -> Text(error ?: "Only group owners or admins may manage invitations.")
                else -> {
                    Text("Expires in", style = MaterialTheme.typography.titleMedium)
                    listOf(30 to "30 minutes", 1440 to "1 day",
                        10080 to "7 days", 0 to "Never").forEach { (minutes, label) ->
                        Row {
                            RadioButton(selected = expires == minutes, onClick = { expires = minutes })
                            TextButton(onClick = { expires = minutes }) { Text(label) }
                        }
                    }
                    OutlinedTextField(
                        value = maxUsesText,
                        onValueChange = { value ->
                            if (value.length <= 5 && value.all(Char::isDigit)) maxUsesText = value
                        },
                        label = { Text("Maximum uses (0 = unlimited)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    val limit = maxUsesText.toIntOrNull()
                    Button(
                        enabled = !busy && limit != null && limit in 0..10000,
                        onClick = {
                            val n = limit ?: return@Button
                            busy = true
                            scope.launch {
                                try {
                                    created = service.create(roomId, expires, n)
                                    refresh()
                                } catch (e: Exception) {
                                    error = e.message ?: "Could not create invitation."
                                } finally {
                                    busy = false
                                }
                            }
                        }
                    ) { Text(if (busy) "Working…" else "Generate link") }
                    created?.let { invite ->
                        Spacer(Modifier.height(12.dp))
                        Text(invite.url, style = MaterialTheme.typography.bodySmall)
                        Row {
                            TextButton(onClick = {
                                clipboard.setText(AnnotatedString(invite.url))
                            }) { Text("Copy link") }
                            TextButton(onClick = {
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, invite.url)
                                }
                                context.startActivity(Intent.createChooser(share, "Share group invitation"))
                            }) { Text("Share") }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("Existing invitations", style = MaterialTheme.typography.titleMedium)
                    if (existing.isEmpty()) Text("No invitations yet.")
                    existing.forEach { invite ->
                        val status = if (invite.isExhausted) "Exhausted" else "Created"
                        HorizontalDivider()
                        Text(status + " • Uses " + invite.uses + "/" +
                            (if (invite.maxUses == 0) "unlimited" else invite.maxUses.toString()),
                            style = MaterialTheme.typography.bodySmall)
                        invite.expiresAt?.let { Text("Expires: " + it, style = MaterialTheme.typography.bodySmall) }
                        TextButton(enabled = !busy, onClick = {
                            busy = true
                            scope.launch {
                                try {
                                    service.revoke(roomId, invite.code)
                                    if (created?.code == invite.code) created = null
                                    refresh()
                                } catch (e: Exception) {
                                    error = e.message ?: "Could not revoke invitation."
                                } finally {
                                    busy = false
                                }
                            }
                        }) { Text("Revoke") }
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}
