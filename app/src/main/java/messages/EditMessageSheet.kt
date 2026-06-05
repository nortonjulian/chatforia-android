package com.chatforia.android.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.chatforia.android.ui.theme.ChatforiaColors
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMessageSheet(
    initialText: String,
    initialGifUrl: String?,
    onCancel: () -> Unit,
    onSave: (text: String, gifUrl: String?) -> Unit,
    onGifTap: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var gifUrl by remember { mutableStateOf(initialGifUrl) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    ModalBottomSheet(
        onDismissRequest = onCancel,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 520.dp, max = 760.dp)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }

                Text(
                    text = "Edit message",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                TextButton(
                    onClick = {
                        onSave(text, gifUrl)
                    }
                ) {
                    Text("Save")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TextButton(onClick = onGifTap) {
                    Text("GIF")
                }

                TextButton(
                    onClick = {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }
                ) {
                    Text("Emoji")
                }
            }

            if (!gifUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .width(160.dp)
                        .height(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ChatforiaColors.cardBackground)
                ) {
                    AsyncImage(
                        model = gifUrl,
                        contentDescription = "Selected GIF",
                        modifier = Modifier.fillMaxSize()
                    )

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        IconButton(
                            onClick = { gifUrl = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove GIF",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .focusRequester(focusRequester),
                placeholder = {
                    Text("Message")
                },
                shape = RoundedCornerShape(18.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text
                )
            )
        }
    }
}