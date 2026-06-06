package com.chatforia.android.billing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chatforia.android.network.ApiClient
import com.chatforia.android.ui.theme.ChatforiaColors
import kotlinx.coroutines.launch

@Composable
fun UpgradeView(
    apiClient: ApiClient,
    onClose: () -> Unit,
    onUpgradeTapped: (PricingProduct) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val pricingService = remember { PricingQuoteService(apiClient) }

    var quotes by remember {
        mutableStateOf<Map<PricingProduct, PricingQuote>>(emptyMap())
    }

    LaunchedEffect(Unit) {
        quotes = pricingService.getQuotes(
            listOf(
                PricingProduct.Plus,
                PricingProduct.PremiumMonthly,
                PricingProduct.PremiumAnnual
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatforiaColors.screenBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Upgrade",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = ChatforiaColors.primaryText,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = ChatforiaColors.primaryText
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HeroSection()

            Spacer(modifier = Modifier.height(22.dp))

            UpgradePlanCard(
                badge = "PLUS",
                title = "Chatforia Plus",
                subtitle = "Remove ads and unlock more everyday features.",
                price = pricingService.formattedPrice(
                    quote = quotes[PricingProduct.Plus],
                    fallbackProduct = PricingProduct.Plus
                ) ?: "$6.99",
                period = "/ month",
                features = listOf(
                    "Ad-free experience",
                    "Message forwarding",
                    "Longer history",
                    "Faster support"
                ),
                highlighted = false,
                onClick = {
                    onUpgradeTapped(PricingProduct.Plus)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            UpgradePlanCard(
                badge = "PREMIUM",
                title = "Chatforia Premium",
                subtitle = "The full Chatforia experience.",
                price = pricingService.formattedPrice(
                    quote = quotes[PricingProduct.PremiumMonthly],
                    fallbackProduct = PricingProduct.PremiumMonthly
                ) ?: "$12.99",
                period = "/ month",
                features = listOf(
                    "Everything in Plus",
                    "AI tools with Ria",
                    "Premium themes",
                    "Premium message tones",
                    "Premium ringtones",
                    "Priority features"
                ),
                highlighted = true,
                onClick = {
                    onUpgradeTapped(PricingProduct.PremiumMonthly)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            UpgradePlanCard(
                badge = "BEST VALUE",
                title = "Premium Annual",
                subtitle = "Save compared to monthly Premium.",
                price = pricingService.formattedPrice(
                    quote = quotes[PricingProduct.PremiumAnnual],
                    fallbackProduct = PricingProduct.PremiumAnnual
                ) ?: "$99.00",
                period = "/ year",
                features = listOf(
                    "Full Premium access",
                    "AI tools",
                    "Premium customization",
                    "Best yearly value"
                ),
                highlighted = false,
                onClick = {
                    onUpgradeTapped(PricingProduct.PremiumAnnual)
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            TextButton(
                onClick = {
                    scope.launch {
                        println("Restore purchases tapped")
                    }
                }
            ) {
                Text(
                    text = "Restore purchases",
                    color = ChatforiaColors.accent
                )
            }
        }
    }
}

@Composable
private fun HeroSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(86.dp),
            shape = CircleShape,
            color = ChatforiaColors.highlightedSurface
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = ChatforiaColors.accent,
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Unlock more with Chatforia",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = ChatforiaColors.primaryText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Choose Plus or Premium to remove ads, customize your experience, and unlock advanced tools.",
            style = MaterialTheme.typography.bodyMedium,
            color = ChatforiaColors.secondaryText,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun UpgradePlanCard(
    badge: String,
    title: String,
    subtitle: String,
    price: String,
    period: String,
    features: List<String>,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color =
            if (highlighted)
                ChatforiaColors.highlightedSurface
            else
                ChatforiaColors.cardBackground,
        tonalElevation = if (highlighted) 6.dp else 2.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = badge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector =
                                if (highlighted)
                                    Icons.Default.AutoAwesome
                                else
                                    Icons.Default.Star,
                            contentDescription = null
                        )
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                if (highlighted) {
                    Text(
                        text = "Best experience",
                        color = ChatforiaColors.accent,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ChatforiaColors.primaryText
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = ChatforiaColors.secondaryText
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = price,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = ChatforiaColors.primaryText
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = period,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChatforiaColors.secondaryText,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            features.forEach { feature ->
                FeatureRow(feature)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Upgrade",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = ChatforiaColors.accent,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = ChatforiaColors.primaryText
        )
    }
}