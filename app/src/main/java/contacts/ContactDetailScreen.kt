package com.chatforia.android.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chatforia.android.ui.theme.ChatforiaColors
import androidx.compose.ui.res.stringResource
import com.chatforia.android.R

@Composable
fun ContactDetailScreen(
    contact: ContactDto,
    displayName: String,
    subtitle: String,
    onMessage: () -> Unit,
    onCall: () -> Unit,
    onVideo: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    isAccountBlocked: Boolean,
    onToggleAccountBlock: () -> Unit,
) {
    var showContactMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatforiaColors.screenBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.android_plan_back),
                    tint = ChatforiaColors.primaryText
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.android_contact_detail_edit_contact),
                        tint = ChatforiaColors.accent
                    )
                }

                Box {
                    IconButton(onClick = { showContactMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More contact options",
                            tint = ChatforiaColors.primaryText
                        )
                    }
                    DropdownMenu(
                        expanded = showContactMenu,
                        onDismissRequest = { showContactMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.android_contact_detail_delete_contact)) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = {
                                showContactMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

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
            title = stringResource(R.string.android_contact_detail_message),
            icon = Icons.Default.Message,
            onClick = onMessage
        )

        ContactActionRow(
            title = stringResource(R.string.android_dial_pad_call),
            icon = Icons.Default.Call,
            onClick = onCall
        )

        if (contact.user?.id != null || contact.userId != null) {
            ContactActionRow(
                title = stringResource(R.string.android_contact_detail_video),
                icon = Icons.Default.Videocam,
                onClick = onVideo
            )
            ContactActionRow(
                title = stringResource(
                    if (isAccountBlocked) R.string.android_blocked_accounts_unblock
                    else R.string.android_block_chatforia_account
                ),
                icon = Icons.Default.Block,
                onClick = onToggleAccountBlock
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
                    title = stringResource(R.string.android_add_contact_name),
                    value = displayName,
                    icon = Icons.Default.Person
                )

                contact.externalName?.takeIf { it.isNotBlank() }?.let {
                    ContactDetailLine(
                        title = stringResource(R.string.android_contact_detail_external_name),
                        value = it,
                        icon = Icons.Default.Person
                    )
                }

                contact.externalPhone?.takeIf { it.isNotBlank() }?.let {
                    ContactDetailLine(
                        title = stringResource(R.string.android_add_contact_phone),
                        value = it,
                        icon = Icons.Default.Phone
                    )
                }

                contact.user?.username?.takeIf { it.isNotBlank() }?.let {
                    ContactDetailLine(
                        title = stringResource(R.string.android_profile_username),
                        value = "@$it",
                        icon = Icons.Default.Person
                    )
                }
            }
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