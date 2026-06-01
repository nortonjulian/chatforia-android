package com.chatforia.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.chatforia.android.ui.theme.ChatforiaColors
import androidx.compose.foundation.clickable

data class ChatforiaAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit = {}
)

@Composable
fun ChatforiaActionPill(
    actions: List<ChatforiaAction>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = ChatforiaColors.highlightedSurface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            actions.forEach { action ->

                Icon(
                    imageVector = action.icon,
                    contentDescription = action.contentDescription,
                    tint = ChatforiaColors.accent,

                    modifier =
                        Modifier.clickable {
                            action.onClick()
                        }
                )
            }
        }
    }
}