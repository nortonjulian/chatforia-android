package com.chatforia.android.messages

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chatforia.android.chats.ConversationDto
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import com.chatforia.android.ui.theme.ChatforiaColors
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardActions
import com.chatforia.android.socket.SocketManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadScreen(
    conversation: ConversationDto,
    viewModel: ChatThreadViewModel,
    currentUserId: Int?,
    socketManager: SocketManager,
    onBack: () -> Unit
) {

    val messages by viewModel.messages.collectAsState()

    val isLoading by viewModel.isLoading.collectAsState()

    val isSending by viewModel.isSending.collectAsState()

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val error by viewModel.error.collectAsState()

    val listState = rememberLazyListState()

    var draft by remember {
        mutableStateOf("")
    }

    LaunchedEffect(conversation.id) {
        conversation.id?.let { roomId: Int ->
            viewModel.loadMessages(roomId)
            viewModel.connectRealtime(
                roomId = roomId,
                socketManager = socketManager
            )
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text(
                        conversation.displayName
                            ?: conversation.title
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {

                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }

    ) { padding ->

        Column(
            modifier =
                Modifier
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

                    messages.isEmpty() -> {
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
                            items(messages) { message ->
                                val isMine =
                                    message.sender.id == currentUserId

                                MessageBubble(
                                    message = message,
                                    isMine = isMine
                                )
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

                OutlinedTextField(
                    value = draft,

                    onValueChange = {
                        draft = it
                    },

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

                            val trimmed =
                                draft.trim()

                            if (trimmed.isNotEmpty()) {

                                conversation.id?.let { roomId ->

                                    viewModel.sendMessage(
                                        roomId = roomId,
                                        text = trimmed
                                    )

                                    draft = ""

                                    focusManager.clearFocus()

                                    keyboardController?.hide()
                                }
                            }
                        }
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        val trimmed = draft.trim()

                        if (trimmed.isNotEmpty()) {
                            conversation.id?.let { roomId ->
                                viewModel.sendMessage(
                                    roomId = roomId,
                                    text = trimmed,
                                    currentUserId = currentUserId
                                )

                                draft = ""
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                        }
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
        }
    }
}

@Composable
private fun MessageBubble(
    message: MessageDto,
    isMine: Boolean
) {


    Row(
        modifier = Modifier.fillMaxWidth(),

        horizontalArrangement =
            if (isMine) {
                Arrangement.End
            } else {
                Arrangement.Start
            }
    ) {

        Surface(

            shape =
                RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,

                    bottomStart =
                        if (isMine) 18.dp else 4.dp,

                    bottomEnd =
                        if (isMine) 4.dp else 18.dp
                ),

            color =
                if (isMine) {
                    ChatforiaColors.accent
                } else {
                    ChatforiaColors.cardBackground
                },

            modifier =
                Modifier.widthIn(max = 300.dp)

        ) {

            Text(

                text =
                    message.rawContent
                        ?: message.translatedForMe
                        ?: "[encrypted or unsupported message]",

                modifier =
                    Modifier.padding(12.dp),

                color =
                    if (isMine) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        ChatforiaColors.primaryText
                    }
            )
        }
    }
}