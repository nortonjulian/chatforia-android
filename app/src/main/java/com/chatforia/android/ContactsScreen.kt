package com.chatforia.android

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import com.chatforia.android.ui.components.ChatforiaSearchField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
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
import com.chatforia.android.ria.RiaRepository
import com.chatforia.android.network.ApiClient
import com.chatforia.android.blocking.BlockedAccountsRepository
import androidx.compose.ui.res.stringResource
import com.chatforia.android.R

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
    uploadRepository: UploadRepository,
    riaRepository: RiaRepository,
    apiClient: ApiClient
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val startChatState by startChatViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val blockedAccountsRepository = remember(apiClient) { BlockedAccountsRepository(apiClient) }
    var blockedUserIds by remember(currentUserId) { mutableStateOf<Set<Int>>(emptySet()) }
    var blockStatusLoading by remember { mutableStateOf(false) }

    var showingImportContacts by remember { mutableStateOf(false) }
    var phoneContactsToImport by remember { mutableStateOf<List<PhoneContactDto>>(emptyList()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val reader = PhoneContactsReader(context)
            coroutineScope.launch {
                val contacts = reader.readContacts()
                phoneContactsToImport = contacts
                showingImportContacts = true
            }
        }
    }

    val searchText = state.searchText
    val contacts = state.contacts

    var savedDetail by remember { mutableStateOf<ContactDto?>(null) }

    val selectedContact = state.selectedContactId?.let { selectedContactId ->
        contacts.firstOrNull { it.id == selectedContactId }
            ?: savedDetail?.takeIf { it.id == selectedContactId }
    }

    var editingContact by remember { mutableStateOf<ContactDto?>(null) }
    var pendingDeleteContact by remember { mutableStateOf<ContactDto?>(null) }
    var pendingBlockContact by remember { mutableStateOf<ContactDto?>(null) }
    var blockingAccount by remember { mutableStateOf(false) }
    var pendingMessageChoice by remember { mutableStateOf<ContactDto?>(null) }
    var revealedContactId by remember { mutableStateOf<Int?>(null) }

    fun showSavedDetail(saved: ContactDto) {
        savedDetail = saved
        viewModel.selectContact(saved.id)
        editingContact = null
    }
    var showingAddContact by remember { mutableStateOf(false) }
    var showingInviteFriends by remember { mutableStateOf(false) }
    var showingNewChat by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadContacts()
    }

    val filteredContacts = contacts
    val openedConversation = state.openedConversation

    // The server's persistent block list is authoritative across website, iOS, and Android.
    val detailUserId = selectedContact?.user?.id ?: selectedContact?.userId
    LaunchedEffect(detailUserId, currentUserId) {
        if (detailUserId == null || currentUserId == null) return@LaunchedEffect
        blockStatusLoading = true
        try {
            blockedUserIds = blockedAccountsRepository.listBlockedAccounts()
                .map { it.userId }.toSet()
        } catch (error: Exception) {
            android.util.Log.w("ContactsScreen", "Could not refresh blocked accounts", error)
            // Do not allow an ambiguous Block/Unblock action if we cannot load its state.
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.android_blocked_accounts_load_failed),
                android.widget.Toast.LENGTH_SHORT
            ).show()
        } finally {
            blockStatusLoading = false
        }
    }

    pendingBlockContact?.let { contact ->
        val targetUserId = contact.user?.id ?: contact.userId
        val alreadyBlocked = targetUserId != null && blockedUserIds.contains(targetUserId)
        val contactName = viewModel.displayName(contact)
        AlertDialog(
            onDismissRequest = { if (!blockingAccount) pendingBlockContact = null },
            title = {
                Text(stringResource(
                    if (alreadyBlocked) R.string.android_blocked_accounts_unblock_title
                    else R.string.android_block_chatforia_account_confirm_title
                ))
            },
            text = {
                Text(stringResource(
                    if (alreadyBlocked) R.string.android_blocked_accounts_unblock_confirmation
                    else R.string.android_block_chatforia_account_confirm_message,
                    contactName
                ))
            },
            confirmButton = {
                TextButton(
                    enabled = !blockingAccount && targetUserId != null,
                    onClick = {
                        val uid = targetUserId ?: return@TextButton
                        coroutineScope.launch {
                            blockingAccount = true
                            try {
                                if (alreadyBlocked) blockedAccountsRepository.unblockAccount(uid)
                                else blockedAccountsRepository.blockAccount(uid)
                                blockedUserIds = if (alreadyBlocked) blockedUserIds - uid
                                    else blockedUserIds + uid
                                pendingBlockContact = null
                                android.widget.Toast.makeText(
                                    context,
                                    context.getString(
                                        if (alreadyBlocked) R.string.android_blocked_accounts_unblock_success
                                        else R.string.android_block_chatforia_account_success
                                    ),
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } catch (error: Exception) {
                                android.widget.Toast.makeText(
                                    context,
                                    error.message ?: context.getString(R.string.android_block_chatforia_account_failed),
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            } finally {
                                blockingAccount = false
                            }
                        }
                    }
                ) {
                    Text(stringResource(
                        if (alreadyBlocked) R.string.android_blocked_accounts_unblock
                        else R.string.android_block_chatforia_account
                    ))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingBlockContact = null }, enabled = !blockingAccount) {
                    Text(stringResource(R.string.android_chats_cancel))
                }
            }
        )
    }

    // Both swipe and detail-screen deletion require confirmation.
    pendingDeleteContact?.let { contact ->
        AlertDialog(
            onDismissRequest = { pendingDeleteContact = null },
            title = { Text(stringResource(R.string.android_contact_detail_delete_contact)) },
            text = { Text("Delete ${viewModel.displayName(contact)} from your contacts?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteContact(contact)
                    if (state.selectedContactId == contact.id) {
                        savedDetail = null
                        viewModel.clearSelectedContact()
                    }
                    pendingDeleteContact = null
                    revealedContactId = null
                }) {
                    Text(
                        stringResource(R.string.android_contact_detail_delete_contact),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteContact = null }) {
                    Text(stringResource(R.string.android_chats_cancel))
                }
            }
        )
    }

    // Offer transport choice only when this saved contact has both options.
    pendingMessageChoice?.let { contact ->
        AlertDialog(
            onDismissRequest = { pendingMessageChoice = null },
            title = { Text("Send message with") },
            text = { Text("How would you like to message ${viewModel.displayName(contact)}?") },
            confirmButton = {
                TextButton(onClick = {
                    pendingMessageChoice = null
                    viewModel.openDirectChat(contact)
                }) { Text("Chatforia") }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingMessageChoice = null
                    viewModel.openSmsThread(contact)
                }) { Text("SMS") }
            }
        )
    }

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
                riaRepository = riaRepository,
                onBack = {
                    viewModel.clearOpenedConversation()
                    viewModel.clearSelectedContact()
                }
            )
            return
        }

        StartChatView(
            viewModel = startChatViewModel,
            onBack = { showingNewChat = false }
        )
        return
    }

    editingContact?.let { contact ->
        AddContactScreen(
            editingContact = contact,
            peopleSearchResults = state.peopleSearchResults,
            selectedPeopleResult = state.selectedPeopleResult,
            isSearchingPeople = state.isSearchingPeople,
            peopleSearchError = state.peopleSearchError,
            onSearchPeople = { query -> viewModel.searchPeople(query) },
            onSelectPeople = { person -> viewModel.selectPeopleResult(person) },
            onClearSelectedPeople = { viewModel.clearPeopleSelection() },
            onSaveUserId = { userId, alias, favorite ->
                // Switching to Username adds/updates that account contact;
                // it must not overwrite the originally saved identity.
                viewModel.saveUserContact(
                    userId, alias, favorite, onSaved = ::showSavedDetail
                )
            },
            onSaveUsername = { _, alias, favorite ->
                viewModel.updateContact(
                    contact = contact,
                    alias = alias,
                    externalName = null,
                    favorite = favorite,
                    onSaved = ::showSavedDetail
                )
            },
            onSaveExternal = { phone, name, alias, favorite ->
                val sameExternalIdentity =
                    contact.userId == null && contact.user?.id == null &&
                    contact.externalPhone?.trim() == phone.trim()
                if (sameExternalIdentity) {
                    viewModel.updateContact(
                        contact = contact,
                        alias = alias,
                        externalName = name,
                        favorite = favorite,
                        onSaved = ::showSavedDetail
                    )
                } else {
                    // Use the normal phone upsert; do not rewrite contact identity here.
                    viewModel.saveExternalContact(
                        phone, name, alias, favorite,
                        onSaved = ::showSavedDetail
                    )
                }
            },
            onBack = {
                editingContact = null
                viewModel.clearSelectedContact()
            }
        )
        return
    }

    if (showingAddContact) {
        AddContactScreen(
            peopleSearchResults = state.peopleSearchResults,
            selectedPeopleResult = state.selectedPeopleResult,
            isSearchingPeople = state.isSearchingPeople,
            peopleSearchError = state.peopleSearchError,
            onSearchPeople = { query -> viewModel.searchPeople(query) },
            onSelectPeople = { person -> viewModel.selectPeopleResult(person) },
            onClearSelectedPeople = { viewModel.clearPeopleSelection() },
            onSaveUserId = { userId, alias, favorite ->
                viewModel.saveUserContact(userId, alias, favorite)
                showingAddContact = false
            },
            onSaveUsername = { username, alias, favorite ->
                viewModel.saveUsernameContact(username, alias, favorite)
                showingAddContact = false
            },
            onSaveExternal = { phone, name, alias, favorite ->
                viewModel.saveExternalContact(phone, name, alias, favorite)
                showingAddContact = false
            },
            onBack = {
                viewModel.clearPeopleSelection()
                showingAddContact = false
            }
        )
        return
    }

    if (showingInviteFriends) {
        InviteFriendsScreen(
            repository = inviteRepository,
            currentUsername = currentUsername,
            onBack = { showingInviteFriends = false }
        )
        return
    }

    if (showingImportContacts) {
        ImportPhoneContactsScreen(
            contacts = phoneContactsToImport,
            isImporting = state.isImportingContacts,
            onImportSelected = { selectedContacts ->
                viewModel.importPhoneContacts(selectedContacts)
                showingImportContacts = false
            },
            onBack = { showingImportContacts = false }
        )
        return
    }

    openedConversation?.let { conversation ->
        key(conversation.id) {
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
                riaRepository = riaRepository,
                onBack = { viewModel.clearOpenedConversation() }
            )
        }
        return
    }

    selectedContact?.let { contact ->
        ContactDetailScreen(
            contact = contact,
            displayName = viewModel.displayName(contact),
            subtitle = viewModel.subtitle(contact),
            onMessage = {
                val chatUserId = contact.user?.id ?: contact.userId
                val savedPhone = contact.externalPhone?.trim()?.takeIf { it.isNotEmpty() }
                when {
                    chatUserId != null && savedPhone != null -> pendingMessageChoice = contact
                    chatUserId != null -> viewModel.openDirectChat(contact)
                    savedPhone != null -> viewModel.openSmsThread(contact)
                    else -> android.widget.Toast.makeText(
                        context,
                        "This contact has no available messaging method.",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onCall = {
                val userId = contact.user?.id ?: contact.userId
                val phone = contact.externalPhone?.trim()?.takeIf { it.isNotBlank() }
                when {
                    userId != null -> androidCallManager.startAudioCall(
                        calleeId = userId,
                        displayName = viewModel.displayName(contact)
                    )
                    phone != null -> androidCallManager.startPhoneCall(phone)
                }
            },
            onVideo = {
                val userId = contact.user?.id ?: contact.userId
                if (userId != null) {
                    androidCallManager.startVideoCall(
                        currentUser = currentUser,
                        calleeId = userId,
                        displayName = viewModel.displayName(contact)
                    )
                }
            },
            onEdit = {
                viewModel.clearPeopleSelection()
                editingContact = contact
            },
            onDelete = { pendingDeleteContact = contact },
            isAccountBlocked = (contact.user?.id ?: contact.userId)
                ?.let { blockedUserIds.contains(it) } == true,
            onToggleAccountBlock = {
                if (!blockStatusLoading) pendingBlockContact = contact
            },
            onBack = {
                savedDetail = null
                viewModel.clearSelectedContact()
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
                text = stringResource(R.string.android_contacts_contacts),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = ChatforiaColors.primaryText
            )

            ChatforiaActionPill(
                actions = listOf(
                    ChatforiaAction(
                        icon = Icons.Default.Add,
                        contentDescription = stringResource(R.string.android_chats_new_chat),
                        onClick = { showingNewChat = true }
                    ),
                    ChatforiaAction(
                        icon = Icons.Default.PersonAdd,
                        contentDescription = stringResource(R.string.android_contacts_add_contact),
                        onClick = {
                            viewModel.clearPeopleSelection()
                            showingAddContact = true
                        }
                    ),
                    ChatforiaAction(
                        icon = Icons.Default.Contacts,
                        contentDescription = stringResource(R.string.android_contacts_import_from_phone),
                        onClick = {
                            val permissionGranted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.READ_CONTACTS
                            ) == PackageManager.PERMISSION_GRANTED

                            if (permissionGranted) {
                                val reader = PhoneContactsReader(context)
                                coroutineScope.launch {
                                    val importedContacts = reader.readContacts()
                                    phoneContactsToImport = importedContacts
                                    showingImportContacts = true
                                }
                            } else {
                                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        }
                    ),
                    ChatforiaAction(
                        icon = Icons.Default.Share,
                        contentDescription = stringResource(R.string.android_contacts_invite_friends),
                        onClick = { showingInviteFriends = true }
                    )
                )
            )
        }

        ChatforiaSearchField(
            value = searchText,
            onValueChange = { viewModel.updateSearchText(it) },
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
                    text = if (searchText.isBlank()) "No contacts yet" else "No contacts found",
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
                LazyColumn(modifier = Modifier.padding(12.dp)) {
                    items(filteredContacts, key = { it.id }) { contact ->
                        SwipeableContactPreviewRow(
                            contact = contact,
                            viewModel = viewModel,
                            revealed = revealedContactId == contact.id,
                            onRevealChanged = { show ->
                                revealedContactId = if (show) contact.id else
                                    revealedContactId.takeUnless { it == contact.id }
                            },
                            onClick = { viewModel.selectContact(contact.id) },
                            onEdit = {
                                revealedContactId = null
                                viewModel.clearPeopleSelection()
                                editingContact = contact
                            },
                            onDelete = {
                                revealedContactId = null
                                pendingDeleteContact = contact
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
            .clickable { onClick() }
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
                        contentDescription = stringResource(R.string.android_contacts_favorite),
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
            contentDescription = stringResource(R.string.android_contacts_open_contact),
            tint = ChatforiaColors.secondaryText
        )
    }
}

// Reveal actions; never delete as a direct consequence of swiping.
@Composable
private fun SwipeableContactPreviewRow(
    contact: ContactDto,
    viewModel: ContactsViewModel,
    revealed: Boolean,
    onRevealChanged: (Boolean) -> Unit,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val actionWidth = 94.dp
    val revealDistance = with(LocalDensity.current) { (actionWidth * 2).toPx() }
    var offsetX by remember(contact.id) { mutableFloatStateOf(0f) }

    LaunchedEffect(revealed) {
        if (!revealed) offsetX = 0f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.matchParentSize(),
            horizontalArrangement = Arrangement.End
        ) {
            ContactSwipeAction(
                title = stringResource(R.string.android_contact_detail_edit_contact),
                icon = Icons.Default.Edit,
                backgroundColor = ChatforiaColors.highlightedSurface,
                contentColor = ChatforiaColors.primaryText,
                modifier = Modifier.width(actionWidth).fillMaxHeight(),
                onClick = onEdit
            )
            ContactSwipeAction(
                title = stringResource(R.string.android_contact_detail_delete_contact),
                icon = Icons.Default.Delete,
                backgroundColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                modifier = Modifier.width(actionWidth).fillMaxHeight(),
                onClick = onDelete
            )
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .background(ChatforiaColors.cardBackground)
                .pointerInput(contact.id, revealDistance) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount).coerceIn(-revealDistance, 0f)
                        },
                        onDragEnd = {
                            val shouldReveal = offsetX < -revealDistance * 0.4f
                            offsetX = if (shouldReveal) -revealDistance else 0f
                            onRevealChanged(shouldReveal)
                        },
                        onDragCancel = {
                            offsetX = if (revealed) -revealDistance else 0f
                        }
                    )
                }
        ) {
            ContactPreviewRow(
                contact = contact,
                viewModel = viewModel,
                onClick = {
                    if (revealed) {
                        offsetX = 0f
                        onRevealChanged(false)
                    } else {
                        onClick()
                    }
                }
            )
        }
    }
}

@Composable
private fun ContactSwipeAction(
    title: String,
    icon: ImageVector,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = backgroundColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = title, tint = contentColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                color = contentColor,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}
