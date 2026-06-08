package com.chatforia.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import com.chatforia.android.ui.components.ChatforiaSearchField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatforia.android.ui.theme.ChatforiaColors
import androidx.compose.material.icons.filled.KeyboardArrowRight
import com.chatforia.android.ui.components.ChatforiaAvatar
import com.chatforia.android.ui.components.ChatforiaAction
import com.chatforia.android.ui.components.ChatforiaActionPill
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chatforia.android.contacts.ContactsViewModel
import com.chatforia.android.contacts.ContactDto
import androidx.compose.foundation.clickable
import com.chatforia.android.contacts.ContactDetailScreen
import com.chatforia.android.messages.ChatThreadScreen
import com.chatforia.android.messages.ChatThreadViewModel
import com.chatforia.android.socket.SocketManager
import com.chatforia.android.contacts.AddContactScreen
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import com.chatforia.android.contacts.InviteFriendsScreen
import com.chatforia.android.contacts.InviteRepository
import androidx.compose.material.icons.filled.PersonAdd
import com.chatforia.android.contacts.PhoneContactsReader

import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Share
import com.chatforia.android.contacts.PhoneContactDto
import com.chatforia.android.contacts.ImportPhoneContactsScreen
import com.chatforia.android.StartChatView
import com.chatforia.android.chats.StartChatViewModel
import com.chatforia.android.tenor.TenorRepository
import com.chatforia.android.upload.UploadRepository
import com.chatforia.android.auth.UserDto
import com.chatforia.android.calls.AndroidCallManager

@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel,
    startChatViewModel: StartChatViewModel,
    inviteRepository: InviteRepository,
    threadViewModel: ChatThreadViewModel,
    currentUserId: Int?,
    currentUsername: String?,
    currentUser: UserDto,
    androidCallManager: AndroidCallManager,
    socketManager: SocketManager,
    tenorRepository: TenorRepository,
    uploadRepository: UploadRepository
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val startChatState by startChatViewModel.state.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    var showingImportContacts by remember {
        mutableStateOf(false)
    }

    var phoneContactsToImport by remember {
        mutableStateOf<List<PhoneContactDto>>(emptyList())
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                val reader = PhoneContactsReader(context)

                coroutineScope.launch {

                    val contacts =
                        reader.readContacts()

                    phoneContactsToImport = contacts
                    showingImportContacts = true
                }
            }
        }

    val searchText = state.searchText
    val contacts = state.contacts

    var selectedContact by remember {
        mutableStateOf<ContactDto?>(null)
    }

    var showingAddContact by remember {
        mutableStateOf(false)
    }

    var showingInviteFriends by remember {
        mutableStateOf(false)
    }

    var showingNewChat by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        viewModel.loadContacts()
    }

    val filteredContacts = contacts

    val openedConversation = state.openedConversation

    if (showingNewChat) {
        startChatState.openedConversation?.let { conversation ->
            ChatThreadScreen(
                conversation = conversation,
                viewModel = threadViewModel,
                currentUserId = currentUserId,
                currentUsername = currentUsername,
                currentUser = currentUser,
                androidCallManager = androidCallManager,
                socketManager = socketManager,
                uploadRepository = uploadRepository,
                tenorRepository = tenorRepository,
                onBack = {
                    showingNewChat = false
                }
            )

            return
        }

        StartChatView(
            viewModel = startChatViewModel,
            onBack = {
                showingNewChat = false
            }
        )

        return
    }

    if (showingAddContact) {
        AddContactScreen(
            onSaveUsername = { username, alias, favorite ->
                viewModel.saveUsernameContact(username, alias, favorite)
                showingAddContact = false
            },
            onSaveExternal = { phone, name, alias, favorite ->
                viewModel.saveExternalContact(phone, name, alias, favorite)
                showingAddContact = false
            },
            onBack = {
                showingAddContact = false
            }
        )

        return
    }

    if (showingInviteFriends) {
        InviteFriendsScreen(
            repository = inviteRepository,
            currentUsername = currentUsername,
            onBack = {
                showingInviteFriends = false
            }
        )

        return
    }

    if (showingImportContacts) {
        ImportPhoneContactsScreen(
            contacts = phoneContactsToImport,
            isImporting = state.isImportingContacts,
            onImportSelected = { selectedContacts ->
                viewModel.importPhoneContacts(
                    selectedContacts
                )

                showingImportContacts = false
            },
            onBack = {
                showingImportContacts = false
            }
        )

        return
    }

    openedConversation?.let { conversation ->
        ChatThreadScreen(
            conversation = conversation,
            viewModel = threadViewModel,
            currentUserId = currentUserId,
            currentUsername = currentUsername,
            currentUser = currentUser,
            androidCallManager = androidCallManager,
            socketManager = socketManager,
            uploadRepository = uploadRepository,
            tenorRepository = tenorRepository,
            onBack = {
                showingNewChat = false
            }
        )

        return
    }

    selectedContact?.let { contact ->

        ContactDetailScreen(
            contact = contact,
            displayName = viewModel.displayName(contact),
            subtitle = viewModel.subtitle(contact),

            onMessage = {
                viewModel.openDirectChat(contact)
            },

            onCall = {
                // TODO
            },

            onVideo = {
                // TODO
            },

            onDelete = {
                viewModel.deleteContact(contact)
                selectedContact = null
            },

            onBack = {
                selectedContact = null
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
                text = "Contacts",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = ChatforiaColors.primaryText
            )

            ChatforiaActionPill(
                actions = listOf(
                    ChatforiaAction(
                        icon = Icons.Default.Add,
                        contentDescription = "New chat",
                        onClick = {
                            showingNewChat = true
                        }
                    ),
                    ChatforiaAction(
                        icon = Icons.Default.PersonAdd,
                        contentDescription = "Add contact",
                        onClick = {
                            showingAddContact = true
                        }
                    ),
                    ChatforiaAction(
                        icon = Icons.Default.Contacts,
                        contentDescription = "Import from phone",
                        onClick = {

                            val permissionGranted =
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.READ_CONTACTS
                                ) == PackageManager.PERMISSION_GRANTED

                            if (permissionGranted) {
                                val reader =
                                    PhoneContactsReader(context)

                                coroutineScope.launch {
                                    val contacts =
                                        reader.readContacts()

                                    phoneContactsToImport = contacts
                                    showingImportContacts = true
                                }
                            } else {
                                permissionLauncher.launch(
                                    Manifest.permission.READ_CONTACTS
                                )
                            }
                        }
                    ),
                    ChatforiaAction(
                        icon = Icons.Default.Share,
                        contentDescription = "Invite friends",
                        onClick = {
                            showingInviteFriends = true
                        }
                    ),
                )
            )
        }

        ChatforiaSearchField(
            value = searchText,
            onValueChange = {
                viewModel.updateSearchText(it)
            },
            placeholder = "Search contacts",
            modifier = Modifier.fillMaxWidth()
        )

        state.importMessage?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                color = ChatforiaColors.accent,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        state.error?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (filteredContacts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchText.isBlank()) {
                        "No contacts yet"
                    } else {
                        "No contacts found"
                    },
                    color = ChatforiaColors.secondaryText
                )
            }
        } else {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = ChatforiaColors.cardBackground,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyColumn(
                    modifier = Modifier.padding(12.dp)
                ) {
                    items(filteredContacts) { contact ->
                        ContactPreviewRow(
                            contact = contact,
                            viewModel = viewModel,
                            onClick = {
                                selectedContact = contact
                            }
                        )
                        HorizontalDivider(color = ChatforiaColors.border)
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactPreviewRow(
    contact: ContactDto,
    viewModel: ContactsViewModel,
    onClick: () -> Unit
) {
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
            name = viewModel.displayName(contact),
            size = 48.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = viewModel.displayName(contact),
                    style = MaterialTheme.typography.titleMedium,
                    color = ChatforiaColors.primaryText
                )

                if (contact.favorite) {
                    Spacer(modifier = Modifier.width(6.dp))

                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Favorite",
                        tint = ChatforiaColors.accent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = viewModel.subtitle(contact),
                style = MaterialTheme.typography.bodyMedium,
                color = ChatforiaColors.secondaryText
            )
        }

        Icon(
            Icons.Default.KeyboardArrowRight,
            contentDescription = "Open contact",
            tint = ChatforiaColors.secondaryText
        )
    }
}