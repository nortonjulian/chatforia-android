package com.chatforia.android.numbers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chatforia.android.auth.UserDto
import com.chatforia.android.network.ApiClient
import com.chatforia.android.ui.theme.ChatforiaColors
import kotlinx.coroutines.launch
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults

import androidx.compose.material3.FilterChipDefaults
import androidx.compose.ui.graphics.Color

private enum class NumberPickMode {
    FREE,
    PREMIUM
}

@Composable
fun PickNumberSheet(
    apiClient: ApiClient,
    user: UserDto,
    currentNumber: PhoneNumberDto?,
    onDismiss: () -> Unit,
    onUpgradeRequired: () -> Unit
) {
    val repository = remember { PhoneNumberRepository(apiClient) }
    val scope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(NumberPickMode.FREE) }
    var country by remember { mutableStateOf("US") }
    var areaCode by remember { mutableStateOf("") }
    var capability by remember { mutableStateOf("both") }

    var isSearching by remember { mutableStateOf(false) }
    var isLeasing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var numbers by remember { mutableStateOf<List<AvailableNumberDto>>(emptyList()) }

    val isPremium =
        user.plan?.equals("PREMIUM", ignoreCase = true) == true

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ChatforiaColors.screenBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pick a Number",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = ChatforiaColors.primaryText,
                    modifier = Modifier.weight(1f)
                )

                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ModePicker(
                mode = mode,
                onChange = {
                    mode = it
                    numbers = emptyList()
                    error = null
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text =
                    if (mode == NumberPickMode.PREMIUM)
                        "Keep a protected number with Premium."
                    else
                        "Pick a free Chatforia number from available inventory.",
                color = ChatforiaColors.secondaryText,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            CountryDropdown(
                selectedCountry = country,
                onCountryChange = { country = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = areaCode,
                    onValueChange = { areaCode = it.filter(Char::isDigit).take(3) },
                    label = { Text("Area Code") },
                    placeholder = { Text("303") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        scope.launch {
                            isSearching = true
                            error = null

                            try {
                                numbers = repository.searchPool(
                                    areaCode = areaCode,
                                    country = country.ifBlank { "US" },
                                    capability = capability,
                                    premium = mode == NumberPickMode.PREMIUM
                                ).numbers
                            } catch (e: Exception) {
                                error = e.message ?: "Number search failed."
                            } finally {
                                isSearching = false
                            }
                        }
                    },
                    enabled = !isSearching,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Search")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            CapabilityPicker(
                capability = capability,
                onChange = { capability = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = ChatforiaColors.secondaryText,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Premium numbers are protected from recycling.",
                    color = ChatforiaColors.secondaryText,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            HorizontalDivider(color = ChatforiaColors.border)

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Available Numbers",
                color = ChatforiaColors.secondaryText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            when {
                error != null -> {
                    Text(
                        text = error ?: "Something went wrong.",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                isSearching -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                numbers.isEmpty() -> {
                    Text(
                        text = "Search by area code to find available numbers.",
                        color = ChatforiaColors.secondaryText
                    )
                }

                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        numbers.forEach { number ->
                            NumberCard(
                                number = number,
                                mode = mode,
                                isLeasing = isLeasing,
                                currentNumber = currentNumber,
                                onSelect = {
                                    val e164 =
                                        number.e164
                                            ?: number.number
                                            ?: return@NumberCard

                                    if (mode == NumberPickMode.PREMIUM && !isPremium) {
                                        onUpgradeRequired()
                                        return@NumberCard
                                    }

                                    scope.launch {
                                        isLeasing = true
                                        error = null

                                        try {
                                            repository.leaseNumber(
                                                e164 = e164,
                                                premium = mode == NumberPickMode.PREMIUM
                                            )

                                            onDismiss()
                                        } catch (e: Exception) {
                                            val message = e.message ?: "Could not lease number."
                                            error = message

                                            if (message.contains("premium", ignoreCase = true)) {
                                                onUpgradeRequired()
                                            }
                                        } finally {
                                            isLeasing = false
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModePicker(
    mode: NumberPickMode,
    onChange: (NumberPickMode) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ModeButton(
            text = "Free",
            selected = mode == NumberPickMode.FREE,
            onClick = { onChange(NumberPickMode.FREE) },
            modifier = Modifier.weight(1f)
        )

        ModeButton(
            text = "Premium",
            selected = mode == NumberPickMode.PREMIUM,
            onClick = { onChange(NumberPickMode.PREMIUM) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ModeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color =
            if (selected)
                ChatforiaColors.accent
            else
                ChatforiaColors.cardBackground,
        modifier = modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            color =
                if (selected)
                    androidx.compose.ui.graphics.Color.White
                else
                    ChatforiaColors.primaryText,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 12.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun CapabilityPicker(
    capability: String,
    onChange: (String) -> Unit
) {
    Column {
        Text(
            text = "Capability",
            color = ChatforiaColors.primaryText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "sms" to "SMS",
                "voice" to "Voice",
                "both" to "SMS + Voice"
            ).forEach { option ->
                FilterChip(
                    selected = capability == option.first,
                    onClick = { onChange(option.first) },
                    label = { Text(option.second) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ChatforiaColors.accent,
                        selectedLabelColor = Color.White,
                        containerColor = ChatforiaColors.cardBackground,
                        labelColor = ChatforiaColors.primaryText
                    )
                )
            }
        }
    }
}

@Composable
private fun NumberCard(
    number: AvailableNumberDto,
    mode: NumberPickMode,
    isLeasing: Boolean,
    currentNumber: PhoneNumberDto?,
    onSelect: () -> Unit
) {
    val e164 = number.e164 ?: number.number ?: "Unknown"
    val location =
        listOfNotNull(number.locality, number.region)
            .filter { it.isNotBlank() }
            .joinToString(", ")

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ChatforiaColors.cardBackground,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Phone,
                contentDescription = null,
                tint = ChatforiaColors.accent,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = e164,
                    color = ChatforiaColors.primaryText,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge
                )

                if (location.isNotEmpty()) {
                    Text(
                        text = location,
                        color = ChatforiaColors.secondaryText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Button(
                onClick = onSelect,
                enabled = !isLeasing && currentNumber == null,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    if (mode == NumberPickMode.PREMIUM)
                        "Keep"
                    else
                        "Select"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountryDropdown(
    selectedCountry: String,
    onCountryChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val countries = listOf(
        "US" to "United States",
        "CA" to "Canada"
    )

    val selectedName =
        countries.firstOrNull { it.first == selectedCountry }?.second
            ?: "United States"

    Column {
        Text(
            text = "Country",
            color = ChatforiaColors.primaryText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedName,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .menuAnchor()
                    .widthIn(min = 180.dp, max = 260.dp),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ChatforiaColors.border,
                    unfocusedBorderColor = ChatforiaColors.border,

                    focusedContainerColor = ChatforiaColors.cardBackground,
                    unfocusedContainerColor = ChatforiaColors.cardBackground,

                    focusedTextColor = ChatforiaColors.primaryText,
                    unfocusedTextColor = ChatforiaColors.primaryText
                )
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(
                    ChatforiaColors.cardBackground
                )
            ) {
                countries.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                option.second,
                                color = ChatforiaColors.primaryText
                            )
                        },
                        onClick = {
                            onCountryChange(option.first)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}