package com.chatforia.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatforia.android.ui.theme.ChatforiaColors
import androidx.compose.material.icons.filled.AutoAwesome
import com.chatforia.android.ui.components.ChatforiaSectionCard
import com.chatforia.android.auth.UserDto

@Composable
fun ProfileScreen(
    user: UserDto,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatforiaColors.screenBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "Profile",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = ChatforiaColors.primaryText,
            modifier = Modifier.padding(top = 20.dp, bottom = 18.dp)
        )

        ProfileHeaderCard(user)

        Spacer(modifier = Modifier.height(16.dp))

        UpgradeCard()

        Spacer(modifier = Modifier.height(16.dp))

        ChatforiaSectionCard(title = "Account") {
            ProfileRow(
                icon = Icons.Default.Person,
                title = "Username",
                subtitle = user.username ?: "Unknown"
            )

            HorizontalDivider(color = ChatforiaColors.border)

            ProfileRow(
                icon = Icons.Default.Email,
                title = "Email",
                subtitle = user.email ?: "No email"
            )

            HorizontalDivider(color = ChatforiaColors.border)

            ProfileRow(
                icon = Icons.Default.Star,
                title = "Plan",
                subtitle = user.plan ?: "Free"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        ChatforiaSectionCard(title = "Plan") {
            ProfileRow(
                icon = Icons.Default.CreditCard,
                title = "Plan & Billing",
                subtitle = "Current plan: Free",
                showChevron = true
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        ChatforiaSectionCard(title = "Wireless") {
            ProfileRow(
                icon = Icons.Default.Wifi,
                title = "Chatforia Mobile",
                subtitle = "eSIM and wireless settings",
                showChevron = true
            )

            HorizontalDivider(color = ChatforiaColors.border)

            ProfileRow(
                icon = Icons.Default.Phone,
                title = "Phone Number",
                subtitle = "Manage your Chatforia number",
                showChevron = true
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        ChatforiaSectionCard(title = "Settings") {
            ProfileRow(
                icon = Icons.Default.Language,
                title = "Language",
                subtitle = user.preferredLanguage ?: "English",
                showChevron = true
            )

            HorizontalDivider(color = ChatforiaColors.border)

            ProfileRow(
                icon = Icons.Default.Settings,
                title = "Appearance",
                subtitle = user.theme ?: "Dawn",
                showChevron = true
            )

            HorizontalDivider(color = ChatforiaColors.border)

            ProfileRow(
                icon = Icons.Default.Security,
                title = "Security",
                subtitle = "Encryption key protected",
                showChevron = true
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = ChatforiaColors.accent
            )
        ) {
            Icon(
                Icons.Default.Logout,
                contentDescription = "Log out"
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text("Log out")
        }
    }
}

@Composable
private fun ProfileHeaderCard(
    user: UserDto
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = ChatforiaColors.cardBackground,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(76.dp),
                shape = CircleShape,
                color = ChatforiaColors.highlightedSurface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text =
                            user.username
                                ?.firstOrNull()
                                ?.uppercase()
                                ?: "?",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChatforiaColors.primaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = user.username ?: "Unknown",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ChatforiaColors.primaryText
            )

            Text(
                text = user.email ?: "No email",
                style = MaterialTheme.typography.bodyMedium,
                color = ChatforiaColors.secondaryText
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text =
                    user.plan
                        ?.uppercase()
                        ?: "FREE",
                style = MaterialTheme.typography.labelMedium,
                color = ChatforiaColors.accent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun UpgradeCard() {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ChatforiaColors.cardBackground,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = "Upgrade",
                tint = ChatforiaColors.accent
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Upgrade",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = ChatforiaColors.primaryText
                )

                Text(
                    text = "Choose Plus or Premium",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChatforiaColors.secondaryText
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Open upgrade",
                tint = ChatforiaColors.secondaryText
            )
        }
    }
}

@Composable
private fun ProfileRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    showChevron: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = ChatforiaColors.accent,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = ChatforiaColors.primaryText
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = ChatforiaColors.secondaryText
            )
        }

        if (showChevron) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = ChatforiaColors.secondaryText
            )
        }
    }
}