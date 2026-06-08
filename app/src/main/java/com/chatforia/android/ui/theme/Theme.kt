package com.chatforia.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance

@Composable
fun ChatforiaTheme(
    content: @Composable () -> Unit
) {
    val isLightTheme = ChatforiaColors.screenBackground.luminance() > 0.5f

    val colorScheme =
        if (isLightTheme) {
            lightColorScheme(
                primary = ChatforiaColors.accent,
                onPrimary = ChatforiaColors.buttonForeground,
                background = ChatforiaColors.screenBackground,
                onBackground = ChatforiaColors.primaryText,
                surface = ChatforiaColors.cardBackground,
                onSurface = ChatforiaColors.primaryText,
                surfaceVariant = ChatforiaColors.cardBackground,
                onSurfaceVariant = ChatforiaColors.secondaryText,
                outline = ChatforiaColors.border
            )
        } else {
            darkColorScheme(
                primary = ChatforiaColors.accent,
                onPrimary = ChatforiaColors.buttonForeground,
                background = ChatforiaColors.screenBackground,
                onBackground = ChatforiaColors.primaryText,
                surface = ChatforiaColors.cardBackground,
                onSurface = ChatforiaColors.primaryText,
                surfaceVariant = ChatforiaColors.cardBackground,
                onSurfaceVariant = ChatforiaColors.secondaryText,
                outline = ChatforiaColors.border
            )
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}