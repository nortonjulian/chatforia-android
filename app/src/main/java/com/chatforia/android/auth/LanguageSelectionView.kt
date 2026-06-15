package com.chatforia.android.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chatforia.android.ui.theme.ChatforiaColors
import androidx.compose.ui.res.stringResource
import com.chatforia.android.R

@Composable
fun LanguageSelectionView(
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val selectedName =
        LANGUAGES
            .firstOrNull { language ->
                language.code == selectedLanguage
            }
            ?.name
            ?: "English"

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            onClick = {
                expanded = true
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = ChatforiaColors.cardBackground,
            border = BorderStroke(
                width = 1.dp,
                color = ChatforiaColors.border
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 18.dp,
                        vertical = 18.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedName,
                    color = ChatforiaColors.accent,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.android_language_selection_select_language),
                    tint = ChatforiaColors.accent
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            },
            containerColor = ChatforiaColors.cardBackground,
            shape = RoundedCornerShape(18.dp)
        ) {
            LANGUAGES.forEach { language ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = language.name,
                            color =
                                if (language.code == selectedLanguage)
                                    ChatforiaColors.accent
                                else
                                    ChatforiaColors.primaryText
                        )
                    },
                    onClick = {
                        onLanguageChange(language.code)
                        expanded = false
                    }
                )
            }
        }
    }
}