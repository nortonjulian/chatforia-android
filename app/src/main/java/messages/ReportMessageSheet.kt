package com.chatforia.android.messages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.chatforia.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportMessageSheet(
    message: MessageDto,
    onCancel: () -> Unit,
    onSubmit: (
        reason: String,
        details: String,
        contextCount: Int,
        blockAfterReport: Boolean
    ) -> Unit
) {
    var reason by remember { mutableStateOf("harassment") }
    var details by remember { mutableStateOf("") }
    var contextCount by remember { mutableStateOf(10) }
    var blockAfterReport by remember { mutableStateOf(true) }

    val previewText =
        message.decryptedContent
            ?: message.translatedForMe
            ?: message.rawContent
            ?: message.content
            ?: "[No visible text]"

    ModalBottomSheet(
        onDismissRequest = onCancel
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.android_report_message_report_message),
                style = MaterialTheme.typography.titleLarge
            )

            Text(stringResource(R.string.android_report_message_reason))

            ReasonDropdown(
                selected = reason,
                onSelected = { reason = it }
            )

            Text(stringResource(R.string.android_report_message_include_previous_messages))

            ContextDropdown(
                selected = contextCount,
                onSelected = { contextCount = it }
            )

            OutlinedTextField(
                value = details,
                onValueChange = { details = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.android_report_message_additional_details)) },
                placeholder = { Text(stringResource(R.string.android_report_message_anything_else_moderators_should_know)) },
                minLines = 3,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.android_report_message_block_this_user_after_reporting))

                Switch(
                    checked = blockAfterReport,
                    onCheckedChange = { blockAfterReport = it }
                )
            }

            ElevatedCard {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Reporting message from ${message.sender.username ?: "Unknown user"}",
                        style = MaterialTheme.typography.labelMedium
                    )

                    Text(
                        text = previewText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.android_chats_cancel))
                }

                Button(
                    onClick = {
                        onSubmit(
                            reason,
                            details,
                            contextCount,
                            blockAfterReport
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.android_report_message_submit_report))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReasonDropdown(
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val reasons = listOf(
        "harassment" to "Harassment",
        "threats" to "Threats",
        "hate" to "Hate or abusive conduct",
        "sexual_content" to "Sexual content",
        "spam_scam" to "Spam or scam",
        "impersonation" to "Impersonation",
        "other" to "Other"
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = reasons.firstOrNull { it.first == selected }?.second ?: "Harassment",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            reasons.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.second) },
                    onClick = {
                        onSelected(item.first)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContextDropdown(
    selected: Int,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val options = listOf(
        0 to "Only this message",
        5 to "This + previous 5",
        10 to "This + previous 10",
        20 to "This + previous 20"
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = options.firstOrNull { it.first == selected }?.second ?: "This + previous 10",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.second) },
                    onClick = {
                        onSelected(item.first)
                        expanded = false
                    }
                )
            }
        }
    }
}