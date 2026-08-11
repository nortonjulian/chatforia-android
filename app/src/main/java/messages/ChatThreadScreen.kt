package com.chatforia.android.messages

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.ContextCompat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.FilledIconButton
import androidx.compose.material.icons.filled.AutoAwesome
import com.chatforia.android.ria.RiaRepository
import com.chatforia.android.ria.RiaRewriteSheet
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.stringResource
import com.chatforia.android.R
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.chatforia.android.ads.InterstitialAdManager

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)
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
    isTemporaryRandomChat: Boolean = false,
    randomChatTitle: String? = null,
    isRandomAlreadyFriend: Boolean = false,
    didRequestRandomFriend: Boolean = false,
    onRandomAddFriend: (() -> Unit)? = null,

    onRandomNext: (() -> Unit)? = null,
    onRandomLeave: (() -> Unit)? = null,
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
    var reportingSmsMessage by remember { mutableStateOf<SmsMessageDto?>(null) }
    var blockingSmsMessage by remember { mutableStateOf<SmsMessageDto?>(null) }
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

    val activity = remember(context) {
        context.findActivity()
    }

    val interstitialAdManager = remember(activity) {
        activity?.let { InterstitialAdManager(it) }
    }

    val recorder = remember(context) {
        AudioRecorderService(context)
    }

    fun startVoiceRecording() {
        try {
            recorder.start()
            isRecordingVoice = true
        } catch (exception: Exception) {
            isRecordingVoice = false

            Toast.makeText(
                context,
                exception.message
                    ?: "Voice recording could not be started.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val microphonePermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startVoiceRecording()
            } else {
                Toast.makeText(
                    context,
                    "Microphone permission is required to record voice notes.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    var showSearchSheet by remember { mutableStateOf(false) }
    var threadSearchText by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    val randomDisclaimerText = "You've been paired for a random chat. Be kind!"

    val visibleChatMessages =
        if (isTemporaryRandomChat) {
            chatMessages.filterNot { message ->
                message.randomNoticeText() == randomDisclaimerText
            }
        } else {
            chatMessages
        }

    val isThreadEmpty =
        if (conversation.kind == "sms") {
            smsMessages.isEmpty()
        } else {
            !isTemporaryRandomChat && visibleChatMessages.isEmpty()
        }

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

    val topActions = buildList {
        add(
            ChatforiaAction(
                icon = Icons.Default.Search,
                contentDescription = stringResource(R.string.android_chats_search),
                onClick = {
                    showSearchSheet = true
                }
            )
        )

        if (!isTemporaryRandomChat) {
            add(
                ChatforiaAction(
                    icon = Icons.Default.Call,
                    contentDescription = stringResource(R.string.android_chat_thread_audio_call),
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
                )
            )

            add(
                ChatforiaAction(
                    icon = Icons.Default.Videocam,
                    contentDescription = stringResource(R.string.android_chat_thread_video_call),
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
        }
    }

    val headerTitle =
        if (isTemporaryRandomChat) {
            randomChatTitle
                ?.takeIf { it.isNotBlank() }
                ?: conversation.randomChat?.partnerAlias
                    ?.takeIf { it.isNotBlank() }
                ?: "Someone"
        } else {
            conversation.randomAwareTitle
        }

    Scaffold(
        modifier = Modifier.imePadding(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ChatforiaColors.screenBackground)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(ChatforiaColors.cardBackground)
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.android_plan_back),
                            tint = ChatforiaColors.primaryText
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = headerTitle,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ChatforiaColors.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    ChatforiaActionPill(
                        modifier = Modifier,
                        actions = topActions
                    )
                }
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ChatforiaColors.screenBackground)
                .padding(padding)
                .padding(12.dp)
        ) {
            if (isTemporaryRandomChat) {
                CompactRandomChatControls(
                    isAlreadyFriend = isRandomAlreadyFriend,
                    didRequestFriend = didRequestRandomFriend,
                    onAddFriend = {
                        onRandomAddFriend?.invoke()
                    },
                    onNext = {
                        onRandomNext?.invoke()
                    },
                    onLeave = {
                        onRandomLeave?.invoke() ?: onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-6).dp)
                        .padding(start = 18.dp, top = 0.dp, bottom = 4.dp)
                )
            }

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
                            Text(stringResource(R.string.android_chat_thread_no_messages_yet))
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
                                        isMine = message.isOutgoing,
                                        onDeleteForMe = {
                                            viewModel.deleteSmsMessage(message)
                                        },
                                        onReport = {
                                            reportingSmsMessage = message
                                        },
                                        onBlockNumber = {
                                            blockingSmsMessage = message
                                        }
                                    )
                                }
                            } else {
                                if (isTemporaryRandomChat) {
                                    item(key = "random-disclaimer") {
                                        RandomChatDisclaimerBanner()
                                    }
                                }

                                items(
                                    items = visibleChatMessages,
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
                        contentDescription = stringResource(R.string.android_chat_thread_selected_gif),
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
                                contentDescription = stringResource(R.string.android_chat_thread_remove_gif),
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
                        contentDescription = stringResource(R.string.android_chat_thread_attach)
                    )
                }

                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(stringResource(R.string.android_contact_detail_message))
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = stringResource(R.string.android_chat_thread_rewrite_with_ria),
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
                                val microphonePermission =
                                    Manifest.permission.RECORD_AUDIO

                                val permissionGranted =
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        microphonePermission
                                    ) == PackageManager.PERMISSION_GRANTED

                                if (permissionGranted) {
                                    startVoiceRecording()
                                } else {
                                    microphonePermissionLauncher.launch(
                                        microphonePermission
                                    )
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
                        contentDescription = stringResource(R.string.android_chat_thread_composer_action)
                    )
                }
            }
            if (deletingMessage != null) {
                AlertDialog(
                    onDismissRequest = { deletingMessage = null },
                    title = { Text(stringResource(R.string.android_chat_thread_delete_message)) },
                    text = { Text(stringResource(R.string.android_chat_thread_choose_how_you_want_to_delete_this_message)) },
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
                                Text(stringResource(R.string.android_chat_thread_delete_for_everyone))
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
                            Text(stringResource(R.string.android_chat_thread_delete_for_me))
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
                            text = stringResource(R.string.android_chat_thread_search_in_chat),
                            style = MaterialTheme.typography.titleLarge,
                            color = ChatforiaColors.primaryText
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        OutlinedTextField(
                            value = threadSearchText,
                            onValueChange = { threadSearchText = it },
                            placeholder = { Text(stringResource(R.string.android_chat_thread_search_messages)) },
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

            if (reportingSmsMessage != null) {
                ReportSmsMessageSheet(
                    message = reportingSmsMessage!!,
                    onCancel = {
                        reportingSmsMessage = null
                    },
                    onSubmit = {
                        reason,
                        details,
                        contextCount,
                        blockAfterReport ->

                        reportingSmsMessage?.let { message ->
                            viewModel.reportSmsMessage(
                                message = message,
                                reason = reason,
                                details = details,
                                contextCount = contextCount,
                                blockAfterReport = blockAfterReport
                            )
                        }

                        reportingSmsMessage = null
                    }
                )
            }

            blockingSmsMessage?.let { message ->
                val phone =
                    message.fromNumber
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: "Unknown number"

                AlertDialog(
                    onDismissRequest = {
                        blockingSmsMessage = null
                    },
                    title = {
                        Text(
                            stringResource(
                                R.string.android_sms_block_number_title
                            )
                        )
                    },
                    text = {
                        Text(
                            stringResource(
                                R.string.android_sms_block_number_confirmation,
                                phone
                            )
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.blockSmsNumber(
                                    message.fromNumber
                                )
                                blockingSmsMessage = null
                            }
                        ) {
                            Text(
                                stringResource(
                                    R.string.android_sms_block_number_confirm
                                ),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                blockingSmsMessage = null
                            }
                        ) {
                            Text(
                                stringResource(
                                    R.string.android_chats_cancel
                                )
                            )
                        }
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

@Composable
private fun RandomChatDisclaimerBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = ChatforiaColors.cardBackground.copy(alpha = 0.9f),
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Text(
                text = "You've been paired for a random chat. Be kind!",
                color = ChatforiaColors.secondaryText,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(
                    horizontal = 14.dp,
                    vertical = 8.dp
                )
            )
        }
    }
}

private fun MessageDto.randomNoticeText(): String {
    return (
            decryptedContent
                ?: translatedForMe
                ?: rawContent
                ?: content
                ?: ""
            ).trim()
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
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
private fun CompactRandomChatControls(
    isAlreadyFriend: Boolean,
    didRequestFriend: Boolean,
    onAddFriend: () -> Unit,
    onNext: () -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val addFriendText =
        when {
            isAlreadyFriend -> "Already Friends ✓"
            didRequestFriend -> "Friend Requested"
            else -> "Add Friend"
        }

    val canAddFriend = !isAlreadyFriend && !didRequestFriend

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(999.dp))
                .border(
                    width = 1.dp,
                    color = ChatforiaColors.border,
                    shape = RoundedCornerShape(999.dp)
                )
                .clickable(enabled = canAddFriend) {
                    onAddFriend()
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = addFriendText,
                fontSize = 11.sp,
                color = if (canAddFriend) {
                    ChatforiaColors.secondaryText
                } else {
                    ChatforiaColors.secondaryText.copy(alpha = 0.65f)
                }
            )
        }

        Box(
            modifier = Modifier
                .width(88.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(ChatforiaColors.accent)
                .clickable { onNext() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Next",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
        }

        Text(
            text = "Leave",
            fontSize = 11.sp,
            color = ChatforiaColors.accent,
            modifier = Modifier.clickable { onLeave() }
        )
    }
}

@Composable
private fun SmsMessageBubble(
    message: SmsMessageDto,
    isMine: Boolean,
    onDeleteForMe: () -> Unit,
    onReport: () -> Unit,
    onBlockNumber: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val clipboard =
        LocalClipboardManager.current

    val displayText =
        message.displayFallbackText

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Box {
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
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            showMenu = true
                        }
                    )
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
                            text = stringResource(
                                R.string.android_chat_thread_sending
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color =
                                if (isMine) {
                                    MaterialTheme.colorScheme.onPrimary.copy(
                                        alpha = 0.75f
                                    )
                                } else {
                                    ChatforiaColors.secondaryText
                                }
                        )
                    }

                    if (message.failed) {
                        Text(
                            text = stringResource(
                                R.string.android_chat_thread_failed_to_send
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (message.editedAt != null) {
                        Text(
                            text = stringResource(
                                R.string.android_chat_thread_edited
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color =
                                if (isMine) {
                                    MaterialTheme.colorScheme.onPrimary.copy(
                                        alpha = 0.75f
                                    )
                                } else {
                                    ChatforiaColors.secondaryText
                                }
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = {
                    showMenu = false
                }
            ) {
                if (displayText.isNotBlank()) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    R.string.android_chat_message_row_copy
                                )
                            )
                        },
                        onClick = {
                            clipboard.setText(
                                AnnotatedString(displayText)
                            )
                            showMenu = false
                        }
                    )
                }

                if (!message.optimistic && message.id > 0) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    R.string.android_chat_thread_delete_for_me
                                )
                            )
                        },
                        onClick = {
                            showMenu = false
                            onDeleteForMe()
                        }
                    )
                }

                if (
                    !isMine &&
                    !message.optimistic &&
                    message.id > 0
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    R.string.android_sms_message_report
                                )
                            )
                        },
                        onClick = {
                            showMenu = false
                            onReport()
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    R.string.android_sms_message_block_number
                                )
                            )
                        },
                        onClick = {
                            showMenu = false
                            onBlockNumber()
                        }
                    )
                }
            }
        }
    }
}

