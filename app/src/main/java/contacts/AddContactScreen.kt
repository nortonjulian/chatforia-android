package com.chatforia.android.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chatforia.android.ui.theme.ChatforiaColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.res.stringResource
import com.chatforia.android.R

private enum class AddContactMode {
    USERNAME,
    PHONE
}

@Composable
fun AddContactScreen(
    editingContact: ContactDto? = null,
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

        val isPhoneContact =
            !contact.externalPhone.isNullOrBlank() &&
                    (contact.user?.id == null && contact.userId == null)

        mode =
            if (isPhoneContact) {
                AddContactMode.PHONE
            } else {
                AddContactMode.USERNAME
            }

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

    val canSave =
        when (mode) {
            AddContactMode.USERNAME -> username.isNotBlank()
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
                Text(stringResource(R.string.android_chats_cancel))
            }

            Text(
                text =
                    if (editingContact == null) {
                        "Add Contact"
                    } else {
                        "Edit Contact"
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
            Button(
                onClick = { mode = AddContactMode.USERNAME },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        if (mode == AddContactMode.USERNAME) {
                            ChatforiaColors.accent
                        } else {
                            ChatforiaColors.cardBackground
                        },
                    contentColor = ChatforiaColors.primaryText
                )
            ) {
                Text(stringResource(R.string.android_profile_username))
            }

            Button(
                onClick = { mode = AddContactMode.PHONE },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        if (mode == AddContactMode.PHONE) {
                            ChatforiaColors.accent
                        } else {
                            ChatforiaColors.cardBackground
                        },
                    contentColor = ChatforiaColors.primaryText
                )
            ) {
                Text(stringResource(R.string.android_add_contact_phone))
            }
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
                    OutlinedTextField(
                        value = usernameText,
                        onValueChange = { usernameText = it },
                        label = { Text(stringResource(R.string.android_profile_username)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
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

                    Switch(
                        checked = favorite,
                        onCheckedChange = { favorite = it }
                    )
                }

                Text(
                    text =
                        if (mode == AddContactMode.USERNAME) {
                            "Use this tab only for existing Chatforia users."
                        } else {
                            "Save any phone number, even if they are not on Chatforia."
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChatforiaColors.secondaryText
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = {
                println("🔥 Save Contact tapped. mode=$mode username=$username phone=$phone favorite=$favorite")
                when (mode) {
                    AddContactMode.USERNAME ->
                        onSaveUsername(username, alias, favorite)

                    AddContactMode.PHONE ->
                        onSaveExternal(phone, name, alias, favorite)
                }
            },
            enabled = canSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ChatforiaColors.accent,
                contentColor = ChatforiaColors.primaryText,
                disabledContainerColor = ChatforiaColors.highlightedSurface,
                disabledContentColor = ChatforiaColors.secondaryText
            )
        ) {
            Text(
                text =
                    if (editingContact == null) {
                        "Save Contact"
                    } else {
                        "Save Changes"
                    },
                fontWeight = FontWeight.Bold
            )
        }
    }
}