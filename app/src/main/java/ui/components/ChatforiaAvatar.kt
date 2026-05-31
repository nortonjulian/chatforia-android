package com.chatforia.android.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chatforia.android.ui.theme.ChatforiaColors

@Composable
fun ChatforiaAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = ChatforiaColors.highlightedSurface
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "?",
                color = ChatforiaColors.primaryText,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}