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

private enum class AddContactMode {
    USERNAME,
    PHONE
}

@Composable
fun AddContactScreen(
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
                Text("Cancel")
            }

            Text(
                text = "Add Contact",
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
                Text("Username")
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
                Text("Phone")
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
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    OutlinedTextField(
                        value = phoneText,
                        onValueChange = { phoneText = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = aliasText,
                    onValueChange = { aliasText = it },
                    label = { Text("Alias optional") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Favorite",
                            style = MaterialTheme.typography.titleMedium,
                            color = ChatforiaColors.primaryText
                        )

                        Text(
                            text = "Pin this person as a favorite contact.",
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
                text = "Save Contact",
                fontWeight = FontWeight.Bold
            )
        }
    }
}