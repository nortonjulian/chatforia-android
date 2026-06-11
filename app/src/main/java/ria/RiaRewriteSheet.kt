package com.chatforia.android.ria

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chatforia.android.ui.theme.ChatforiaColors

@Composable
fun RiaRewriteSheet(
    draft: String,
    isLoading: Boolean,
    options: List<String>,
    errorText: String?,
    disabledReason: String?,
    onToneTap: (String) -> Unit,
    onSelectRewrite: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val tones = listOf("friendly", "shorter", "professional", "clearer")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 680.dp)
            .background(ChatforiaColors.screenBackground)
            .padding(18.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Rewrite with Ria",
            style = MaterialTheme.typography.titleLarge,
            color = ChatforiaColors.primaryText
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Original text",
            style = MaterialTheme.typography.titleMedium,
            color = ChatforiaColors.primaryText
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = draft,
            color = ChatforiaColors.secondaryText,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    ChatforiaColors.cardBackground,
                    RoundedCornerShape(14.dp)
                )
                .padding(12.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Choose tone",
            style = MaterialTheme.typography.titleMedium,
            color = ChatforiaColors.primaryText
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tones.take(2).forEach { tone ->
                OutlinedButton(
                    onClick = { onToneTap(tone) },
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(tone.replaceFirstChar { it.uppercase() })
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tones.drop(2).forEach { tone ->
                OutlinedButton(
                    onClick = { onToneTap(tone) },
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(tone.replaceFirstChar { it.uppercase() })
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        when {
            isLoading -> {
                Row {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = ChatforiaColors.accent
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "Rewriting…",
                        color = ChatforiaColors.secondaryText
                    )
                }
            }

            !disabledReason.isNullOrBlank() -> {
                Text(
                    text = disabledReason,
                    color = MaterialTheme.colorScheme.error
                )
            }

            !errorText.isNullOrBlank() -> {
                Text(
                    text = errorText,
                    color = MaterialTheme.colorScheme.error
                )
            }

            options.isNotEmpty() -> {
                Text(
                    text = "Suggestions",
                    style = MaterialTheme.typography.titleMedium,
                    color = ChatforiaColors.primaryText
                )

                Spacer(modifier = Modifier.height(10.dp))

                options.forEach { option ->
                    Button(
                        onClick = { onSelectRewrite(option) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChatforiaColors.cardBackground,
                            contentColor = ChatforiaColors.primaryText
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Text(
                            text = option,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            else -> {
                Text(
                    text = "Choose a tone to generate rewrite suggestions.",
                    color = ChatforiaColors.secondaryText
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Close")
        }
    }
}