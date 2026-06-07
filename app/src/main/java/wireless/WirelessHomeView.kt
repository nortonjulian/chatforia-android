package com.chatforia.android.wireless

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chatforia.android.network.ApiClient
import com.chatforia.android.ui.components.ChatforiaSectionCard
import com.chatforia.android.ui.theme.ChatforiaColors
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color

@Composable
fun WirelessHomeView(
    apiClient: ApiClient,
    onBack: () -> Unit
) {
    val repository = remember { WirelessRepository(apiClient) }
    val scope = rememberCoroutineScope()

    var selectedScope by remember { mutableStateOf(EsimScope.LOCAL) }
    var status by remember { mutableStateOf<WirelessStatusResponse?>(null) }
    var esim by remember { mutableStateOf<EsimSubscriberDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isPurchasing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var activation by remember { mutableStateOf<ReserveEsimResponse?>(null) }

    fun reload() {
        scope.launch {
            isLoading = true
            error = null

            try {
                status = repository.getWirelessStatus()
                esim = repository.getCurrentEsim().subscriber
            } catch (e: Exception) {
                error = e.message ?: "Could not load wireless details."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        reload()
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
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = ChatforiaColors.primaryText
                )
            }

            Text(
                text = "Wireless",
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
            activation = activation
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
                scope.launch {
                    isPurchasing = true
                    error = null

                    try {
                        activation = repository.reserveEsim(pack.region)
                        reload()
                    } catch (e: Exception) {
                        error = e.message ?: "Could not start eSIM activation."
                    } finally {
                        isPurchasing = false
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        ChatforiaSectionCard(title = "Manage") {
            OutlinedButton(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Manage Wireless")
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Port My Number")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "eSIM data packs require an eSIM-compatible and unlocked device.",
            color = ChatforiaColors.secondaryText,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun HeroSection() {
    ChatforiaSectionCard(title = "Chatforia Mobile") {
        Text(
            text = "Stay connected when you’re traveling or away from Wi-Fi.",
            color = ChatforiaColors.primaryText,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Choose a one-time eSIM data pack for Local, Europe, or Global coverage.",
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
    ChatforiaSectionCard(title = "Coverage") {
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
            text = "We don’t sell data packs under 3 GB.",
            color = ChatforiaColors.secondaryText,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun EsimSection(
    esim: EsimSubscriberDto?,
    activation: ReserveEsimResponse?
) {
    ChatforiaSectionCard(title = "Your eSIM") {
        val hasEsim = esim != null || activation != null

        Text(
            text =
                if (hasEsim)
                    "Your eSIM is ready to install."
                else
                    "You don’t have an eSIM yet.",
            color = ChatforiaColors.primaryText,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        if (hasEsim) {
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text =
                    activation?.lpaUri
                        ?: activation?.qrPayload
                        ?: esim?.lpaUri
                        ?: esim?.qrPayload
                        ?: "Activation details will appear here.",
                color = ChatforiaColors.secondaryText,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Icon(Icons.Default.SimCard, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (hasEsim) "Activate" else "Set up")
        }
    }
}

@Composable
private fun UsageSection(
    isLoading: Boolean,
    error: String?,
    status: WirelessStatusResponse?
) {
    ChatforiaSectionCard(title = "Current Usage") {
        when {
            isLoading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Loading usage…",
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
            }

            status?.mode == "NONE" -> {
                Text(
                    text = "No active data pack yet. Buy a pack below to start tracking usage.",
                    color = ChatforiaColors.secondaryText
                )
            }

            else -> {
                Text(
                    text = "Usage details will appear once your data pack is active.",
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

                FeatureRow("Instant eSIM activation")
                FeatureRow("One-time pack. No contract.")
                FeatureRow("Top up anytime")

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { onChoosePack(pack) },
                    enabled = !isPurchasing,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(if (isPurchasing) "Processing…" else "Choose this pack")
                }
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
            DataPackOption("esim_local_unlimited", scope, "Local Unlimited", "Unlimited", "$59.99", "Best for heavy usage, streaming, and never worrying about data limits.", "US"),
            DataPackOption("esim_local_3", scope, "Local 3GB", "3 GB", "$14.99", "Perfect for short trips and light browsing.", "US"),
            DataPackOption("esim_local_5", scope, "Local 5GB", "5 GB", "$22.99", "Great for travel, messaging, maps, and everyday use.", "US"),
            DataPackOption("esim_local_10", scope, "Local 10GB", "10 GB", "$34.99", "Ideal for heavier travel usage and streaming.", "US"),
            DataPackOption("esim_local_20", scope, "Local 20GB", "20 GB", "$54.99", "Maximum data for extended travel and heavy usage.", "US")
        )

        EsimScope.EUROPE -> listOf(
            DataPackOption("esim_local_unlimited", scope, "Local Unlimited", "Unlimited", "$69.99", "Best for heavy usage, streaming, and never worrying about data limits.", "US"),
            DataPackOption("esim_europe_3", scope, "Europe 3GB", "3 GB", "$16.99", "Light browsing and messaging across Europe.", "EU"),
            DataPackOption("esim_europe_5", scope, "Europe 5GB", "5 GB", "$24.99", "Great for maps, messaging, and travel.", "EU"),
            DataPackOption("esim_europe_10", scope, "Europe 10GB", "10 GB", "$36.99", "More room for streaming and longer trips.", "EU"),
            DataPackOption("esim_europe_20", scope, "Europe 20GB", "20 GB", "$64.99", "Maximum Europe coverage for heavy usage.", "EU")
        )

        EsimScope.GLOBAL -> listOf(
            DataPackOption("esim_local_unlimited", scope, "Local Unlimited", "Unlimited", "$79.99", "Best for heavy usage, streaming, and never worrying about data limits.", "US"),
            DataPackOption("esim_global_3", scope, "Global 3GB", "3 GB", "$21.99", "Light global travel data.", "GLOBAL"),
            DataPackOption("esim_global_5", scope, "Global 5GB", "5 GB", "$32.99", "Everyday travel coverage around the world.", "GLOBAL"),
            DataPackOption("esim_global_10", scope, "Global 10GB", "10 GB", "$49.99", "More data for extended global trips.", "GLOBAL")
        )
    }
}