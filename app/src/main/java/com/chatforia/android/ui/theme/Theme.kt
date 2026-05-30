package com.chatforia.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ChatforiaLightColorScheme = lightColorScheme(
    primary = ChatforiaAccent,
    onPrimary = Color(0xFF2B1712),

    secondary = ChatforiaTitleAccent,
    onSecondary = Color.White,

    background = ChatforiaScreenBackground,
    onBackground = ChatforiaPrimaryText,

    surface = ChatforiaCardBackground,
    onSurface = ChatforiaPrimaryText,

    surfaceVariant = ChatforiaCardBackground,
    onSurfaceVariant = ChatforiaSecondaryText,

    outline = ChatforiaBorder,

    error = Color(0xFFE53935)
)

@Composable
fun ChatforiaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ChatforiaLightColorScheme,
        typography = Typography,
        content = content
    )
}