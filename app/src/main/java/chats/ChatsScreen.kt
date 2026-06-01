package com.chatforia.android.chats

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
import com.chatforia.android.ui.components.ChatforiaAction
import com.chatforia.android.ui.components.ChatforiaActionPill
import com.chatforia.android.ui.components.ChatforiaAvatar
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.clickable
import com.chatforia.android.messages.ChatThreadScreen
import com.chatforia.android.messages.ChatThreadViewModel
import com.chatforia.android.socket.SocketManager

@Composable
fun ChatsScreen(
    viewModel: ChatsViewModel,
    threadViewModel: ChatThreadViewModel,
    currentUserId: Int?,
    socketManager: SocketManager
) {
    var searchText by remember {
        mutableStateOf("")
    }

    var selectedConversation by remember {
        mutableStateOf<ConversationDto?>(null)
    }

    val conversations by
    viewModel.conversations.collectAsState()

    val isLoading by
    viewModel.isLoading.collectAsState()

    val error by
    viewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadConversations()
    }

    LaunchedEffect(socketManager) {
        socketManager.messageUpserts.collect { messageJson ->
            viewModel.applyRealtimeMessageJson(messageJson)
        }
    }

    LaunchedEffect(conversations) {
        conversations
            .filter { it.kind.equals("chat", ignoreCase = true) }
            .mapNotNull { it.id }
            .forEach { roomId ->
                socketManager.joinRoom(roomId)
            }
    }

    val filtered =
        conversations.filter {

            val title =
                it.displayName
                    ?: it.title

            val lastText =
                it.last?.text
                    ?: ""

            title.contains(
                searchText,
                ignoreCase = true
            ) ||
                    lastText.contains(
                        searchText,
                        ignoreCase = true
                    )
        }

    selectedConversation?.let { room ->

        ChatThreadScreen(
            conversation = room,
            viewModel = threadViewModel,
            currentUserId = currentUserId,
            socketManager = socketManager,
            onBack = {
                selectedConversation = null
                viewModel.loadConversations()
            }
        )

        return
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

            ChatforiaActionPill(
                actions = listOf(
                    ChatforiaAction(
                        icon = Icons.Default.Add,
                        contentDescription = "Start chat"
                    ),
                    ChatforiaAction(
                        icon = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        onClick = {
                            viewModel.loadConversations()
                        }
                    ),
                    ChatforiaAction(
                        icon = Icons.Default.AutoAwesome,
                        contentDescription = "Ria"
                    ),
                    ChatforiaAction(
                        icon = Icons.Default.Shuffle,
                        contentDescription = "Random chat"
                    )
                )
            )
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
                        ConversationRow(
                            conversation = chat,

                            onClick = {
                                selectedConversation = chat
                            }
                        )
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

private fun formatConversationTime(
    isoString: String?
): String {
    if (isoString.isNullOrBlank()) {
        return ""
    }

    return try {
        val instant =
            Instant.parse(isoString)

        val zone =
            ZoneId.systemDefault()

        val date =
            instant.atZone(zone).toLocalDate()

        val today =
            LocalDate.now(zone)

        when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else ->
                date.format(
                    DateTimeFormatter.ofPattern("MMM d")
                )
        }
    } catch (error: Exception) {
        ""
    }
}

@Composable
private fun ConversationRow(
    conversation: ConversationDto,
    onClick: () -> Unit
) {
    val title =
        conversation.displayName
            ?: conversation.title

    val subtitle =
        conversation.last?.text
            ?: if (conversation.last?.hasMedia == true) {
                "[media]"
            } else {
                "No messages yet"
            }

    val timestamp =
        formatConversationTime(
            conversation.last?.at
                ?: conversation.updatedAt
        )

    val unreadCount =
        conversation.unreadCount ?: 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChatforiaAvatar(
            name = title,
            size = 48.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = timestamp,
                style = MaterialTheme.typography.bodySmall
            )

            if (unreadCount > 0) {
                Badge {
                    Text(unreadCount.toString())
                }
            }
        }
    }
}