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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadScreen(
    conversation: ConversationDto,
    viewModel: ChatThreadViewModel,
    currentUserId: Int?,
    currentUsername: String?,
    socketManager: SocketManager,
    uploadRepository: UploadRepository,
    tenorRepository: TenorRepository,
    onBack: () -> Unit
) {
    val chatMessages by viewModel.messages.collectAsState()
    val smsMessages by viewModel.smsMessages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val error by viewModel.error.collectAsState()

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    var draft by remember { mutableStateOf("") }
    var showMediaPicker by remember { mutableStateOf(false) }
    var showGifPicker by remember { mutableStateOf(false) }

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

    LaunchedEffect(chatMessages.size, smsMessages.size) {
        val itemCount =
            if (conversation.kind == "sms") {
                smsMessages.size
            } else {
                chatMessages.size
            }

        if (itemCount > 0) {
            listState.animateScrollToItem(itemCount - 1)
        }
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
                                items(smsMessages) { message ->
                                    SmsMessageBubble(
                                        message = message,
                                        isMine = message.isOutgoing
                                    )
                                }
                            } else {
                                items(chatMessages) { message ->
                                    ChatMessageRow(
                                        message = message,
                                        isMine = message.sender.id == currentUserId
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
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            sendDraftMessage(
                                draft = draft,
                                conversation = conversation,
                                viewModel = viewModel,
                                currentUserId = currentUserId,
                                currentUsername = currentUsername,
                                onSent = {
                                    draft = ""
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }
                            )
                        }
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        sendDraftMessage(
                            draft = draft,
                            conversation = conversation,
                            viewModel = viewModel,
                            currentUserId = currentUserId,
                            currentUsername = currentUsername,
                            onSent = {
                                draft = ""
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                        )
                    },
                    enabled =
                        draft.trim().isNotEmpty() &&
                                conversation.id != null &&
                                !isSending
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Send")
                    }
                }
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

            if (showGifPicker) {
                GifPickerSheet(
                    tenorRepository = tenorRepository,
                    onDismiss = {
                        showGifPicker = false
                    },
                    onGifSelected = { gif ->
                        showGifPicker = false

                        viewModel.sendMedia(
                            conversation = conversation,
                            mediaUrls = listOf(gif.url)
                        )
                    }
                )
            }
        }
    }
}

private fun sendDraftMessage(
    draft: String,
    conversation: ConversationDto,
    viewModel: ChatThreadViewModel,
    currentUserId: Int?,
    currentUsername: String?,
    onSent: () -> Unit
) {
    val trimmed = draft.trim()

    if (trimmed.isEmpty()) return
    if (conversation.id == null) return

    viewModel.sendMessage(
        conversation = conversation,
        text = trimmed,
        currentUserId = currentUserId,
        currentUsername = currentUsername
    )

    onSent()
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