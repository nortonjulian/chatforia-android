package com.chatforia.android.chats

import com.chatforia.android.ui.theme.ChatforiaColors
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.launch
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
import androidx.compose.ui.platform.LocalContext
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
    pendingOpenSmsThreadId: Int? = null,
    onPendingOpenChatConsumed: () -> Unit = {},
    onPendingOpenSmsConsumed: () -> Unit = {},
    onMaybeShowInterstitial: () -> Unit = {},
    onRiaVisibilityChanged: (Boolean) -> Unit = {},
    onThreadVisibilityChanged: (Boolean) -> Unit = {}
) {
    var isEditing by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    var pendingBulkDeletion by remember {
        mutableStateOf<List<ConversationDto>>(emptyList())
    }
    var failedDeletionCount by remember { mutableStateOf(0) }
    val selectionScope = rememberCoroutineScope()
    val isDeleting by viewModel.isDeletingConversations.collectAsState()

    var searchText by remember {
        mutableStateOf("")
    }

    var selectedConversation by remember {
        mutableStateOf<ConversationDto?>(null)
    }

    LaunchedEffect(selectedConversation != null) {
        onThreadVisibilityChanged(selectedConversation != null)
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            onThreadVisibilityChanged(false)
        }
    }

    var showRia by remember { mutableStateOf(false) }

    LaunchedEffect(showRia) {
        onRiaVisibilityChanged(showRia)
    }

    var showRandomMatching by remember {
        mutableStateOf(false)
    }

    var showStartChat by remember {
        mutableStateOf(false)
    }

    var pendingDeleteConversation by remember {
        mutableStateOf<ConversationDto?>(null)
    }

    var activeRandomTitle by remember {
        mutableStateOf<String?>(null)
    }
    var peerEndedNotice by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val randomState by
    randomChatViewModel.state.collectAsState()

    LaunchedEffect(randomState.endedRoomId) {
        val endedId = randomState.endedRoomId ?: return@LaunchedEffect
        if (selectedConversation?.id == endedId) {
            selectedConversation = null
            activeRandomTitle = null
            peerEndedNotice = true
            viewModel.loadConversations()
        }
    }

    val conversations by
    viewModel.conversations.collectAsState()

    val isLoading by
    viewModel.isLoading.collectAsState()

    val error by
    viewModel.error.collectAsState()

    val startChatState by startChatViewModel.state.collectAsState()

    LaunchedEffect(randomState.session) {
        val session = randomState.session ?: return@LaunchedEffect

        activeRandomTitle =
            session.partnerAlias
                .takeIf { it.isNotBlank() }
                ?: "Someone"

        showRandomMatching = false

        socketManager.joinRoom(session.roomId)

        val randomTitle =
            session.partnerAlias
                .takeIf { it.isNotBlank() }
                ?: "Someone"

        selectedConversation =
            ConversationDto(
                kind = "chat",
                id = session.roomId,
                title = randomTitle,
                displayName = randomTitle,
                isRandomChat = true,
                randomChatRoomId = session.randomChatRoomId,
                randomChat = RandomChatDto(
                    chatRoomId = session.roomId,
                    id = session.randomChatRoomId,
                    myAlias = session.myAlias,
                    partnerAlias = randomTitle
                )
            )

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

    LaunchedEffect(
        pendingOpenChatRoomId,
        conversations
    ) {
        val chatRoomId =
            pendingOpenChatRoomId
                ?: return@LaunchedEffect

        val conversation =
            conversations.firstOrNull {
                it.id == chatRoomId &&
                    it.kind != "sms"
            }

        if (conversation != null) {
            showStartChat = false
            showRia = false
            showRandomMatching = false
            selectedConversation = conversation
            onPendingOpenChatConsumed()
        }
    }

    LaunchedEffect(
        pendingOpenSmsThreadId,
        conversations
    ) {
        val threadId =
            pendingOpenSmsThreadId
                ?: return@LaunchedEffect

        val conversation =
            conversations.firstOrNull {
                it.id == threadId &&
                    it.kind == "sms"
            }

        if (conversation != null) {
            showStartChat = false
            showRia = false
            showRandomMatching = false
            selectedConversation = conversation
            onPendingOpenSmsConsumed()
        }
    }

    LaunchedEffect(conversations) {
        selectedIds = selectedIds.intersect(
            conversations.map { it.uniqueId }.toSet()
        )
    }

    val showTransportBadges =
        conversations.any { it.kind.equals("sms", ignoreCase = true) } &&
        conversations.any { it.kind.equals("chat", ignoreCase = true) }

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

    val chatsListState = rememberLazyListState()
    val newestConversationKey = filtered.firstOrNull()?.uniqueId

    LaunchedEffect(newestConversationKey) {
        // Keyed LazyColumn preserves the previous first visible chat when a
        // newer conversation moves above it. Near the top, this can leave
        // the new first row partly clipped behind the list viewport edge.
        // Keep recent chats fully visible without jumping users who are
        // browsing farther down their history.
        if (newestConversationKey != null &&
            chatsListState.firstVisibleItemIndex <= 1
        ) {
            chatsListState.scrollToItem(0)
        }
    }

    selectedConversation?.let { room ->

        val isTemporaryRandomChat =
            room.isTemporaryRandomChat ||
                    randomState.session?.roomId == room.id

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
            isTemporaryRandomChat = isTemporaryRandomChat,
            randomChatTitle = activeRandomTitle,
            isRandomAlreadyFriend = randomState.session?.isAlreadyFriend == true,
            didRequestRandomFriend = randomState.session?.iRequestedFriend == true,
            onRandomAddFriend = {
                room.id?.let { socketManager.requestRandomFriend(it) }
            },
            onRandomNext = {
                randomChatViewModel.skip()
                activeRandomTitle = null
                selectedConversation = null
                showRandomMatching = true
            },
            onRandomLeave = {
                randomChatViewModel.endChat()
                activeRandomTitle = null
                selectedConversation = null
                viewModel.deleteConversation(room)
            },
            onBack = {
                val wasSmsThread = room.kind == "sms"

                if (isTemporaryRandomChat) {
                    // Back is navigation, not an explicit Random Chat Leave.
                    // Keep the active pairing so returning to this thread
                    // retains Add Friend / Next / Leave and anonymous rules.
                    selectedConversation = null
                    viewModel.loadConversations()
                    return@ChatThreadScreen
                }

                selectedConversation = null
                viewModel.loadConversations()

                if (wasSmsThread) {
                    onMaybeShowInterstitial()
                }
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
            memoryEnabled = currentUser.riaRemember ?: true,
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
                            if (isEditing || isDeleting) return@ChatforiaAction
                            showStartChat = true
                        }
                    ),
                    ChatforiaAction(
                        icon = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.android_chats_refresh),
                        onClick = {
                            if (isEditing || isDeleting) return@ChatforiaAction
                            viewModel.loadConversations()
                        }
                    ),
                    ChatforiaAction(
                        icon = Icons.Default.AutoAwesome,
                        contentDescription = "Ria",
                        onClick = {
                            if (isEditing || isDeleting) return@ChatforiaAction
                            showRia = true
                        }
                    ),
                    ChatforiaAction(
                        icon = Icons.Default.Shuffle,
                        contentDescription = stringResource(R.string.android_chats_random_chat),
                        onClick = {
                            if (isEditing || isDeleting) return@ChatforiaAction
                            showRandomMatching = true
                            randomChatViewModel.startSearch()
                        }
                    )
                )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                enabled = !isDeleting,
                onClick = {
                    isEditing = !isEditing
                    selectedIds = emptySet()
                }
            ) {
                Text(stringResource(
                    if (isEditing) R.string.chat_selection_done
                    else R.string.chat_selection_edit
                ))
            }

            if (isEditing) {
                val visibleIds = filtered.map { it.uniqueId }.toSet()
                val allSelected = visibleIds.isNotEmpty() &&
                    selectedIds.containsAll(visibleIds)

                TextButton(
                    enabled = !isDeleting && visibleIds.isNotEmpty(),
                    onClick = {
                        selectedIds = if (allSelected) {
                            selectedIds - visibleIds
                        } else {
                            selectedIds + visibleIds
                        }
                    }
                ) {
                    Text(stringResource(
                        if (allSelected) R.string.chat_selection_deselect_all
                        else R.string.chat_selection_select_all
                    ))
                }
            }
        }

        OutlinedTextField(
            enabled = !isDeleting,
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
                if (isLoading) {
                    CircularProgressIndicator(
                        color = ChatforiaColors.accent
                    )
                } else {
                    Text(stringResource(R.string.android_chats_no_chats_yet))
                }
            }
        } else {
            LazyColumn(
                state = chatsListState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                itemsIndexed(
                    items = filtered,
                    key = { _, item -> item.uniqueId }
                ) { index, chat ->

                    if (isEditing) {
                        ConversationRow(
                            conversation = chat,
                            showTransportBadge = showTransportBadges,
                            isSelected = chat.uniqueId in selectedIds,
                            enabled = !isDeleting,
                            onClick = {
                                selectedIds = if (chat.uniqueId in selectedIds) {
                                    selectedIds - chat.uniqueId
                                } else {
                                    selectedIds + chat.uniqueId
                                }
                            }
                        )
                    } else {
                        SwipeRevealConversationRow(
                            conversation = chat,
                            showTransportBadge = showTransportBadges,
                            onClick = {
                                selectedConversation = chat
                            },
                            onDelete = {
                                pendingDeleteConversation = chat
                            }
                        )
                    }

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


        if (isEditing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    enabled = selectedIds.isNotEmpty() && !isDeleting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    onClick = {
                        pendingBulkDeletion = conversations.filter {
                            it.uniqueId in selectedIds
                        }
                    }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(
                        R.string.chat_selection_delete_count,
                        selectedIds.size
                    ))
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

    if (pendingBulkDeletion.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { pendingBulkDeletion = emptyList() },
            title = {
                Text(stringResource(
                    R.string.chat_selection_confirm_title,
                    pendingBulkDeletion.size
                ))
            },
            text = {
                Text(stringResource(R.string.chat_selection_confirm_body))
            },
            confirmButton = {
                TextButton(onClick = {
                    val snapshot = pendingBulkDeletion
                    pendingBulkDeletion = emptyList()
                    selectionScope.launch {
                        val failed = viewModel.deleteConversations(snapshot)
                        selectedIds = failed
                        failedDeletionCount = failed.size
                        if (failed.isEmpty()) isEditing = false
                    }
                }) {
                    Text(
                        stringResource(R.string.chat_selection_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingBulkDeletion = emptyList()
                }) {
                    Text(stringResource(R.string.android_chats_cancel))
                }
            }
        )
    }

    if (failedDeletionCount > 0) {
        AlertDialog(
            onDismissRequest = { failedDeletionCount = 0 },
            title = {
                Text(stringResource(R.string.chat_selection_failed_title))
            },
            text = {
                Text(stringResource(
                    R.string.chat_selection_failed_body,
                    failedDeletionCount
                ))
            },
            confirmButton = {
                TextButton(onClick = { failedDeletionCount = 0 }) {
                    Text(stringResource(R.string.chat_selection_done))
                }
            }
        )
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

    if (peerEndedNotice) {
        AlertDialog(
            onDismissRequest = { peerEndedNotice = false },
            title = { Text("Random Chat ended") },
            text = { Text("Your partner left the Random Chat.") },
            confirmButton = {
                TextButton(onClick = { peerEndedNotice = false }) {
                    Text("OK")
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
    onClick: () -> Unit,
    isSelected: Boolean? = null,
    enabled: Boolean = true,
    showTransportBadge: Boolean
) {
    val title = conversation.randomAwareTitle

    val last = conversation.last

    val baseSubtitle = formatConversationPreview(last)

    val senderName =
        last?.senderName
            ?.trim()
            .orEmpty()

    val hasMessagePreview =
        !last?.text.isNullOrBlank() ||
                last?.hasMedia == true

    val subtitle =
        if (
            conversation.isGroup == true &&
            senderName.isNotEmpty() &&
            hasMessagePreview
        ) {
            "$senderName: $baseSubtitle"
        } else {
            baseSubtitle
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
            .then(
                if (isSelected != null) {
                    Modifier.selectable(
                        selected = isSelected,
                        enabled = enabled,
                        role = Role.Checkbox,
                        onClick = onClick
                    )
                } else {
                    Modifier.clickable(enabled = enabled, onClick = onClick)
                }
            )
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelected != null) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = null,
                enabled = enabled
            )
            Spacer(Modifier.width(8.dp))
        }

        ChatforiaAvatar(
            name = title,
            size = 48.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = ChatforiaColors.primaryText,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (showTransportBadge) {
                    Text(
                        text = if (conversation.kind.equals("sms", ignoreCase = true)) "SMS" else "Chatforia",
                        style = MaterialTheme.typography.labelSmall,
                        color = ChatforiaColors.accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(ChatforiaColors.accent.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

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
    onDelete: () -> Unit,
    showTransportBadge: Boolean
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
                onClick = onClick,
                showTransportBadge = showTransportBadge
            )
        }
    }
}