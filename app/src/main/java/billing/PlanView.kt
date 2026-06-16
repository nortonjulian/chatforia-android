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
import androidx.compose.ui.res.stringResource
import com.chatforia.android.R
import androidx.compose.runtime.LaunchedEffect
import analytics.AnalyticsManager
import analytics.AnalyticsTracker
@Composable
fun PlanView(
    user: UserDto,
    onBack: () -> Unit,
    onUpgrade: () -> Unit,
    analytics: AnalyticsTracker = AnalyticsManager
) {
    val currentPlan = user.plan?.lowercase() ?: "free"

    LaunchedEffect(currentPlan) {
        analytics.capture(
            "plan screen viewed",
            mapOf(
                "current_plan" to currentPlan
            )
        )
    }

    val displayPlan = when (currentPlan) {
        "plus" -> stringResource(R.string.android_plan_plus)
        "premium" -> stringResource(R.string.android_plan_premium)
        else -> stringResource(R.string.android_profile_free)
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
                    contentDescription = stringResource(R.string.android_plan_back),
                    tint = ChatforiaColors.primaryText
                )
            }

            Text(
                text = stringResource(R.string.android_plan_plan_billing),
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
            ChatforiaSectionCard(title = stringResource(R.string.android_plan_my_plan)) {
                Text(
                    text = stringResource(R.string.android_plan_current_plan),
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
                        "plus" -> stringResource(R.string.billing_description_plus)
                        "premium" -> stringResource(R.string.billing_description_premium)
                        else -> stringResource(R.string.billing_description_free)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChatforiaColors.secondaryText
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            ChatforiaSectionCard(title = stringResource(R.string.android_plan_compare_plans)) {
                Text(
                    text = stringResource(R.string.android_plan_go_ad_free_with_plus_or_unlock_ai_tools_and_cust),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChatforiaColors.secondaryText
                )

                Spacer(modifier = Modifier.height(18.dp))

                PlanCompareHeader()

                PlanCompareRow(stringResource(R.string.billing_feature_adFree), plus = true, premium = true)
                PlanCompareRow(stringResource(R.string.billing_feature_longerHistory), plus = true, premium = true)
                PlanCompareRow(stringResource(R.string.billing_feature_forwarding), plus = true, premium = true)
                PlanCompareRow(stringResource(R.string.billing_feature_aiTools), plus = false, premium = true)
                PlanCompareRow(stringResource(R.string.billing_feature_premiumThemes), plus = false, premium = true)
                PlanCompareRow(stringResource(R.string.billing_feature_prioritySupport), plus = false, premium = true)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        analytics.capture(
                            "upgrade entry tapped",
                            mapOf(
                                "source" to "plan_screen",
                                "current_plan" to currentPlan
                            )
                        )

                        onUpgrade()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (currentPlan == "premium") {
                            stringResource(R.string.common_manageSubscription)
                        } else {
                            stringResource(R.string.android_upgrade_upgrade)
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            ChatforiaSectionCard(title = stringResource(R.string.android_plan_included)) {
                IncludedRow(stringResource(R.string.billing_included_messaging))
                IncludedRow(stringResource(R.string.billing_included_translation))
                IncludedRow(stringResource(R.string.billing_included_mediaSharing))

                if (currentPlan == "plus" || currentPlan == "premium") {
                    IncludedRow(stringResource(R.string.billing_included_expandedAccess))
                    IncludedRow(stringResource(R.string.billing_included_enhancedFeatures))
                }

                if (currentPlan == "premium") {
                    IncludedRow(stringResource(R.string.billing_included_premiumThemes))
                    IncludedRow(stringResource(R.string.billing_included_premiumSounds))
                    IncludedRow(stringResource(R.string.billing_included_aiTools))
                    IncludedRow(stringResource(R.string.billing_included_prioritySupport))
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
            text = stringResource(R.string.android_plan_plus),
            style = MaterialTheme.typography.labelLarge,
            color = ChatforiaColors.secondaryText,
            modifier = Modifier.width(70.dp)
        )

        Text(
            text = stringResource(R.string.android_plan_premium),
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