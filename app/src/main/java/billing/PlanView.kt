package com.chatforia.android.billing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chatforia.android.auth.UserDto
import com.chatforia.android.ui.components.ChatforiaSectionCard
import com.chatforia.android.ui.theme.ChatforiaColors

@Composable
fun PlanView(
    user: UserDto,
    onBack: () -> Unit,
    onUpgrade: () -> Unit
) {
    val currentPlan = user.plan?.lowercase() ?: "free"
    val displayPlan = when (currentPlan) {
        "plus" -> "Plus"
        "premium" -> "Premium"
        else -> "Free"
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
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Back",
                    tint = ChatforiaColors.primaryText
                )
            }

            Text(
                text = "Plan & Billing",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = ChatforiaColors.primaryText,
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 28.dp)
        ) {
            ChatforiaSectionCard(title = "My Plan") {
                Text(
                    text = "Current plan",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ChatforiaColors.secondaryText
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = displayPlan,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = ChatforiaColors.primaryText
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = when (currentPlan) {
                        "plus" -> "Ad-free access with forwarding and expanded features."
                        "premium" -> "Full access to Chatforia customization, AI tools, and advanced features."
                        else -> "Basic access to Chatforia with core messaging features."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChatforiaColors.secondaryText
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            ChatforiaSectionCard(title = "Compare Plans") {
                Text(
                    text = "Go ad-free with Plus, or unlock AI tools and customization with Premium.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChatforiaColors.secondaryText
                )

                Spacer(modifier = Modifier.height(18.dp))

                PlanCompareHeader()

                PlanCompareRow("Ad-free experience", plus = true, premium = true)
                PlanCompareRow("Longer message history", plus = true, premium = true)
                PlanCompareRow("Message forwarding", plus = true, premium = true)
                PlanCompareRow("AI tools", plus = false, premium = true)
                PlanCompareRow("Premium themes & sounds", plus = false, premium = true)
                PlanCompareRow("Priority support", plus = false, premium = true)

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onUpgrade,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (currentPlan == "premium") {
                            "Manage Subscription"
                        } else {
                            "Upgrade"
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            ChatforiaSectionCard(title = "Included") {
                IncludedRow("Messaging")
                IncludedRow("Translation")
                IncludedRow("Media sharing")

                if (currentPlan == "plus" || currentPlan == "premium") {
                    IncludedRow("Expanded access")
                    IncludedRow("Enhanced features")
                }

                if (currentPlan == "premium") {
                    IncludedRow("Premium themes")
                    IncludedRow("Premium sounds")
                    IncludedRow("AI tools")
                    IncludedRow("Priority support")
                }
            }
        }
    }
}

@Composable
private fun PlanCompareHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Plus",
            style = MaterialTheme.typography.labelLarge,
            color = ChatforiaColors.secondaryText,
            modifier = Modifier.width(70.dp)
        )

        Text(
            text = "Premium",
            style = MaterialTheme.typography.labelLarge,
            color = ChatforiaColors.secondaryText,
            modifier = Modifier.width(90.dp)
        )
    }
}

@Composable
private fun PlanCompareRow(
    title: String,
    plus: Boolean,
    premium: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = ChatforiaColors.primaryText,
            modifier = Modifier.weight(1f)
        )

        PlanCheck(checked = plus, modifier = Modifier.width(70.dp))
        PlanCheck(checked = premium, modifier = Modifier.width(90.dp))
    }
}

@Composable
private fun PlanCheck(
    checked: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector =
                if (checked) Icons.Default.CheckCircle else Icons.Default.Close,
            contentDescription = null,
            tint =
                if (checked)
                    ChatforiaColors.accent
                else
                    ChatforiaColors.secondaryText,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun IncludedRow(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = ChatforiaColors.accent,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = ChatforiaColors.primaryText
        )
    }
}