package com.chatforia.android.auth

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.Column
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

@Composable
fun MfaLoginDialog(onConfirm: suspend (String) -> Unit, onCancel: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!submitting) onCancel() },
        title = { Text("Two-factor verification") },
        text = {
            Column {
                Text("Enter your authenticator code or a recovery code.")
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it; error = null },
                    label = { Text("Verification code") },
                    singleLine = true,
                    enabled = !submitting
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(enabled = code.isNotBlank() && !submitting, onClick = {
                submitting = true
                scope.launch {
                    try {
                        onConfirm(code)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        error = "Unable to verify. Check your code or cancel and sign in again."
                    } finally {
                        submitting = false
                    }
                }
            }) { Text(if (submitting) "Verifying…" else "Verify") }
        },
        dismissButton = {
            TextButton(enabled = !submitting, onClick = onCancel) { Text("Cancel") }
        }
    )
}
