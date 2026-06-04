package com.chatforia.android.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chatforia.android.ui.theme.ChatforiaColors
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLogin: suspend (String, String) -> Unit,
    onGoogleLogin: suspend () -> Unit,
    onResetEncryption: suspend (String, String) -> Unit
) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val canLogin =
        identifier.trim().isNotEmpty() &&
                password.isNotEmpty() &&
                !isLoading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatforiaColors.screenBackground)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to Chatforia",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = ChatforiaColors.primaryText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Sign in or create an account",
            style = MaterialTheme.typography.titleMedium,
            color = ChatforiaColors.secondaryText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ChatforiaColors.cardBackground,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 2.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                errorMessage = null

                                try {
                                    onGoogleLogin()
                                } catch (error: Exception) {
                                    errorMessage =
                                        error.stackTraceToString()
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("Google")
                    }

                    OutlinedButton(
                        onClick = {
                            errorMessage = "Apple sign-in is not available yet."
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("Apple")
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = ChatforiaColors.border
                    )

                    Text(
                        text = "or",
                        color = ChatforiaColors.secondaryText,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = ChatforiaColors.border
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                TextButton(
                    onClick = {
                        errorMessage = "Forgot password is coming next."
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Forgot password?")
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    label = { Text("Email or username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    shape = RoundedCornerShape(18.dp)
                )

                errorMessage?.let { message ->

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (
                        message.contains(
                            "missing your encryption key",
                            ignoreCase = true
                        )
                    ) {

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    isLoading = true

                                    try {
                                        onResetEncryption(
                                            identifier.trim(),
                                            password
                                        )
                                    } catch (error: Exception) {
                                        errorMessage =
                                            error.message ?: "Failed to reset encryption."
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = canLogin
                        ) {
                            Text("Reset Encryption")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            errorMessage = null

                            try {
                                onLogin(
                                    identifier.trim(),
                                    password
                                )
                            } catch (error: Exception) {
                                errorMessage =
                                    error.message ?: "Login failed."
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = canLogin,
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        if (isLoading) {
                            "Signing in..."
                        } else {
                            "Log in"
                        }
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = "Don’t have an account yet?",
                    color = ChatforiaColors.secondaryText,
                    style = MaterialTheme.typography.bodyMedium
                )

                TextButton(
                    onClick = {
                        errorMessage = "Create account is coming next."
                    }
                ) {
                    Text("Create an account")
                }
            }
        }
    }
}