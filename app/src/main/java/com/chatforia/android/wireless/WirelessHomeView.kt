package com.chatforia.android.wireless

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import com.chatforia.android.network.ApiClient
import com.chatforia.android.network.ApiException
import com.chatforia.android.ui.components.ChatforiaSectionCard
import com.chatforia.android.ui.theme.ChatforiaColors
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.chatforia.android.R
import com.chatforia.android.ChatforiaGradientButton
import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WirelessHomeView(
    apiClient: ApiClient,
    currentUserId: Int,
    onBack: () -> Unit
) {
    val repository = remember { WirelessRepository(apiClient) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val checkoutStore =
        remember(context) {
            WirelessCheckoutStore(
                context.applicationContext
            )
        }

    var selectedScope by remember { mutableStateOf(EsimScope.LOCAL) }
    var status by remember { mutableStateOf<WirelessStatusResponse?>(null) }
    var esim by remember { mutableStateOf<EsimSubscriberDto?>(null) }
    var showEsimActivationSheet by remember { mutableStateOf(false) }
    var showReplaceEsimConfirmation by remember { mutableStateOf(false) }
    var isReplacingEsim by remember { mutableStateOf(false) }
    var replacementError by remember { mutableStateOf<String?>(null) }
    var replacementIdempotencyKey by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isPurchasing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var pendingCheckout by remember {
        mutableStateOf(checkoutStore.load(currentUserId))
    }

    fun clearPendingCheckout() {
        checkoutStore.clear()
        pendingCheckout = null
    }

    suspend fun reconcilePendingCheckout() {
        val pending =
            pendingCheckout
                ?: checkoutStore.load(currentUserId)
                    ?.also {
                        pendingCheckout = it
                    }
                ?: return

        val checkoutStatus =
            try {
                repository.getCheckoutStatus(
                    pending.sessionId
                )
            } catch (e: ApiException) {
                if (
                    e.statusCode == 401 ||
                    e.statusCode == 403 ||
                    e.statusCode == 404
                ) {
                    clearPendingCheckout()
                    return
                }

                throw e
            }

        val expired =
            checkoutStatus.status
                ?.equals(
                    "EXPIRED",
                    ignoreCase = true
                ) == true ||
                checkoutStatus.sessionStatus
                    ?.equals(
                        "expired",
                        ignoreCase = true
                    ) == true

        val complete =
            checkoutStatus.complete &&
                checkoutStatus.paid &&
                checkoutStatus.provisioned

        if (expired || complete) {
            clearPendingCheckout()
        }
    }

    fun reload() {
        scope.launch {
            isLoading = true
            error = null

            try {
                try {
                    reconcilePendingCheckout()
                } catch (_: Exception) {
                    // Keep the saved session during temporary
                    // network or Stripe-status failures.
                }

                status = repository.getWirelessStatus()
                esim = repository.getCurrentEsim().subscriber
            } catch (e: Exception) {
                error = e.message ?: "Could not load wireless details."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(currentUserId) {
        reload()
    }

    val canReplaceEsim =
        esim != null &&
            (status?.source?.id ?: 0) > 0 &&
            status?.exhausted != true &&
            status?.expired != true &&
            status?.source?.remainingDataMb != 0 &&
            status?.source?.addonKind
                ?.uppercase()
                ?.contains("UNLIMITED") != true

    val lifecycleOwner =
        LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (
                    event ==
                    Lifecycle.Event.ON_RESUME
                ) {
                    reload()
                }
            }

        lifecycleOwner.lifecycle
            .addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle
                .removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatforiaColors.screenBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.android_plan_back),
                    tint = ChatforiaColors.primaryText
                )
            }

            Text(
                text = stringResource(R.string.android_profile_wireless),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = ChatforiaColors.primaryText
            )
        }

        HeroSection()

        Spacer(modifier = Modifier.height(16.dp))

        CoverageSection(
            selectedScope = selectedScope,
            onScopeChange = { selectedScope = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        EsimSection(
            esim = esim,
            onActivateEsim = {
                showEsimActivationSheet = true
            },
            onManageEsim = {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        "https://chatforia.com/account/esim".toUri()
                    )
                )
            },
            onSetupEsim = {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        "https://chatforia.com/upgrade?section=mobile".toUri()
                    )
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        UsageSection(
            isLoading = isLoading,
            error = error,
            status = status
        )

        Spacer(modifier = Modifier.height(16.dp))

        PackListSection(
            packs = packsFor(selectedScope),
            isPurchasing = isPurchasing,
            onChoosePack = { pack ->
                if (isPurchasing) {
                    return@PackListSection
                }

                isPurchasing = true
                error = null

                scope.launch {
                    try {
                        val existingPending =
                            pendingCheckout
                                ?: checkoutStore.load(currentUserId)
                                    ?.also {
                                        pendingCheckout = it
                                    }

                        if (existingPending != null) {
                            val checkoutStatus =
                                try {
                                    repository.getCheckoutStatus(
                                        existingPending.sessionId
                                    )
                                } catch (e: ApiException) {
                                    if (
                                        e.statusCode == 401 ||
                                        e.statusCode == 403 ||
                                        e.statusCode == 404
                                    ) {
                                        clearPendingCheckout()
                                        return@launch
                                    }

                                    null
                                } catch (_: Exception) {
                                    /*
                                     * Preserve this user's session during
                                     * temporary connectivity failures.
                                     */
                                    null
                                }

                            val expired =
                                checkoutStatus?.status
                                    ?.equals(
                                        "EXPIRED",
                                        ignoreCase = true
                                    ) == true ||
                                    checkoutStatus?.sessionStatus
                                        ?.equals(
                                            "expired",
                                            ignoreCase = true
                                        ) == true

                            val complete =
                                checkoutStatus?.complete == true &&
                                    checkoutStatus.paid &&
                                    checkoutStatus.provisioned

                            when {
                                complete -> {
                                    clearPendingCheckout()
                                    reload()
                                    return@launch
                                }

                                expired -> {
                                    clearPendingCheckout()
                                }

                                checkoutStatus?.paid == true -> {
                                    error =
                                        "Payment received. Finishing your eSIM setup…"
                                    reload()
                                    return@launch
                                }

                                else -> {
                                    // Reopen the same Stripe session.
                                    // This prevents another Checkout
                                    // Session from being created.
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            existingPending
                                                .checkoutUrl
                                                .toUri()
                                        )
                                    )

                                    return@launch
                                }
                            }
                        }

                        val checkoutAttemptId =
                            UUID.randomUUID().toString()

                        val response =
                            repository.startCheckout(
                                product = pack.product,
                                checkoutAttemptId =
                                    checkoutAttemptId
                            )

                        val checkoutUrl =
                            response.resolvedCheckoutUrl()
                                ?: throw IllegalStateException(
                                    "Stripe checkout did not return a valid URL."
                                )

                        val sessionId =
                            response.sessionId
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: throw IllegalStateException(
                                    "Stripe checkout did not return a session ID."
                                )

                        val newPending =
                            PendingWirelessCheckout(
                                userId = currentUserId,
                                product = pack.product,
                                checkoutAttemptId =
                                    checkoutAttemptId,
                                sessionId = sessionId,
                                checkoutUrl = checkoutUrl
                            )

                        checkoutStore.save(newPending)
                        pendingCheckout = newPending

                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                checkoutUrl.toUri()
                            )
                        )
                    } catch (e: Exception) {
                        error =
                            e.message
                                ?: "Could not start wireless checkout."
                    } finally {
                        isPurchasing = false
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        ChatforiaSectionCard(title = stringResource(R.string.android_wireless_home_manage)) {
            OutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            "https://chatforia.com/account/esim".toUri()
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(R.string.android_wireless_home_manage_wireless))
            }

            if (canReplaceEsim) {
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        replacementError = null
                        showReplaceEsimConfirmation = true
                    },
                    enabled = !isReplacingEsim,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Request eSIM for a new phone")
                }
            }

//            Spacer(modifier = Modifier.height(10.dp))
//
//            OutlinedButton(
//                onClick = {},
//                modifier = Modifier.fillMaxWidth(),
//                shape = RoundedCornerShape(16.dp)
//            ) {
//                Text(stringResource(R.string.android_wireless_home_port_my_number))
//            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.android_wireless_home_esim_data_packs_require_an_esim_compatible_and_u),
            color = ChatforiaColors.secondaryText,
            style = MaterialTheme.typography.bodySmall
        )
    }

    if (showReplaceEsimConfirmation) {
        AlertDialog(
            onDismissRequest = {
                if (!isReplacingEsim) {
                    showReplaceEsimConfirmation = false
                    replacementIdempotencyKey = null
                    replacementError = null
                }
            },
            title = {
                Text("Request eSIM for a new phone?")
            },
            text = {
                Column {
                    Text(
                        "Your remaining data and expiration date will move " +
                            "to a new eSIM. After replacement succeeds, " +
                            "the old eSIM will stop working."
                    )

                    replacementError?.let { message ->
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (isReplacingEsim) {
                            return@TextButton
                        }

                        val purchaseId =
                            status?.source?.id
                                ?.takeIf { it > 0 }
                                ?: return@TextButton

                        val key =
                            replacementIdempotencyKey
                                ?: UUID.randomUUID()
                                    .toString()
                                    .also {
                                        replacementIdempotencyKey = it
                                    }

                        replacementError = null
                        isReplacingEsim = true

                        scope.launch {
                            try {
                                val result =
                                    repository.replaceEsim(
                                        purchaseId = purchaseId,
                                        idempotencyKey = key
                                    )

                                if (!result.ok) {
                                    replacementError =
                                        "Chatforia could not replace this eSIM."
                                    return@launch
                                }

                                // The backend confirmed the logical
                                // replacement. A future replacement
                                // should use a new idempotency key.
                                replacementIdempotencyKey = null
                                showReplaceEsimConfirmation = false

                                // Re-read canonical state instead of
                                // treating the replacement response as
                                // the long-term subscriber state.
                                status =
                                    repository.getWirelessStatus()

                                esim =
                                    repository.getCurrentEsim()
                                        .subscriber

                                if (esim != null) {
                                    showEsimActivationSheet = true
                                } else {
                                    replacementError =
                                        "Your replacement eSIM was created, " +
                                            "but its installation details " +
                                            "could not be loaded. Please try " +
                                            "Manage Wireless again."
                                }
                            } catch (e: Exception) {
                                // Keep the idempotency key for this
                                // logical attempt. Retrying with the
                                // same key prevents duplicate
                                // replacement work after an ambiguous
                                // network/provider failure.
                                replacementError =
                                    e.message
                                        ?: "Could not request a new eSIM."
                            } finally {
                                isReplacingEsim = false
                            }
                        }
                    },
                    enabled = !isReplacingEsim
                ) {
                    Text(
                        if (isReplacingEsim)
                            "Requesting…"
                        else
                            "Request new eSIM"
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showReplaceEsimConfirmation = false
                        replacementIdempotencyKey = null
                        replacementError = null
                    },
                    enabled = !isReplacingEsim
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEsimActivationSheet) {
        esim?.let { currentEsim ->
            ModalBottomSheet(
                onDismissRequest = {
                    showEsimActivationSheet = false
                },
                containerColor = ChatforiaColors.screenBackground
            ) {
                EsimActivationSheet(
                    esim = currentEsim,
                    onClose = {
                        showEsimActivationSheet = false
                    }
                )
            }
        }
    }
}

@Composable
private fun EsimActivationSheet(
    esim: EsimSubscriberDto,
    onClose: () -> Unit
) {
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Install your eSIM",
            color = ChatforiaColors.primaryText,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text =
                "Open your phone’s SIM settings and choose to add an eSIM. " +
                    "If Android asks for carrier details, use the information below.",
            color = ChatforiaColors.secondaryText
        )

        esim.smdp
            ?.takeIf { it.isNotBlank() }
            ?.let { smdp ->
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "SM-DP+ address",
                    color = ChatforiaColors.secondaryText,
                    style = MaterialTheme.typography.labelLarge
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = smdp,
                    color = ChatforiaColors.primaryText
                )

                TextButton(
                    onClick = {
                        clipboard.setText(
                            AnnotatedString(smdp)
                        )
                    }
                ) {
                    Text("Copy address")
                }
            }

        esim.activationCode
            ?.takeIf { it.isNotBlank() }
            ?.let { activationCode ->
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Activation code",
                    color = ChatforiaColors.secondaryText,
                    style = MaterialTheme.typography.labelLarge
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = activationCode,
                    color = ChatforiaColors.primaryText
                )

                TextButton(
                    onClick = {
                        clipboard.setText(
                            AnnotatedString(activationCode)
                        )
                    }
                ) {
                    Text("Copy activation code")
                }
            }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text =
                "After Android finishes installing the eSIM, return to Chatforia. " +
                    "We’ll check its activation status automatically.",
            color = ChatforiaColors.secondaryText,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedButton(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Done")
        }
    }
}

@Composable
private fun HeroSection() {
    ChatforiaSectionCard(title = stringResource(R.string.android_profile_mobile)) {
        Text(
            text = stringResource(R.string.android_wireless_home_stay_connected_when_you_re_traveling_or_away_fro),
            color = ChatforiaColors.primaryText,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.android_wireless_home_choose_a_one_time_esim_data_pack_for_local_europ),
            color = ChatforiaColors.secondaryText,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun CoverageSection(
    selectedScope: EsimScope,
    onScopeChange: (EsimScope) -> Unit
) {
    ChatforiaSectionCard(title = stringResource(R.string.android_wireless_home_coverage)) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            EsimScope.entries.forEachIndexed { index, scope ->
                SegmentedButton(
                    selected = selectedScope == scope,
                    onClick = { onScopeChange(scope) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = EsimScope.entries.size
                    ),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = ChatforiaColors.accent,
                        activeContentColor = Color.White,
                        inactiveContainerColor = ChatforiaColors.cardBackground,
                        inactiveContentColor = ChatforiaColors.primaryText,
                        activeBorderColor = ChatforiaColors.border,
                        inactiveBorderColor = ChatforiaColors.border
                    )
                ) {
                    Text(scope.label)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = selectedScope.subtitle,
            color = ChatforiaColors.secondaryText,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.android_wireless_home_we_don_t_sell_data_packs_under_3_gb),
            color = ChatforiaColors.secondaryText,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun EsimSection(
    esim: EsimSubscriberDto?,
    onActivateEsim: () -> Unit,
    onManageEsim: () -> Unit,
    onSetupEsim: () -> Unit
) {
    ChatforiaSectionCard(title = stringResource(R.string.android_wireless_home_your_esim)) {
        val hasEsim = esim != null
        val esimStatus = esim?.status?.uppercase()
        val needsInstallation =
            esimStatus == "PENDING" ||
                esimStatus == "PROVISIONING"
        val isActive = esimStatus == "ACTIVE"

        val headline =
            when {
                !hasEsim ->
                    "You don’t have an eSIM yet."
                isActive ->
                    "Your eSIM is active."
                needsInstallation ->
                    "Your eSIM is ready to install."
                else ->
                    "Your eSIM is available."
            }

        Text(
            text = headline,
            color = ChatforiaColors.primaryText,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        if (needsInstallation) {
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text =
                    esim?.lpaUri
                        ?: esim?.qrPayload
                        ?: "Activation details will appear here.",
                color = ChatforiaColors.secondaryText,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        ChatforiaGradientButton(
            text =
                when {
                    !hasEsim -> "Set up"
                    needsInstallation -> "Activate"
                    else -> "Manage eSIM"
                },
            onClick = {
                when {
                    !hasEsim ->
                        onSetupEsim()
                    needsInstallation ->
                        onActivateEsim()
                    else ->
                        onManageEsim()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            leadingIcon = {
                Icon(
                    Icons.Default.SimCard,
                    contentDescription = null,
                    tint = ChatforiaColors.buttonForeground
                )
            }
        )
    }
}

@Composable
private fun UsageSection(
    isLoading: Boolean,
    error: String?,
    status: WirelessStatusResponse?
) {
    ChatforiaSectionCard(title = stringResource(R.string.android_wireless_home_current_usage)) {
        when {
            isLoading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.android_wireless_home_loading_usage),
                        color = ChatforiaColors.secondaryText
                    )
                }
            }

            error != null -> {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error
                )
            }

            status?.source?.totalDataMb != null &&
                    status.source.remainingDataMb != null &&
                    status.source.totalDataMb > 0 -> {
                val total = status.source.totalDataMb
                val remaining = status.source.remainingDataMb
                val used = (total - remaining).coerceAtLeast(0)
                val progress = used.toFloat() / total.toFloat()

                Text(
                    text = "${formatGb(remaining)} remaining",
                    color = ChatforiaColors.primaryText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Used ${formatGb(used)} of ${formatGb(total)}",
                    color = ChatforiaColors.secondaryText
                )

                status.source.daysRemaining?.let {
                    Text(
                        text = "Expires in $it day${if (it == 1) "" else "s"}",
                        color = ChatforiaColors.secondaryText
                    )
                }

                status.queuedPacks.firstOrNull()?.let { queuedPack ->
                    val queuedRemaining = queuedPack.remainingDataMb

                    if (queuedRemaining != null && queuedRemaining > 0) {
                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "Next Data Pack",
                            color = ChatforiaColors.secondaryText,
                            style = MaterialTheme.typography.labelLarge
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${formatGb(queuedRemaining)} · Queued",
                            color = ChatforiaColors.primaryText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                status.totalAvailableDataMb?.let { totalAvailable ->
                    if (totalAvailable > remaining) {
                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "Total Data Available",
                            color = ChatforiaColors.secondaryText,
                            style = MaterialTheme.typography.labelLarge
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = formatGb(totalAvailable),
                            color = ChatforiaColors.primaryText,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            status?.mode == "NONE" -> {
                Text(
                    text = stringResource(R.string.android_wireless_home_no_active_data_pack_yet_buy_a_pack_below_to_star),
                    color = ChatforiaColors.secondaryText
                )
            }

            else -> {
                Text(
                    text = stringResource(R.string.android_wireless_home_usage_details_will_appear_once_your_data_pack_is),
                    color = ChatforiaColors.secondaryText
                )
            }
        }
    }
}

@Composable
private fun PackListSection(
    packs: List<DataPackOption>,
    isPurchasing: Boolean,
    onChoosePack: (DataPackOption) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        packs.forEach { pack ->
            ChatforiaSectionCard(title = pack.title) {
                Text(
                    text = pack.amountLabel,
                    color = ChatforiaColors.primaryText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = pack.priceLabel,
                    color = ChatforiaColors.primaryText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = pack.description,
                    color = ChatforiaColors.secondaryText
                )

                Spacer(modifier = Modifier.height(12.dp))

                FeatureRow(
                    stringResource(
                        R.string.android_wireless_activates_automatically_on_first_data_connection
                    )
                )

                FeatureRow(
                    stringResource(
                        R.string.android_wireless_valid_for_30_days_from_first_data_connection
                    )
                )

                FeatureRow(
                    stringResource(
                        R.string.android_wireless_one_time_purchase_with_no_contract
                    )
                )

                FeatureRow(
                    stringResource(
                        R.string.android_wireless_top_up_anytime
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                ChatforiaGradientButton(
                    text = if (isPurchasing) "Processing…" else "Choose this pack",
                    onClick = { onChoosePack(pack) },
                    enabled = !isPurchasing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = ChatforiaColors.accent,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = text,
            color = ChatforiaColors.primaryText
        )
    }
}

private fun formatGb(
    mb: Int
): String {
    val gb = mb / 1024.0

    return if (gb >= 10) {
        "%.0f GB".format(gb)
    } else {
        "%.1f GB".format(gb)
    }
}

private fun packsFor(
    scope: EsimScope
): List<DataPackOption> {
    return when (scope) {
        EsimScope.LOCAL -> listOf(
            DataPackOption("chatforia_esim_local_unlimited_premium", scope, "Local Unlimited", "Unlimited", "$59.99", "Best for heavy usage, streaming, and never worrying about data limits.", "US"),
            DataPackOption("chatforia_esim_local_3_premium", scope, "Local 3GB", "3 GB", "$9.99", "Perfect for short trips and light browsing.", "US"),
            DataPackOption("chatforia_esim_local_5_premium", scope, "Local 5GB", "5 GB", "$14.99", "Great for travel, messaging, maps, and everyday use.", "US"),
            DataPackOption("chatforia_esim_local_10_premium", scope, "Local 10GB", "10 GB", "$24.99", "Ideal for heavier travel usage and streaming.", "US"),
            DataPackOption("chatforia_esim_local_20_premium", scope, "Local 20GB", "20 GB", "$39.99", "Maximum data for extended travel and heavy usage.", "US")
        )

        EsimScope.EUROPE -> listOf(
            DataPackOption("chatforia_esim_europe_unlimited_premium", scope, "Europe Unlimited", "Unlimited", "$69.99", "Best for heavy usage, streaming, and never worrying about data limits.", "EU"),
            DataPackOption("chatforia_esim_europe_3_premium", scope, "Europe 3GB", "3 GB", "$12.99", "Light browsing and messaging across Europe.", "EU"),
            DataPackOption("chatforia_esim_europe_5_premium", scope, "Europe 5GB", "5 GB", "$19.99", "Great for maps, messaging, and travel.", "EU"),
            DataPackOption("chatforia_esim_europe_10_premium", scope, "Europe 10GB", "10 GB", "$32.99", "More room for streaming and longer trips.", "EU"),
            DataPackOption("chatforia_esim_europe_20_premium", scope, "Europe 20GB", "20 GB", "$49.99", "Maximum Europe coverage for heavy usage.", "EU")
        )

        EsimScope.GLOBAL -> listOf(
            DataPackOption("chatforia_esim_global_unlimited_premium", scope, "Global Unlimited", "Unlimited", "$79.99", "Best for heavy usage, streaming, and never worrying about data limits.", "GLOBAL"),
            DataPackOption("chatforia_esim_global_3_premium", scope, "Global 3GB", "3 GB", "$21.99", "Light global travel data.", "GLOBAL"),
            DataPackOption("chatforia_esim_global_5_premium", scope, "Global 5GB", "5 GB", "$32.99", "Everyday travel coverage around the world.", "GLOBAL"),
            DataPackOption("chatforia_esim_global_10_premium", scope, "Global 10GB", "10 GB", "$49.99", "More data for extended global trips.", "GLOBAL")
        )
    }
}