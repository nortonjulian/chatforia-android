package com.chatforia.android.crypto

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chatforia.android.R
import com.chatforia.android.ui.theme.ChatforiaColors

@Composable
fun SecureMessageRecoveryScreen(
    viewModel: KeySetupViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatforiaColors.screenBackground)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 4.dp,
                    top = 8.dp,
                    end = 16.dp,
                    bottom = 4.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector =
                        Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription =
                        stringResource(
                            R.string.android_secure_messages_back
                        ),
                    tint = ChatforiaColors.primaryText
                )
            }

            Text(
                text =
                    stringResource(
                        R.string.android_secure_messages_recovery_title
                    ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = ChatforiaColors.primaryText
            )
        }

        Text(
            text =
                stringResource(
                    R.string.android_secure_messages_recovery_description
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = ChatforiaColors.secondaryText,
            modifier = Modifier.padding(
                start = 20.dp,
                end = 20.dp,
                bottom = 4.dp
            )
        )

        KeySetupScreen(
            viewModel = viewModel,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
