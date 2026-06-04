package com.chatforia.android.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatforia.android.ui.theme.ChatforiaColors

@Composable
fun ImportPhoneContactsScreen(
    contacts: List<PhoneContactDto>,
    isImporting: Boolean,
    onImportSelected: (List<PhoneContactDto>) -> Unit,
    onBack: () -> Unit
) {
    var selectedPhones by remember {
        mutableStateOf(
            contacts.map { it.phone }.toSet()
        )
    }

    val selectedContacts =
        contacts.filter { selectedPhones.contains(it.phone) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatforiaColors.screenBackground)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = ChatforiaColors.primaryText
                )
            }

            Text(
                text = "Import Contacts",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = ChatforiaColors.primaryText
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "${selectedContacts.size} of ${contacts.size} selected",
            color = ChatforiaColors.secondaryText
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(
                onClick = {
                    selectedPhones = contacts.map { it.phone }.toSet()
                }
            ) {
                Text("Select All")
            }

            TextButton(
                onClick = {
                    selectedPhones = emptySet()
                }
            ) {
                Text("Clear")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(contacts) { contact ->
                val checked = selectedPhones.contains(contact.phone)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedPhones =
                                if (checked) {
                                    selectedPhones - contact.phone
                                } else {
                                    selectedPhones + contact.phone
                                }
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { isChecked ->
                            selectedPhones =
                                if (isChecked) {
                                    selectedPhones + contact.phone
                                } else {
                                    selectedPhones - contact.phone
                                }
                        }
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = contact.name,
                            color = ChatforiaColors.primaryText,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = contact.phone,
                            color = ChatforiaColors.secondaryText
                        )
                    }
                }
            }
        }

        Button(
            onClick = {
                onImportSelected(selectedContacts)
            },
            enabled = selectedContacts.isNotEmpty() && !isImporting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (isImporting) {
                    "Importing..."
                } else {
                    "Import ${selectedContacts.size} selected"
                }
            )
        }
    }
}