package com.chatforia.android.pickers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VideoCameraBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chatforia.android.ui.theme.ChatforiaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPickerSheet(
    onDismiss: () -> Unit,
    onPickPhoto: () -> Unit,
    onPickVideo: () -> Unit,
    onPickGif: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(
            topStart = 32.dp,
            topEnd = 32.dp
        ),
        containerColor = ChatforiaColors.cardBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 36.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MediaOption(
                icon = Icons.Default.Image,
                label = "Photo",
                onClick = onPickPhoto
            )

            MediaOption(
                icon = Icons.Default.VideoCameraBack,
                label = "Video",
                onClick = onPickVideo
            )

            MediaOption(
                icon = Icons.Default.AutoAwesome,
                label = "GIF",
                onClick = onPickGif
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun MediaOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = Modifier.size(72.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = ChatforiaColors.highlightedSurface,
                contentColor = ChatforiaColors.accent
            )
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(label)
    }
}