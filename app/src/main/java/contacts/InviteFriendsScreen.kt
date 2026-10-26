package com.chatforia.android.contacts

import android.content.Intent
import android.telephony.PhoneNumberUtils
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Contacts
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.chatforia.android.ui.theme.ChatforiaColors
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.chatforia.android.R

@Composable
fun InviteFriendsScreen(
    repository: InviteRepository,
    currentUsername: String?,
    onBack: () -> Unit,
    onInviteSms: (phone: String, message: String) -> Unit,
    isOpeningSmsThread: Boolean = false,
    smsThreadError: String? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var recipientPhone by remember { mutableStateOf("") }
    val isBusy = isLoading || isOpeningSmsThread
    val contactPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.data?.let { uri ->
            runCatching {
                context.contentResolver.query(
                    uri,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) recipientPhone = cursor.getString(0).orEmpty()
                }
            }.onFailure { error = "Could not read selected phone number." }
        }
    }

    fun shareText(inviteUrl: String): String {
        val name = currentUsername?.trim()?.takeIf { it.isNotBlank() }

        return if (name != null) {
            "$name invited you to join Chatforia. Download or join here: $inviteUrl"
        } else {
            "Join me on Chatforia. Download or join here: $inviteUrl"
        }
    }

    fun normalizeRecipient(raw: String): String? =
        normalizeInvitePhoneNumber(
            raw = raw,
            formattedE164 = PhoneNumberUtils.formatNumberToE164(
                raw.trim(),
                Locale.getDefault().country
            )
        )

    fun createSmsInvite() {
        val phone = normalizeRecipient(recipientPhone)
        if (phone == null) {
            error = "Enter a valid recipient phone number."
            return
        }
        coroutineScope.launch {
            isLoading = true
            error = null
            try {
                val response = repository.createInvite(targetPhone = phone, channel = "share_link")
                onInviteSms(phone, shareText(response.url))
            } catch (e: Exception) {
                error = e.message ?: "Could not create SMS invitation."
            } finally {
                isLoading = false
            }
        }
    }

    fun createAndShareInvite() {
        coroutineScope.launch {
            isLoading = true
            error = null

            try {
                val response =
                    repository.createInvite(
                        channel = "share_link"
                    )

                val sendIntent =
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            shareText(response.url)
                        )
                    }

                val chooser =
                    Intent.createChooser(
                        sendIntent,
                        "Invite friends"
                    )

                context.startActivity(chooser)

            } catch (e: Exception) {
                error = e.message ?: "Failed to create invite."
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatforiaColors.screenBackground)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.android_plan_back),
                    tint = ChatforiaColors.primaryText
                )
            }

            Text(
                text = stringResource(R.string.android_invite_friends_invite_friends),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = ChatforiaColors.primaryText
            )
        }

        Text(
            text = "Send a personalized invitation to someone using their regular phone number, or share your link through another app.",
            color = ChatforiaColors.secondaryText,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Invite via Chatforia SMS",
            style = MaterialTheme.typography.titleMedium,
            color = ChatforiaColors.primaryText
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = recipientPhone,
                onValueChange = { recipientPhone = it },
                label = { Text("Recipient phone number") },
                placeholder = { Text("Enter a phone number") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true
            )
            IconButton(
                enabled = !isBusy,
                onClick = {
                    contactPicker.launch(
                        Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                    )
                }
            ) {
                Icon(Icons.Default.Contacts, contentDescription = "Choose phone contact")
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "This is the person you are inviting—not your Chatforia number.",
            style = MaterialTheme.typography.bodySmall,
            color = ChatforiaColors.secondaryText
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = { createSmsInvite() },
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.Message, contentDescription = null)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isBusy) "Opening SMS invitation…" else "Review SMS invitation")
        }

        (error ?: smsThreadError)?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Or share with another app",
            style = MaterialTheme.typography.titleMedium,
            color = ChatforiaColors.primaryText
        )
        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { createAndShareInvite() },
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(stringResource(R.string.android_invite_friends_share_invite_link))
            }
        }

    }
}