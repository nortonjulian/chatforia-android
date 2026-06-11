package com.chatforia.android.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.chatforia.android.chats.ConversationDto
import com.chatforia.android.pickers.GifPickerSheet
import com.chatforia.android.pickers.MediaPickerSheet
import com.chatforia.android.socket.SocketManager
import com.chatforia.android.ui.theme.ChatforiaColors
import com.chatforia.android.tenor.TenorRepository

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import com.chatforia.android.upload.UploadRepository
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import com.chatforia.android.auth.UserDto
import com.chatforia.android.calls.AndroidCallManager
import com.chatforia.android.ui.components.ChatforiaAction
import com.chatforia.android.ui.components.ChatforiaActionPill
import kotlinx.coroutines.CoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.FilledIconButton
import androidx.compose.material.icons.filled.AutoAwesome
import com.chatforia.android.ria.RiaRepository
import com.chatforia.android.ria.RiaRewriteSheet
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadScreen(
    conversation: ConversationDto,
    viewModel: ChatThreadViewModel,
    currentUserId: Int?,
    currentUsername: String?,
    currentUser: UserDto,
    androidCallManager: AndroidCallManager,
    socketManager: SocketManager,
    uploadRepository: UploadRepository,
    tenorRepository: TenorRepository,
    riaRepository: RiaRepository,
    onBack: () -> Unit
) {
    val chatMessages by viewModel.messages.collectAsState()
    val smsMessages by viewModel.smsMessages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val error by viewModel.error.collectAsState()

    var pendingGifUrl by remember { mutableStateOf<String?>(null) }
    var pendingGifPreviewUrl by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    var deletingMessage by remember { mutableStateOf<MessageDto?>(null) }
    var editingMessage by remember { mutableStateOf<MessageDto?>(null) }

    var reportingMessage by remember { mutableStateOf<MessageDto?>(null) }
    var receiptMessage by remember { mutableStateOf<MessageDto?>(null) }

    var showEditSheet by remember { mutableStateOf(false) }
    var editDraft by remember { mutableStateOf("") }
    var editGifUrl by remember { mutableStateOf<String?>(null) }
    var showEditGifPicker by remember { mutableStateOf(false) }

    var draft by remember { mutableStateOf("") }
    var showMediaPicker by remember { mutableStateOf(false) }
    var showGifPicker by remember { mutableStateOf(false) }

    var showRewriteSheet by remember { mutableStateOf(false) }

    var rewriteOptions by remember {
        mutableStateOf<List<String>>(emptyList())
    }

    var rewriteLoading by remember {
        mutableStateOf(false)
    }

    var rewriteError by remember {
        mutableStateOf<String?>(null)
    }

    var isRecordingVoice by remember { mutableStateOf(false) }

    var voiceDraft by remember { mutableStateOf<VoiceNoteDraft?>(null) }

    val context = LocalContext.current

    val recorder = remember(context) {
        AudioRecorderService(context)
    }

    var showSearchSheet by remember { mutableStateOf(false) }
    var threadSearchText by remember { mutableStateOf("") }

    val isThreadEmpty =
        if (conversation.kind == "sms") {
            smsMessages.isEmpty()
        } else {
            chatMessages.isEmpty()
        }

    val scope = rememberCoroutineScope()

    val photoPicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            scope.launch {
                val uploaded = uploadRepository.uploadMedia(uri)

                viewModel.sendMedia(
                    conversation = conversation,
                    mediaUrls = listOf(uploaded.url)
                )
            }
        }

    val videoPicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            scope.launch {
                val uploaded = uploadRepository.uploadMedia(uri)

                viewModel.sendMedia(
                    conversation = conversation,
                    mediaUrls = listOf(uploaded.url)
                )
            }
        }

    LaunchedEffect(conversation.id, conversation.kind, currentUserId) {
        val userId = currentUserId ?: return@LaunchedEffect

        viewModel.loadConversation(
            conversation = conversation,
            currentUserId = userId
        )

        if (conversation.kind == "sms") {
            viewModel.connectSmsRealtime(socketManager)
        } else {
            conversation.id?.let { roomId ->
                viewModel.connectRealtime(
                    roomId = roomId,
                    socketManager = socketManager,
                    currentUserId = userId
                )
            }
        }
    }

    val latestMessageKey =
        if (conversation.kind == "sms") {
            smsMessages.lastOrNull()?.id
        } else {
            chatMessages.lastOrNull()?.id
        }

    LaunchedEffect(
        conversation.id,
        conversation.kind,
        isLoading,
        latestMessageKey
    ) {
        if (isLoading) return@LaunchedEffect

        val itemCount =
            if (conversation.kind == "sms") {
                smsMessages.size
            } else {
                chatMessages.size
            }

        if (itemCount <= 0) return@LaunchedEffect

        // First jump immediately.
        listState.scrollToItem(itemCount - 1)

        // Then jump again after layout settles.
        kotlinx.coroutines.delay(150)
        listState.scrollToItem(itemCount - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        conversation.displayName
                            ?: conversation.title
                            ?: "Chat"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    ChatforiaActionPill(
                        modifier = Modifier.padding(end = 10.dp),
                        actions = listOf(
                            ChatforiaAction(
                                icon = Icons.Default.Search,
                                contentDescription = "Search",
                                onClick = {
                                    showSearchSheet = true
                                }
                            ),
                            ChatforiaAction(
                                icon = Icons.Default.Call,
                                contentDescription = "Audio Call",
                                onClick = {
                                    conversation.phone?.let { phone ->
                                        androidCallManager.startPhoneCall(phone)
                                        return@ChatforiaAction
                                    }

                                    val callee = conversation.avatarUsers
                                        ?.firstOrNull { it.id != currentUser.id }

                                    if (callee != null) {
                                        androidCallManager.startAudioCall(
                                            calleeId = callee.id,
                                            displayName = callee.displayName
                                                ?: callee.username
                                                ?: conversation.displayName
                                                ?: conversation.title
                                        )
                                    }
                                }
                            ),
                            ChatforiaAction(
                                icon = Icons.Default.Videocam,
                                contentDescription = "Video Call",
                                onClick = {
                                    val callee = conversation.avatarUsers
                                        ?.firstOrNull { it.id != currentUser.id }

                                    if (
                                        conversation.kind != "sms" &&
                                        conversation.isGroup != true &&
                                        callee != null
                                    ) {
                                        androidCallManager.startVideoCall(
                                            currentUser = currentUser,
                                            calleeId = callee.id,
                                            displayName = callee.displayName
                                                ?: callee.username
                                                ?: conversation.displayName
                                                ?: conversation.title,
                                            chatRoomId = conversation.id
                                        )
                                    }
                                }
                            )
                        )
                    )
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ChatforiaColors.screenBackground)
                .padding(padding)
                .imePadding()
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier.weight(1f)
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = ChatforiaColors.accent
                            )
                        }
                    }

                    isThreadEmpty -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No messages yet")
                        }
                    }

                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(
                                10.dp,
                                Alignment.Bottom
                            )
                        ) {
                            if (conversation.kind == "sms") {
                                items(
                                    items = smsMessages,
                                    key = { message -> message.id }
                                ) { message ->
                                    SmsMessageBubble(
                                        message = message,
                                        isMine = message.isOutgoing
                                    )
                                }
                            } else {
                                items(
                                    items = chatMessages,
                                    key = { message ->
                                        if (message.id > 0) {
                                            "server-${message.id}"
                                        } else {
                                            "client-${message.clientMessageId ?: message.id}"
                                        }
                                    }
                                ) { message ->
                                    ChatMessageRow(
                                        message = message,
                                        isMine = message.sender.id == currentUserId,
                                        onEdit = { selected ->
                                            editingMessage = selected

                                            editDraft =
                                                selected.decryptedContent
                                                    ?: selected.translatedForMe
                                                            ?: selected.rawContent
                                                            ?: selected.content
                                                            ?: selected.attachments.firstOrNull { !it.caption.isNullOrBlank() }?.caption
                                                            ?: selected.attachmentsInline.firstOrNull { !it.caption.isNullOrBlank() }?.caption
                                                            ?: ""

                                            editGifUrl =
                                                (selected.attachments + selected.attachmentsInline)
                                                    .firstOrNull {
                                                        it.kind.uppercase() == "GIF" ||
                                                                it.mimeType.orEmpty().lowercase() == "image/gif"
                                                    }
                                                    ?.url

                                            showEditSheet = true
                                        },
                                        onDelete = { selected ->
                                            deletingMessage = selected
                                        },
                                        onReport = { selected ->
                                            reportingMessage = selected
                                        },
                                        onMessageInfo = { selected ->
                                            receiptMessage = selected
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!error.isNullOrBlank()) {
                Text(
                    text = error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (pendingGifPreviewUrl != null) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .width(180.dp)
                        .height(130.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ChatforiaColors.cardBackground)
                ) {
                    AsyncImage(
                        model = pendingGifPreviewUrl,
                        contentDescription = "Selected GIF",
                        modifier = Modifier.fillMaxSize()
                    )

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        IconButton(
                            onClick = {
                                pendingGifUrl = null
                                pendingGifPreviewUrl = null
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove GIF",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            if (showEditSheet && editingMessage != null) {
                EditMessageSheet(
                    initialText = editDraft,
                    initialGifUrl = editGifUrl,
                    onCancel = {
                        showEditSheet = false
                        editingMessage = null
                        editDraft = ""
                        editGifUrl = null
                    },
                    onSave = { text, gifUrl ->
                        editingMessage?.let { message ->
                            viewModel.editMessage(
                                message = message,
                                text = text,
                                gifUrl = gifUrl
                            )
                        }

                        showEditSheet = false
                        editingMessage = null
                        editDraft = ""
                        editGifUrl = null
                    },
                    onGifTap = {
                        showEditGifPicker = true
                    }
                )
            }

            val canSendText = draft.trim().isNotEmpty() || pendingGifUrl != null
            val canSendVoice = voiceDraft != null

            val smartRepliesEnabled = currentUser.enableSmartReplies == true
            val riaAvailable = smartRepliesEnabled && draft.isNotBlank()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { showMediaPicker = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Attach"
                    )
                }

                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text("Message")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Rewrite with Ria",
                            tint =
                                if (riaAvailable) {
                                    ChatforiaColors.accent.copy(alpha = 0.85f)
                                } else {
                                    ChatforiaColors.secondaryText.copy(alpha = 0.45f)
                                },
                            modifier = Modifier
                                .size(16.dp)
                                .padding(start = 4.dp)
                                .clickable(enabled = riaAvailable) {
                                    showRewriteSheet = true
                                }
                        )
                    },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            sendDraftMessage(
                                draft = draft,
                                pendingGifUrl = pendingGifUrl,
                                conversation = conversation,
                                viewModel = viewModel,
                                currentUserId = currentUserId,
                                currentUsername = currentUsername,
                                onSent = {
                                    draft = ""
                                    pendingGifUrl = null
                                    pendingGifPreviewUrl = null
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }
                            )
                        }
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                FilledIconButton(
                    onClick = {
                        when {
                            isRecordingVoice -> {
                                recorder.stop()?.let {
                                    voiceDraft = it
                                }
                                isRecordingVoice = false
                            }

                            canSendVoice -> {
                                sendVoiceNote(
                                    voiceDraft = voiceDraft!!,
                                    conversation = conversation,
                                    viewModel = viewModel,
                                    uploadRepository = uploadRepository,
                                    scope = scope,
                                    onSent = {
                                        voiceDraft = null
                                        draft = ""
                                        pendingGifUrl = null
                                        pendingGifPreviewUrl = null
                                    }
                                )
                            }

                            canSendText -> {
                                sendDraftMessage(
                                    draft = draft,
                                    pendingGifUrl = pendingGifUrl,
                                    conversation = conversation,
                                    viewModel = viewModel,
                                    currentUserId = currentUserId,
                                    currentUsername = currentUsername,
                                    onSent = {
                                        draft = ""
                                        pendingGifUrl = null
                                        pendingGifPreviewUrl = null
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                    }
                                )
                            }

                            else -> {
                                scope.launch {
                                    try {
                                        recorder.start()
                                        isRecordingVoice = true
                                    } catch (_: Exception) {
                                        isRecordingVoice = false
                                    }
                                }
                            }
                        }
                    },
                    enabled = conversation.id != null && !isSending,
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = ChatforiaColors.accent,
                        contentColor = ChatforiaColors.outgoingBubbleText
                    )
                ) {
                    Icon(
                        imageVector =
                            when {
                                isRecordingVoice -> Icons.Default.Stop
                                canSendText || canSendVoice -> Icons.Default.ArrowUpward
                                else -> Icons.Default.Mic
                            },
                        contentDescription = "Composer action"
                    )
                }
            }
            if (deletingMessage != null) {
                AlertDialog(
                    onDismissRequest = { deletingMessage = null },
                    title = { Text("Delete message?") },
                    text = { Text("Choose how you want to delete this message.") },
                    confirmButton = {
                        val canDeleteForEveryone =
                            deletingMessage?.let { message ->
                                message.sender.id == currentUserId &&
                                        message.deletedForAll != true &&
                                        message.isWithinActionWindow()
                            } == true

                        if (canDeleteForEveryone) {
                            TextButton(
                                onClick = {
                                    deletingMessage?.let {
                                        viewModel.deleteMessage(it, deleteForEveryone = true)
                                    }
                                    deletingMessage = null
                                }
                            ) {
                                Text("Delete for everyone")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                deletingMessage?.let { viewModel.deleteMessage(it, deleteForEveryone = false) }
                                deletingMessage = null
                            }
                        ) {
                            Text("Delete for me")
                        }
                    }
                )
            }

            if (showMediaPicker) {
                MediaPickerSheet(
                    onDismiss = { showMediaPicker = false },
                    onPickPhoto = {
                        showMediaPicker = false

                        photoPicker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    onPickVideo = {
                        showMediaPicker = false

                        videoPicker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.VideoOnly
                            )
                        )
                    },
                    onPickGif = {
                        showMediaPicker = false
                        showGifPicker = true
                    }
                )
            }

            if (showSearchSheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showSearchSheet = false
                        threadSearchText = ""
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Search in chat",
                            style = MaterialTheme.typography.titleLarge,
                            color = ChatforiaColors.primaryText
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        OutlinedTextField(
                            value = threadSearchText,
                            onValueChange = { threadSearchText = it },
                            placeholder = { Text("Search messages") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(28.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (showEditGifPicker) {
                GifPickerSheet(
                    tenorRepository = tenorRepository,
                    onDismiss = {
                        showEditGifPicker = false
                    },
                    onGifSelected = { gif ->
                        editGifUrl = gif.url
                        showEditGifPicker = false
                    }
                )
            }

            if (showGifPicker) {
                GifPickerSheet(
                    tenorRepository = tenorRepository,
                    onDismiss = {
                        showGifPicker = false
                    },
                    onGifSelected = { gif ->
                        showGifPicker = false
                        pendingGifUrl = gif.url
                        pendingGifPreviewUrl = gif.previewUrl ?: gif.url
                    }
                )
            }

            if (showRewriteSheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showRewriteSheet = false
                    }
                ) {
                    RiaRewriteSheet(
                        draft = draft,
                        isLoading = rewriteLoading,
                        options = rewriteOptions,
                        errorText = rewriteError,
                        disabledReason = null,
                        onDismiss = {
                            showRewriteSheet = false
                        },
                        onToneTap = { tone ->
                            scope.launch {
                                rewriteLoading = true
                                rewriteError = null

                                try {
                                    rewriteOptions =
                                        riaRepository.rewriteText(
                                            text = draft,
                                            tone = tone,
                                            filterProfanity = false
                                        )
                                } catch (e: Exception) {
                                    rewriteError = e.message ?: "Failed to rewrite."
                                } finally {
                                    rewriteLoading = false
                                }
                            }
                        },
                        onSelectRewrite = { option ->
                            draft = option
                            showRewriteSheet = false
                        }
                    )
                }
            }

            if (reportingMessage != null) {
                ReportMessageSheet(
                    message = reportingMessage!!,
                    onCancel = {
                        reportingMessage = null
                    },
                    onSubmit = { reason, details, contextCount, blockAfterReport ->
                        reportingMessage?.let { message ->
                            viewModel.reportMessage(
                                message = message,
                                reason = reason,
                                details = details,
                                contextCount = contextCount,
                                blockAfterReport = blockAfterReport
                            )
                        }

                        reportingMessage = null
                    }
                )
            }

            if (receiptMessage != null) {
                MessageReceiptSheet(
                    message = receiptMessage!!,
                    currentUserId = currentUserId,
                    isGroupRoom = conversation.isGroup == true,
                    onDismiss = {
                        receiptMessage = null
                    }
                )
            }
        }
    }
}

private fun sendDraftMessage(
    draft: String,
    pendingGifUrl: String?,
    conversation: ConversationDto,
    viewModel: ChatThreadViewModel,
    currentUserId: Int?,
    currentUsername: String?,
    onSent: () -> Unit
) {
    val trimmed = draft.trim()

    if (trimmed.isEmpty() && pendingGifUrl == null) return
    if (conversation.id == null) return

    if (pendingGifUrl != null) {
        viewModel.sendMedia(
            conversation = conversation,
            mediaUrls = listOf(pendingGifUrl),
            text = trimmed
        )
    } else {
        viewModel.sendMessage(
            conversation = conversation,
            text = trimmed,
            currentUserId = currentUserId,
            currentUsername = currentUsername
        )
    }

    onSent()
}

private fun sendVoiceNote(
    voiceDraft: VoiceNoteDraft,
    conversation: ConversationDto,
    viewModel: ChatThreadViewModel,
    uploadRepository: UploadRepository,
    scope: CoroutineScope,
    onSent: () -> Unit
) {
    if (conversation.id == null) return

    scope.launch {
        val uploaded = uploadRepository.uploadAudio(voiceDraft.file)

        viewModel.sendMedia(
            conversation = conversation,
            mediaUrls = listOf(uploaded.url),
            text = ""
        )

        onSent()
    }
}


@Composable
private fun SmsMessageBubble(
    message: SmsMessageDto,
    isMine: Boolean
) {
    val displayText = message.displayFallbackText

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMine) 18.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 18.dp
            ),
            color =
                if (isMine) {
                    ChatforiaColors.accent
                } else {
                    ChatforiaColors.cardBackground
                },
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = displayText,
                    color =
                        if (isMine) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            ChatforiaColors.primaryText
                        }
                )

                if (message.optimistic) {
                    Text(
                        text = "Sending…",
                        style = MaterialTheme.typography.labelSmall,
                        color =
                            if (isMine) {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                            } else {
                                ChatforiaColors.secondaryText
                            }
                    )
                }

                if (message.failed) {
                    Text(
                        text = "Failed to send",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (message.editedAt != null) {
                    Text(
                        text = "Edited",
                        style = MaterialTheme.typography.labelSmall,
                        color =
                            if (isMine) {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                            } else {
                                ChatforiaColors.secondaryText
                            }
                    )
                }
            }
        }
    }
}

