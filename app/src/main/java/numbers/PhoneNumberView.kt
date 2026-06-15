package com.chatforia.android.numbers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chatforia.android.auth.UserDto
import com.chatforia.android.network.ApiClient
import com.chatforia.android.ui.components.ChatforiaSectionCard
import com.chatforia.android.ui.theme.ChatforiaColors
import kotlinx.coroutines.launch
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.ui.res.stringResource
import com.chatforia.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneNumberView(
    apiClient: ApiClient,
    user: UserDto,
    onBack: () -> Unit,
    onUpgradeRequired: () -> Unit
) {
    val repository = remember { PhoneNumberRepository(apiClient) }
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var isReleasing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var response by remember { mutableStateOf<MyNumberResponse?>(null) }
    var showPicker by remember { mutableStateOf(false) }

    fun reload() {
        scope.launch {
            isLoading = true
            error = null

            try {
                response = repository.getMyNumber()
            } catch (e: Exception) {
                error = e.message ?: "Could not load your number."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        reload()
    }

    if (showPicker) {
        ModalBottomSheet(
            onDismissRequest = {
                showPicker = false
                reload()
            },
            containerColor = ChatforiaColors.screenBackground
        ) {
            PickNumberSheet(
                apiClient = apiClient,
                user = user,
                currentNumber = response?.number,
                onDismiss = {
                    showPicker = false
                    reload()
                },
                onUpgradeRequired = onUpgradeRequired
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatforiaColors.screenBackground)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.android_plan_back),
                    tint = ChatforiaColors.primaryText
                )
            }

            Text(
                text = stringResource(R.string.android_profile_phone_number),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = ChatforiaColors.primaryText
            )
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                Text(
                    text = error ?: "Something went wrong.",
                    color = MaterialTheme.colorScheme.error
                )
            }

            response?.number != null -> {
                val number = response?.number
                val policy = response?.policy

                ChatforiaSectionCard(title = stringResource(R.string.android_phone_number_your_number)) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            tint = ChatforiaColors.accent,
                            modifier = Modifier.size(36.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = number?.e164 ?: "",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = ChatforiaColors.primaryText
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        StatusBadge(number?.status ?: "ASSIGNED")

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = when {
                                number?.keepLocked == true ->
                                    "Protected from automatic recycling."

                                policy?.description != null ->
                                    policy.description

                                else ->
                                    "Numbers may recycle after inactivity on the Free plan."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = ChatforiaColors.secondaryText
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { showPicker = true },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.widthIn(min = 180.dp, max = 260.dp)
                        ) {
                            Text(stringResource(R.string.android_phone_number_replace_number))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isReleasing = true
                                    try {
                                        repository.releaseNumber()
                                        reload()
                                    } catch (e: Exception) {
                                        error = e.message ?: "Could not release number."
                                    } finally {
                                        isReleasing = false
                                    }
                                }
                            },
                            enabled = !isReleasing && number?.keepLocked != true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isReleasing) "Releasing..." else "Release Number")
                        }
                    }
                }
            }

            else -> {
                ChatforiaSectionCard(title = stringResource(R.string.android_phone_number_your_number)) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.android_phone_number_no_number_assigned),
                            style = MaterialTheme.typography.bodyLarge,
                            color = ChatforiaColors.secondaryText
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = { showPicker = true },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ChatforiaColors.accent,
                                contentColor = ChatforiaColors.buttonForeground
                            ),
                            modifier = Modifier.widthIn(min = 180.dp, max = 260.dp)
                        ) {
                            Text(stringResource(R.string.android_phone_number_pick_a_number))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    status: String
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = ChatforiaColors.cardBackground
    ) {
        Text(
            text = status.uppercase(),
            color = ChatforiaColors.primaryText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}