package com.chatforia.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
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

data class ContactPreview(
    val name: String,
    val subtitle: String,
    val favorite: Boolean = false,
    val externalPhone: String? = null
)

@Composable
fun ContactsScreen() {
    var searchText by remember { mutableStateOf("") }

    var contacts by remember {
        mutableStateOf(
            listOf(
                ContactPreview(
                    name = "John Appleseed",
                    subtitle = "+18885551212"
                )
            )
        )
    }

    val filteredContacts = contacts.filter {
        it.name.contains(searchText, ignoreCase = true) ||
                it.subtitle.contains(searchText, ignoreCase = true)
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
                        contentDescription = "Add contact"
                    ),
                    ChatforiaAction(
                        icon = Icons.Default.Refresh,
                        contentDescription = "Refresh"
                    )
                )
            )
        }

        ChatforiaSearchField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = "Search contacts",
            modifier = Modifier.fillMaxWidth()
        )

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
                        ContactPreviewRow(contact)
                        HorizontalDivider(color = ChatforiaColors.border)
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactPreviewRow(contact: ContactPreview) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChatforiaAvatar(
            name = contact.name,
            size = 48.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = contact.name,
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
                text = contact.subtitle,
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