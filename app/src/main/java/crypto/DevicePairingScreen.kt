package com.chatforia.android.crypto

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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

        Text(
            "Open Chatforia on your new device and sign in. If it needs access to your encrypted chats, approve it from a trusted device."
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onOpenLinkedDevices,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View pending device requests")
        }
    }
}