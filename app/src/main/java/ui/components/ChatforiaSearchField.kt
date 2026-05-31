package com.chatforia.android.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chatforia.android.ui.theme.ChatforiaColors

@Composable
fun ChatforiaSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(placeholder)
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                tint = ChatforiaColors.secondaryText
            )
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = ChatforiaColors.cardBackground,
            unfocusedContainerColor = ChatforiaColors.cardBackground,
            focusedBorderColor = ChatforiaColors.border,
            unfocusedBorderColor = ChatforiaColors.border
        ),
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        singleLine = true
    )
}