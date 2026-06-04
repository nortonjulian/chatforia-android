package com.chatforia.android.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chatforia.android.ui.theme.ChatforiaColors

@Composable
fun ContactDetailScreen(
    contact: ContactDto,
    displayName: String,
    subtitle: String,
    onMessage: () -> Unit,
    onCall: () -> Unit,
    onVideo: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatforiaColors.screenBackground)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(88.dp),
            shape = CircleShape,
            color = ChatforiaColors.highlightedSurface
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = displayName.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = ChatforiaColors.primaryText
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = displayName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = ChatforiaColors.primaryText
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = ChatforiaColors.secondaryText
        )

        Spacer(modifier = Modifier.height(24.dp))

        ContactActionRow(
            title = "Message",
            icon = Icons.Default.Message,
            onClick = onMessage
        )

        ContactActionRow(
            title = "Call",
            icon = Icons.Default.Call,
            onClick = onCall
        )

        if (contact.user?.id != null || contact.userId != null) {
            ContactActionRow(
                title = "Video",
                icon = Icons.Default.Videocam,
                onClick = onVideo
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ChatforiaColors.cardBackground,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ContactDetailLine(
                    title = "Name",
                    value = displayName,
                    icon = Icons.Default.Person
                )

                contact.externalName?.takeIf { it.isNotBlank() }?.let {
                    ContactDetailLine(
                        title = "External name",
                        value = it,
                        icon = Icons.Default.Person
                    )
                }

                contact.externalPhone?.takeIf { it.isNotBlank() }?.let {
                    ContactDetailLine(
                        title = "Phone",
                        value = it,
                        icon = Icons.Default.Phone
                    )
                }

                contact.user?.username?.takeIf { it.isNotBlank() }?.let {
                    ContactDetailLine(
                        title = "Username",
                        value = "@$it",
                        icon = Icons.Default.Person
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Delete Contact")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}

@Composable
private fun ContactActionRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = ChatforiaColors.cardBackground,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = ChatforiaColors.accent
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = ChatforiaColors.primaryText
            )
        }
    }
}

@Composable
private fun ContactDetailLine(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = title,
            tint = ChatforiaColors.accent,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = ChatforiaColors.secondaryText
            )

            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = ChatforiaColors.primaryText
            )
        }
    }
}