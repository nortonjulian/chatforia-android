package com.chatforia.android.crypto

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.chatforia.android.R
import com.chatforia.android.ChatforiaGradientButton

@Composable
fun DevicePairingScreen(
    onOpenLinkedDevices: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector =
                        Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription =
                        stringResource(
                            R.string.android_secure_messages_back
                        )
                )
            }

            Text(
                text = "Pair a New Device",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(stringResource(R.string.android_device_pairing_open_on_your_new_device_and_sign_in_if_it_needs_))

        Spacer(modifier = Modifier.height(20.dp))

        ChatforiaGradientButton(
            text = stringResource(R.string.android_device_pairing_view_pending_device_requests),
            onClick = onOpenLinkedDevices,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        )
    }
}
