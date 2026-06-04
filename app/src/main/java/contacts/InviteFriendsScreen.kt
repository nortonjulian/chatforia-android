package com.chatforia.android.contacts

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatforia.android.ui.theme.ChatforiaColors
import kotlinx.coroutines.launch

@Composable
fun InviteFriendsScreen(
    repository: InviteRepository,
    currentUsername: String?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun shareText(inviteUrl: String): String {
        val name = currentUsername?.trim()?.takeIf { it.isNotBlank() }

        return if (name != null) {
            "$name invited you to join Chatforia. Download or join here: $inviteUrl"
        } else {
            "Join me on Chatforia. Download or join here: $inviteUrl"
        }
    }

    fun createAndShareInvite() {
        coroutineScope.launch {
            isLoading = true
            error = null

            try {
                val response =
                    repository.createInvite(
                        channel = "share_link"
                    )

                val sendIntent =
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            shareText(response.url)
                        )
                    }

                val chooser =
                    Intent.createChooser(
                        sendIntent,
                        "Invite friends"
                    )

                context.startActivity(chooser)

            } catch (e: Exception) {
                error = e.message ?: "Failed to create invite."
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatforiaColors.screenBackground)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = ChatforiaColors.primaryText
                )
            }

            Text(
                text = "Invite Friends",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = ChatforiaColors.primaryText
            )
        }

        Text(
            text = "Share a Chatforia invite link with friends using Messages, email, or any app on your phone.",
            color = ChatforiaColors.secondaryText,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { createAndShareInvite() },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text("Share Invite Link")
            }
        }

        error?.let {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}