package com.chatforia.android.random

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chatforia.android.ui.theme.ChatforiaColors

@Composable
fun RandomMatchingView(
    error: String?,
    onCancel: () -> Unit
) {
    Surface(
        color = ChatforiaColors.cardBackground,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 360.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            CircularProgressIndicator(
                color = ChatforiaColors.accent
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Finding someone",
                style = MaterialTheme.typography.headlineSmall,
                color = ChatforiaColors.primaryText,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = error
                    ?: "We’re looking for someone who’s open to chat right now.",
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (error == null)
                        ChatforiaColors.secondaryText
                    else
                        MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ChatforiaColors.accent
                )
            ) {
                Text("Cancel")
            }
        }
    }
}