package com.chatforia.android.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chatforia.android.ui.theme.ChatforiaColors
import androidx.compose.ui.res.stringResource
import com.chatforia.android.R

private enum class AddContactMode {
    USERNAME,
    PHONE
}

@Composable
fun AddContactScreen(
    editingContact: ContactDto? = null,
    peopleSearchResults: List<PeopleSearchItem> = emptyList(),
    selectedPeopleResult: PeopleSearchItem? = null,
    isSearchingPeople: Boolean = false,
    peopleSearchError: String? = null,
    onSearchPeople: (String) -> Unit = {},
    onSelectPeople: (PeopleSearchItem) -> Unit = {},
    onClearSelectedPeople: () -> Unit = {},
    onSaveUserId: (userId: Int, alias: String?, favorite: Boolean) -> Unit = { _, _, _ -> },
    onSaveUsername: (username: String, alias: String?, favorite: Boolean) -> Unit,
    onSaveExternal: (phone: String, name: String?, alias: String?, favorite: Boolean) -> Unit,
    onBack: () -> Unit
) {
    var mode by remember { mutableStateOf(AddContactMode.USERNAME) }
    var usernameText by remember { mutableStateOf("") }
    var phoneText by remember { mutableStateOf("") }
    var nameText by remember { mutableStateOf("") }
    var aliasText by remember { mutableStateOf("") }
    var favorite by remember { mutableStateOf(false) }

    LaunchedEffect(editingContact?.id) {
        val contact = editingContact ?: return@LaunchedEffect
        val isPhoneContact = !contact.externalPhone.isNullOrBlank() &&
                (contact.user?.id == null && contact.userId == null)

        mode = if (isPhoneContact) AddContactMode.PHONE else AddContactMode.USERNAME
        usernameText = contact.user?.username ?: ""
        phoneText = contact.externalPhone ?: ""
        nameText = contact.externalName ?: ""
        aliasText = contact.alias ?: ""
        favorite = contact.favorite
    }

    val username = usernameText.trim()
    val phone = phoneText.trim()
    val name = nameText.trim().ifBlank { null }
    val alias = aliasText.trim().ifBlank { null }
    val isEditing = editingContact != null

    val originallyLinkedUser = editingContact?.let {
        it.user?.id != null || it.userId != null
    } == true
    val originalMode = if (editingContact != null && !originallyLinkedUser) {
        AddContactMode.PHONE
    } else {
        AddContactMode.USERNAME
    }
    val originalUsername = editingContact?.user?.username?.trim().orEmpty()
    val unchangedUsername = isEditing && originalMode == AddContactMode.USERNAME &&
        username.equals(originalUsername, ignoreCase = true) &&
        selectedPeopleResult == null
    // Switching to another identity creates/updates that contact separately.
    // Only an unchanged contact identity may be saved through PATCH /contacts.
    val canSave = when (mode) {
        AddContactMode.USERNAME ->
            selectedPeopleResult?.userId != null || unchangedUsername
        AddContactMode.PHONE -> phone.isNotBlank()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatforiaColors.screenBackground)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack) {
                Text(
                    if (editingContact == null)
                        stringResource(R.string.android_chats_cancel)
                    else
                        stringResource(R.string.android_plan_back)
                )
            }

            Text(
                text = if (editingContact == null) {
                    stringResource(R.string.android_add_contact_add_contact)
                } else {
                    stringResource(R.string.android_add_contact_edit_contact)
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = ChatforiaColors.primaryText
            )

            Spacer(modifier = Modifier.width(72.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ContactModeButton(
                text = stringResource(R.string.android_profile_username),
                selected = mode == AddContactMode.USERNAME,
                enabled = true,
                onClick = {
                    mode = AddContactMode.USERNAME
                    onClearSelectedPeople()
                },
                modifier = Modifier.weight(1f)
            )

            ContactModeButton(
                text = stringResource(R.string.android_add_contact_phone),
                selected = mode == AddContactMode.PHONE,
                enabled = true,
                onClick = {
                    mode = AddContactMode.PHONE
                    onClearSelectedPeople()
                },
                modifier = Modifier.weight(1f)
            )
        }

        if (isEditing && mode != originalMode) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.android_add_contact_switch_type_note),
                style = MaterialTheme.typography.bodySmall,
                color = ChatforiaColors.secondaryText
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ChatforiaColors.cardBackground,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (mode == AddContactMode.USERNAME) {
                    if (selectedPeopleResult != null) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = ChatforiaColors.screenBackground,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedPeopleResult.resolvedDisplayName,
                                        fontWeight = FontWeight.Bold,
                                        color = ChatforiaColors.primaryText
                                    )
                                    selectedPeopleResult.username?.let {
                                        Text(
                                            text = "@$it",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ChatforiaColors.secondaryText
                                        )
                                    }
                                }
                                TextButton(onClick = {
                                    usernameText = ""
                                    onClearSelectedPeople()
                                }) {
                                    Text("Change")
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = usernameText,
                            onValueChange = {
                                usernameText = it
                                onClearSelectedPeople()
                                onSearchPeople(it)
                            },
                            label = { Text("Name or username") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        if (username.isNotEmpty() && username.length < 3) {
                            Text(
                                text = "Enter at least 3 characters to search people.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ChatforiaColors.secondaryText
                            )
                        }

                        if (isSearchingPeople) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }

                        if (mode == AddContactMode.USERNAME) {
                            peopleSearchResults.forEach { person ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = ChatforiaColors.screenBackground,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectPeople(person) }
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = person.resolvedDisplayName,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ChatforiaColors.primaryText
                                        )
                                        person.username?.let {
                                            Text(
                                                text = "@$it",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = ChatforiaColors.secondaryText
                                            )
                                        }
                                    }
                                }
                            }

                            peopleSearchError?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = phoneText,
                        onValueChange = { phoneText = it },
                        label = { Text(stringResource(R.string.android_profile_phone_number)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        label = { Text(stringResource(R.string.android_add_contact_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = aliasText,
                    onValueChange = { aliasText = it },
                    label = { Text(stringResource(R.string.android_add_contact_alias_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.android_contacts_favorite),
                            style = MaterialTheme.typography.titleMedium,
                            color = ChatforiaColors.primaryText
                        )
                        Text(
                            text = stringResource(R.string.android_add_contact_pin_this_person_as_a_favorite_contact),
                            style = MaterialTheme.typography.bodySmall,
                            color = ChatforiaColors.secondaryText
                        )
                    }
                    Switch(checked = favorite, onCheckedChange = { favorite = it })
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = {
                when (mode) {
                    AddContactMode.USERNAME -> {
                        if (unchangedUsername) {
                            // Preserve metadata editing for the original contact.
                            onSaveUsername(username, alias, favorite)
                        } else {
                            selectedPeopleResult?.userId?.let { selectedId ->
                                // Different user: upsert a separate account contact.
                                onSaveUserId(selectedId, alias, favorite)
                            }
                        }
                    }
                    AddContactMode.PHONE -> onSaveExternal(phone, name, alias, favorite)
                }
            },
            enabled = canSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ChatforiaColors.buttonStart,
                contentColor = ChatforiaColors.buttonForeground,
                disabledContainerColor = ChatforiaColors.buttonStart.copy(alpha = 0.35f),
                disabledContentColor = ChatforiaColors.buttonForeground.copy(alpha = 0.60f)
            )
        ) {
            Text(
                text = if (editingContact == null) {
                    stringResource(R.string.android_add_contact_save_contact)
                } else {
                    stringResource(R.string.android_add_contact_save_changes)
                },
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ContactModeButton(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .height(50.dp)
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                ChatforiaColors.buttonStart,
                                ChatforiaColors.buttonEnd
                            )
                        )
                    )
                } else {
                    Modifier
                        .background(ChatforiaColors.cardBackground)
                        .border(1.dp, ChatforiaColors.border, shape)
                }
            )
            .alpha(if (enabled) 1f else 0.70f)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) ChatforiaColors.buttonForeground else ChatforiaColors.primaryText,
            fontWeight = FontWeight.SemiBold
        )
    }
}
