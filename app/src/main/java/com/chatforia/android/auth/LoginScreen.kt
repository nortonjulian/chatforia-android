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
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.chatforia.android.R

@Composable
fun LoginScreen(
    onLogin: suspend (String, String) -> Unit,
    onGoogleLogin: suspend () -> Unit,
    onAppleLogin: suspend () -> Unit,
    onCreateAccount: () -> Unit,
    onResetEncryption: suspend (String, String) -> Unit,
    onForgotPassword: suspend (String) -> Unit,
    onResendVerification: suspend (String) -> Unit,
) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    var canResendVerification by remember { mutableStateOf(false) }

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
            text = stringResource(R.string.android_login_welcome_to),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = ChatforiaColors.primaryText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.android_login_sign_in_or_create_an_account),
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
                            scope.launch {
                                isLoading = true
                                errorMessage = null

                                try {
                                    onAppleLogin()
                                } catch (error: Exception) {
                                    errorMessage =
                                        error.message ?: "Apple sign-in failed."
                                } finally {
                                    isLoading = false
                                }
                            }
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
                        text = stringResource(R.string.android_login_or),
                        color = ChatforiaColors.secondaryText,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = ChatforiaColors.border
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    label = { Text(stringResource(R.string.android_login_email_or_username)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.android_login_password)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    shape = RoundedCornerShape(18.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                TextButton(
                    onClick = {
                        scope.launch {
                            val value = identifier.trim()

                            if (value.isBlank()) {
                                errorMessage = "Enter your email, username, or phone number first."
                                return@launch
                            }

                            isLoading = true
                            errorMessage = null

                            try {
                                onForgotPassword(value)
                                errorMessage = "If an account exists, we’ll send password reset instructions."
                            } catch (error: Exception) {
                                errorMessage = "If an account exists, we’ll send password reset instructions."
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        "Forgot password?",
                        color = ChatforiaColors.accent
                    )
                }

                errorMessage?.let { message ->

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (canResendVerification) {

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        onResendVerification(identifier.trim())

                                        errorMessage =
                                            "Verification email sent. Check your inbox."

                                        canResendVerification = false

                                    } catch (_: Exception) {

                                        errorMessage =
                                            "If that account exists, we'll send a verification email."
                                    }
                                }
                            }
                        ) {
                            Text(
                                "Resend verification email",
                                color = ChatforiaColors.accent
                            )
                        }
                    }

                    if (
                        message.contains(
                            "missing your encryption key",
                            ignoreCase = true
                        )
                    ) {

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ChatforiaColors.accent
                            ),
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
                            Text(stringResource(R.string.android_login_reset_encryption))
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
                                val raw = error.message.orEmpty()

                                errorMessage =
                                    when {
                                        raw.contains(
                                            "email_not_verified",
                                            ignoreCase = true
                                        ) ||
                                                raw.contains(
                                                    "Please verify your email",
                                                    ignoreCase = true
                                                ) -> {
                                            canResendVerification = true
                                            "Please verify your email before logging in. Check your inbox for the Chatforia verification link."
                                        }

                                        else ->
                                            "Login failed. Please try again."
                                    }
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = canLogin,
                    shape = RoundedCornerShape(28.dp),

                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    ),

                    border = BorderStroke(
                        1.dp,
                        Color.Transparent
                    ),

                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        ChatforiaColors.buttonStart,
                                        ChatforiaColors.buttonEnd
                                    )
                                ),
                                shape = RoundedCornerShape(28.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text =
                                if (isLoading) {
                                    "Signing in..."
                                } else {
                                    "Log in"
                                },
                            color = ChatforiaColors.buttonForeground,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = stringResource(R.string.android_login_don_t_have_an_account_yet),
                    color = ChatforiaColors.secondaryText,
                    style = MaterialTheme.typography.bodyMedium
                )

                TextButton(
                    onClick = onCreateAccount
                ) {
                    Text(
                        "Create an account",
                        color = ChatforiaColors.accent
                    )
                }
            }
        }
    }
}