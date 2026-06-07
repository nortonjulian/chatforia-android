package com.chatforia.android.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

object ChatforiaColors {
    private var palette by mutableStateOf(themePalette("dawn"))

    val screenBackground: Color get() = palette.screenBackground
    val cardBackground: Color get() = palette.cardBackground
    val border: Color get() = palette.border
    val primaryText: Color get() = palette.primaryText
    val secondaryText: Color get() = palette.secondaryText
    val accent: Color get() = palette.accent
    val highlightedSurface: Color get() = palette.highlightedSurface
    val buttonStart: Color get() = palette.buttonStart
    val buttonEnd: Color get() = palette.buttonEnd
    val buttonForeground: Color get() = readableTextFor(buttonEnd)

    fun applyTheme(code: String?) {
        palette = themePalette(code ?: "dawn")
    }
}

data class ChatforiaPalette(
    val screenBackground: Color,
    val cardBackground: Color,
    val border: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val accent: Color,
    val highlightedSurface: Color,
    val buttonStart: Color,
    val buttonEnd: Color
)

private fun readableTextFor(background: Color): Color {
    val luminance =
        0.299f * background.red +
                0.587f * background.green +
                0.114f * background.blue

    return if (luminance > 0.55f) {
        Color(0xFF1A120B)
    } else {
        Color.White
    }
}

private fun themePalette(code: String): ChatforiaPalette {
    return when (code.lowercase()) {
        "midnight" -> ChatforiaPalette(
            screenBackground = Color(0xFF0B1020),
            cardBackground = Color(0xFF121933),
            border = Color(0xFF1E2747),
            primaryText = Color(0xFFEEF2FF),
            secondaryText = Color(0xFFA3AED0),
            accent = Color(0xFF3CF9FF),
            highlightedSurface = Color(0xFFFEF3C7),
            buttonStart = Color(0xFF6A3CC1),
            buttonEnd = Color(0xFF00C2A8)
        )

        "amoled" -> ChatforiaPalette(
            screenBackground = Color.Black,
            cardBackground = Color(0xFF0A0A0A),
            border = Color(0xFF141414),
            primaryText = Color(0xFFF2F2F2),
            secondaryText = Color(0xFFA3A3A3),
            accent = Color(0xFF7C3AED),
            highlightedSurface = Color(0xFF171717),
            buttonStart = Color(0xFF7C3AED),
            buttonEnd = Color(0xFF5B21B6)
        )

        "aurora" -> ChatforiaPalette(
            screenBackground = Color(0xFF061A1A),
            cardBackground = Color(0xFF0B2323),
            border = Color(0xFF123131),
            primaryText = Color(0xFFE7FFF9),
            secondaryText = Color(0xFFA7D7CF),
            accent = Color(0xFF29D39A),
            highlightedSurface = Color(0xFF133C36),
            buttonStart = Color(0xFF00C2A8),
            buttonEnd = Color(0xFF29D39A)
        )

        "neon" -> ChatforiaPalette(
            screenBackground = Color(0xFF0B0F14),
            cardBackground = Color(0xFF0F141D),
            border = Color(0xFF171821),
            primaryText = Color(0xFFEAF2FF),
            secondaryText = Color(0xFF9FB3D1),
            accent = Color(0xFF3CF9FF),
            highlightedSurface = Color(0xFF142536),
            buttonStart = Color(0xFF3CF9FF),
            buttonEnd = Color(0xFF6A3CC1)
        )

        "sunset" -> ChatforiaPalette(
            screenBackground = Color(0xFFFFF1E8),
            cardBackground = Color(0xFFFFF7F3),
            border = Color(0xFFF2D9CB),
            primaryText = Color(0xFF2C1A10),
            secondaryText = Color(0xFF7B6152),
            accent = Color(0xFFFF9800),
            highlightedSurface = Color(0xFFFFF4D8),
            buttonStart = Color(0xFFFFB300),
            buttonEnd = Color(0xFFFF9800)
        )

        "solarized" -> ChatforiaPalette(
            screenBackground = Color(0xFFFDF6E3),
            cardBackground = Color.White,
            border = Color(0xFFEEE8D5),
            primaryText = Color(0xFF073642),
            secondaryText = Color(0xFF657B83),
            accent = Color(0xFFB58900),
            highlightedSurface = Color(0xFFFDF0C0),
            buttonStart = Color(0xFFB58900),
            buttonEnd = Color(0xFFCB4B16)
        )

        "velvet" -> ChatforiaPalette(
            screenBackground = Color(0xFF150919),
            cardBackground = Color(0xFF1F0D26),
            border = Color(0xFF2B1331),
            primaryText = Color(0xFFFFF3FA),
            secondaryText = Color(0xFFE8B9D6),
            accent = Color(0xFFE91E63),
            highlightedSurface = Color(0xFF35162E),
            buttonStart = Color(0xFFE91E63),
            buttonEnd = Color(0xFFFFB300)
        )

        else -> ChatforiaPalette(
            screenBackground = Color(0xFFFFF7F0),
            cardBackground = Color.White,
            border = Color(0xFFF1E3D8),
            primaryText = Color(0xFF241510),
            secondaryText = Color(0xFF745E53),
            accent = Color(0xFFFFB300),
            highlightedSurface = Color(0xFFFFF4D0),
            buttonStart = Color(0xFFFFB300),
            buttonEnd = Color(0xFFFF9800)
        )
    }
}