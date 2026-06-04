package com.chatforia.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chatforia.android.chats.StartChatViewModel
import com.chatforia.android.ui.theme.ChatforiaColors
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.KeyboardArrowRight
import com.chatforia.android.contacts.ContactDto

@Composable
fun StartChatView(
    viewModel: StartChatViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val isSearching =
        state.username.trim().isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatforiaColors.screenBackground)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = ChatforiaColors.primaryText
                )
            }

            Text(
                text = "New Chat",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = ChatforiaColors.primaryText
            )
        }

        if (!isSearching) {
            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = ChatforiaColors.secondaryText,
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Start a chat",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChatforiaColors.primaryText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Search by username, contact name, or phone number.",
                    color = ChatforiaColors.secondaryText,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        } else {
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = state.username,
            onValueChange = {
                viewModel.updateUsername(it)
            },
            placeholder = {
                Text("Username, contact name, or phone number")
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    viewModel.startChat()
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        state.error?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (state.contactResults.isNotEmpty()) {
            Text(
                text = "Results",
                color = ChatforiaColors.secondaryText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
            ) {
                items(state.contactResults) { contact ->
                    StartChatContactRow(
                        contact = contact,
                        onClick = {
                            viewModel.openContact(contact)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun StartChatContactRow(
    contact: ContactDto,
    onClick: () -> Unit
) {
    val title =
        contact.alias?.trim()?.takeIf { it.isNotBlank() }
            ?: contact.user?.username?.trim()?.takeIf { it.isNotBlank() }
            ?: contact.externalName?.trim()?.takeIf { it.isNotBlank() }
            ?: contact.externalPhone?.trim()?.takeIf { it.isNotBlank() }
            ?: "Unknown contact"

    val subtitle =
        contact.user?.username?.trim()?.takeIf { it.isNotBlank() }?.let { "@$it" }
            ?: contact.externalPhone?.trim()?.takeIf { it.isNotBlank() }
            ?: "Tap to chat"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = ChatforiaColors.primaryText,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = subtitle,
                color = ChatforiaColors.secondaryText,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Icon(
            Icons.Default.KeyboardArrowRight,
            contentDescription = "Open chat",
            tint = ChatforiaColors.secondaryText
        )
    }
}