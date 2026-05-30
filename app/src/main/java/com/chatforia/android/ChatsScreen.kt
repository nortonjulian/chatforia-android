package com.chatforia.android

import com.chatforia.android.ui.theme.ChatforiaColors
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon

data class ChatPreview(
    val title: String,
    val subtitle: String,
    val timestamp: String,
    val unreadCount: Int = 0
)

@Composable
fun ChatsScreen() {
    var searchText by remember { mutableStateOf("") }

    val conversations = listOf(
        ChatPreview("Ria", "How can I help?", "Now", 1),
        ChatPreview("Welcome to Chatforia", "Start your first conversation", "Today"),
        ChatPreview("Random Chat", "Meet someone new", "Today")
    )

    val filtered = conversations.filter {
        it.title.contains(searchText, ignoreCase = true) ||
                it.subtitle.contains(searchText, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatforiaColors.screenBackground)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Chats",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = ChatforiaColors.primaryText
            )

            Surface(
                shape = RoundedCornerShape(28.dp),
                color = ChatforiaColors.highlightedSurface,
                tonalElevation = 2.dp
            ) {

                Row(
                    modifier = Modifier.padding(
                        horizontal = 14.dp,
                        vertical = 8.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Start chat",
                        tint = ChatforiaColors.accent
                    )

                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = ChatforiaColors.accent
                    )

                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "Ria",
                        tint = ChatforiaColors.accent
                    )

                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "Random chat",
                        tint = ChatforiaColors.accent
                    )
                }
            }
        }

        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = { Text("Search chats") },

            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = ChatforiaColors.secondaryText
                )
            },

            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ChatforiaColors.cardBackground,
                unfocusedContainerColor = ChatforiaColors.cardBackground,
                focusedBorderColor = ChatforiaColors.border,
                unfocusedBorderColor = ChatforiaColors.border
            ),

            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(24.dp),

            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No chats yet")
            }
        } else {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = ChatforiaColors.cardBackground,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyColumn(
                    modifier = Modifier.padding(12.dp)
                ) {
                    items(filtered) { chat ->
                        ChatPreviewRow(chat)
                        HorizontalDivider()
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 12.dp),
            tonalElevation = 1.dp,
            shape = RoundedCornerShape(2.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("AdMob Ad")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun ChatPreviewRow(chat: ChatPreview) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = MaterialTheme.shapes.large,
            color = ChatforiaColors.highlightedSurface
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(chat.title.first().uppercase())
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(chat.title, style = MaterialTheme.typography.titleMedium)
            Text(chat.subtitle, style = MaterialTheme.typography.bodyMedium)
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(chat.timestamp, style = MaterialTheme.typography.bodySmall)

            if (chat.unreadCount > 0) {
                Badge {
                    Text(chat.unreadCount.toString())
                }
            }
        }
    }
}