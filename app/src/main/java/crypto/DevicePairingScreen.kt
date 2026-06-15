package com.chatforia.android.crypto

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.chatforia.android.R

@Composable
fun DevicePairingScreen(
    onOpenLinkedDevices: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Pair a New Device",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(stringResource(R.string.android_device_pairing_open_on_your_new_device_and_sign_in_if_it_needs_))

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onOpenLinkedDevices,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.android_device_pairing_view_pending_device_requests))
        }
    }
}