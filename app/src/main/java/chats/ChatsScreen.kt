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
import com.chatforia.android.tenor.TenorRepository
import com.chatforia.android.upload.UploadRepository
import com.chatforia.android.network.ApiClient
import com.chatforia.android.ria.RiaChatScreen
import com.chatforia.android.ria.RiaChatViewModel
import com.chatforia.android.ria.RiaRepository
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.ExperimentalMaterial3Api
import com.chatforia.android.random.RandomChatViewModel
import com.chatforia.android.random.RandomMatchingView
import com.chatforia.android.auth.UserDto
import com.chatforia.android.calls.AndroidCallManager
import com.chatforia.android.StartChatView
import com.chatforia.android.chats.StartChatViewModel
import com.chatforia.android.random.RandomChatSheet
import androidx.compose.ui.platform.LocalContext
import com.chatforia.android.sounds.AudioPlayerService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.res.stringResource
import com.chatforia.android.R
import com.chatforia.android.ads.AdMobBanner
import com.chatforia.android.ads.shouldShowAds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    viewModel: ChatsViewModel,
    threadViewModel: ChatThreadViewModel,
    startChatViewModel: StartChatViewModel,
    randomChatViewModel: RandomChatViewModel,
    currentUserId: Int?,
    currentUsername: String?,
    currentUser: UserDto,
    androidCallManager: AndroidCallManager,
    socketManager: SocketManager,
    tenorRepository: TenorRepository,
    uploadRepository: UploadRepository,
    apiClient: ApiClient,
    pendingOpenChatRoomId: Int? = null,
    onPendingOpenChatConsumed: () -> Unit = {},
    onMaybeShowInterstitial: () -> Unit = {}
) {
    var searchText by remember {
        mutableStateOf("")
    }

    var selectedConversation by remember {
        mutableStateOf<ConversationDto?>(null)
    }

    var showRia by remember { mutableStateOf(false) }

    var showRandomMatching by remember {
        mutableStateOf(false)
    }

    var showStartChat by remember {
        mutableStateOf(false)
    }

    var pendingDeleteConversation by remember {
        mutableStateOf<ConversationDto?>(null)
    }

    val context = LocalContext.current

    val randomState by
    randomChatViewModel.state.collectAsState()

    val conversations by
    viewModel.conversations.collectAsState()

    val isLoading by
    viewModel.isLoading.collectAsState()

    val error by
    viewModel.error.collectAsState()

    val startChatState by startChatViewModel.state.collectAsState()

    LaunchedEffect(currentUserId) {
        viewModel.configureCurrentUser(currentUserId)
    }

    LaunchedEffect(Unit) {
        viewModel.loadConversations()
    }

    LaunchedEffect(socketManager) {
        socketManager.messageUpserts.collect { messageJson ->

            viewModel.applyRealtimeMessageJson(messageJson)

            try {
                val senderId =
                    Json.parseToJsonElement(messageJson)
                        .jsonObject["sender"]
                        ?.jsonObject?.get("id")
                        ?.jsonPrimitive?.content
                        ?.toIntOrNull()

                if (
                    senderId != null &&
                    senderId != currentUserId
                ) {
                    AudioPlayerService(context)
                        .playSavedMessageTone()
                }
            } catch (_: Exception) {
            }
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

    LaunchedEffect(randomState.session) {
        val session = randomState.session ?: return@LaunchedEffect

        showRandomMatching = false

        selectedConversation =
            conversations.firstOrNull {
                it.id == session.roomId
            }

        viewModel.loadConversations()
    }

    LaunchedEffect(startChatState.openedConversation) {
        startChatState.openedConversation?.let { conversation ->
            showStartChat = false
            selectedConversation = conversation
            startChatViewModel.clearOpenedConversation()
            viewModel.loadConversations()
        }
    }

    LaunchedEffect(pendingOpenChatRoomId, conversations) {
        val chatRoomId = pendingOpenChatRoomId ?: return@LaunchedEffect

        val conversation =
            conversations.firstOrNull { it.id == chatRoomId }

        if (conversation != null) {
            showStartChat = false
            showRia = false
            showRandomMatching = false
            selectedConversation = conversation
            onPendingOpenChatConsumed()
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
            currentUsername = currentUsername,
            currentUser = currentUser,
            androidCallManager = androidCallManager,
            socketManager = socketManager,
            uploadRepository = uploadRepository,
            tenorRepository = tenorRepository,
            riaRepository = RiaRepository(apiClient),
            onBack = {
                selectedConversation = null
                viewModel.loadConversations()
            }
        )

        return
    }

    if (showRia) {
        val riaViewModel = remember {
            RiaChatViewModel(
                RiaRepository(apiClient)
            )
        }

        RiaChatScreen(
            viewModel = riaViewModel,
            memoryEnabled = true,
            filterProfanity = false,
            onClose = {
                showRia = false
                onMaybeShowInterstitial()
            }
        )

        return
    }

    if (showStartChat) {
        StartChatView(
            viewModel = startChatViewModel,
            onBack = {
                showStartChat = false
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
                text = stringResource(R.string.android_chats_chats),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = ChatforiaColors.primaryText
            )

            ChatforiaActionPill(
                actions = listOf(
                    ChatforiaAction(
                        icon = Icons.Default.Add,
                        contentDescription = stringResource(R.string.android_chats_new_chat),
                        onClick = {
                            showStartChat = true
                        }
                    ),
                    ChatforiaAction(
                        icon = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.android_chats_refresh),
                        onClick = {
                            viewModel.loadConversations()
                        }
                    ),
                    ChatforiaAction(
                        icon = Icons.Default.AutoAwesome,
                        contentDescription = "Ria",
                        onClick = {
                            showRia = true
                        }
                    ),
                    ChatforiaAction(
                        icon = Icons.Default.Shuffle,
                        contentDescription = stringResource(R.string.android_chats_random_chat),
                        onClick = {
                            showRandomMatching = true
                            randomChatViewModel.startSearch()
                        }
                    )
                )
            )
        }

        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = { Text(stringResource(R.string.android_chats_search_chats)) },

            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = stringResource(R.string.android_chats_search),
                    tint = ChatforiaColors.secondaryText
                )
            },

            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ChatforiaColors.cardBackground,
                unfocusedContainerColor = ChatforiaColors.cardBackground,
                focusedBorderColor = ChatforiaColors.border,
                unfocusedBorderColor = ChatforiaColors.border,
                focusedTextColor = ChatforiaColors.primaryText,
                unfocusedTextColor = ChatforiaColors.primaryText,
                focusedPlaceholderColor = ChatforiaColors.secondaryText,
                unfocusedPlaceholderColor = ChatforiaColors.secondaryText,
                cursorColor = ChatforiaColors.accent
            ),

            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(24.dp),

            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.android_chats_no_chats_yet))
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                itemsIndexed(
                    items = filtered,
                    key = { _, item -> item.uniqueId }
                ) { index, chat ->

                    SwipeRevealConversationRow(
                        conversation = chat,
                        onClick = {
                            selectedConversation = chat
                        },
                        onDelete = {
                            pendingDeleteConversation = chat
                        }
                    )

                    if (index < filtered.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(
                                start = 80.dp,
                                end = 56.dp
                            ),
                            thickness = 1.dp,
                            color = Color(0xFFE0DDD8)
                        )
                    }
                }
            }
        }


        if (currentUser.shouldShowAds()) {
            AdMobBanner(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    pendingDeleteConversation?.let { conversation ->
        AlertDialog(
            onDismissRequest = {
                pendingDeleteConversation = null
            },
            title = {
                Text(stringResource(R.string.android_chats_delete_conversation))
            },
            text = {
                Text(stringResource(R.string.android_chats_this_will_remove_this_conversation_from_your_cha))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteConversation = null
                        viewModel.deleteConversation(conversation)
                    }
                ) {
                    Text("Delete", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingDeleteConversation = null
                    }
                ) {
                    Text(stringResource(R.string.android_chats_cancel))
                }
            }
        )
    }

    if (showRandomMatching) {
        ModalBottomSheet(
            onDismissRequest = {
                randomChatViewModel.cancelSearch()
                showRandomMatching = false
            }
        ) {
            RandomMatchingView(
                error = randomState.error,
                onCancel = {
                    randomChatViewModel.cancelSearch()
                    showRandomMatching = false
                }
            )
        }
    }

    if (randomState.session != null) {
        ModalBottomSheet(
            onDismissRequest = {
                randomChatViewModel.endChat()
            }
        ) {
            RandomChatSheet(
                session = randomState.session!!,
                messages = randomState.messages,
                onSend = randomChatViewModel::sendMessage,
                onAddFriend = randomChatViewModel::requestFriend,
                onSkip = randomChatViewModel::skip,
                onClose = randomChatViewModel::endChat
            )
        }
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
                style = MaterialTheme.typography.titleMedium,
                color = ChatforiaColors.primaryText
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = ChatforiaColors.secondaryText
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = timestamp,
                style = MaterialTheme.typography.bodySmall,
                color = ChatforiaColors.secondaryText
            )

            if (unreadCount > 0) {
                Badge {
                    Text(unreadCount.toString())
                }
            }
        }
    }
}


@Composable
private fun SwipeRevealConversationRow(
    conversation: ConversationDto,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var offsetX by remember(conversation.uniqueId) {
        mutableFloatStateOf(0f)
    }

    val maxRevealPx = 92.dp.value * LocalDensity.current.density

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(92.dp)
                .background(Color(0xFFE53935)),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.android_chats_delete_conversation_2),
                    tint = Color.White
                )
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .background(ChatforiaColors.cardBackground)
                .pointerInput(conversation.uniqueId) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            offsetX = (offsetX + dragAmount)
                                .coerceIn(-maxRevealPx, 0f)
                        },
                        onDragEnd = {
                            offsetX = if (offsetX < -maxRevealPx / 2) {
                                -maxRevealPx
                            } else {
                                0f
                            }
                        },
                        onDragCancel = {
                            offsetX = 0f
                        }
                    )
                }
        ) {
            ConversationRow(
                conversation = conversation,
                onClick = onClick
            )
        }
    }
}