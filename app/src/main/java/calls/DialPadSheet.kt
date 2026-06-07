package com.chatforia.android.calls

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatforia.android.ui.theme.ChatforiaColors
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.OutlinedTextFieldDefaults

@Composable
fun DialPadSheet(
    onDismiss: () -> Unit,
    onCall: (String) -> Unit
) {
    var number by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Dial",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = ChatforiaColors.primaryText
            )

            TextButton(onClick = onDismiss) {
                Text(
                    "Done",
                    color = ChatforiaColors.accent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = number,
            onValueChange = { value ->
                number = value.filter {
                    it.isDigit() || it == '+' || it == '*' || it == '#'
                }
            },
            placeholder = {
                Text("Enter number")
            },
            trailingIcon = {
                if (number.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            number = number.dropLast(1)
                        }
                    ) {
                        Icon(
                            Icons.Default.Backspace,
                            contentDescription = "Delete digit",
                            tint = ChatforiaColors.secondaryText
                        )
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            textStyle = LocalTextStyle.current.copy(
                fontSize = 28.sp,
                textAlign = TextAlign.Center
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = ChatforiaColors.primaryText,
                unfocusedTextColor = ChatforiaColors.primaryText,
                focusedPlaceholderColor = ChatforiaColors.secondaryText,
                unfocusedPlaceholderColor = ChatforiaColors.secondaryText,
                focusedContainerColor = ChatforiaColors.screenBackground,
                unfocusedContainerColor = ChatforiaColors.screenBackground,
                focusedBorderColor = ChatforiaColors.border,
                unfocusedBorderColor = ChatforiaColors.border,
                cursorColor = ChatforiaColors.accent
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        val rows = listOf(
            listOf("1" to "", "2" to "ABC", "3" to "DEF"),
            listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
            listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
            listOf("*" to "", "0" to "+", "#" to "")
        )

        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { key ->
                    DialKey(
                        number = key.first,
                        letters = key.second,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            number += key.first
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (number.isNotBlank()) {
                    onCall(number)
                }
            },
            enabled = number.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ChatforiaColors.accent
            )
        ) {
            Icon(Icons.Default.Call, contentDescription = null)

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                "Call",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DialKey(
    number: String,
    letters: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(20.dp),
        color = ChatforiaColors.cardBackground,
        tonalElevation = 1.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                number,
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                color = ChatforiaColors.primaryText
            )

            if (letters.isNotBlank()) {
                Text(
                    letters,
                    fontSize = 12.sp,
                    color = ChatforiaColors.secondaryText
                )
            }
        }
    }
}