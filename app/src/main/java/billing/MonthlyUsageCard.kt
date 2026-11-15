package com.chatforia.android.billing

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chatforia.android.R
import com.chatforia.android.network.ApiClient
import com.chatforia.android.ui.components.ChatforiaSectionCard
import kotlinx.coroutines.CancellationException
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.floor

private val usageMeters = listOf(
    Triple("riaActions", R.string.usage_ai, 1),
    Triple("translationChars", R.string.usage_translation, 1),
    Triple("hostedParticipantSeconds", R.string.usage_app_calls, 60),
    Triple("smsMessages", R.string.usage_sms, 1),
    Triple("pstnSeconds", R.string.usage_phone, 60),
    Triple("forwardingSeconds", R.string.usage_forwarding, 60),
    Triple("voiceNoteTranscriptionSeconds", R.string.usage_voice_notes, 60),
    Triple("voicemailTranscriptionSeconds", R.string.usage_voicemail, 60)
)

@Composable
fun MonthlyUsageCard(apiClient: ApiClient) {
    var revision by remember { mutableIntStateOf(0) }
    var snapshot by remember { mutableStateOf<PlanEntitlementsResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(apiClient, revision) {
        loading = true; failed = false; snapshot = null
        try {
            val response = PlanEntitlementsRepository(apiClient).fetch(usageOnly = true)
            Instant.parse(requireNotNull(response.resetAt))
            require(usageMeters.all { (key, _, _) ->
                response.usage[key]?.let { it.used >= 0 && it.limit >= 0 && it.remaining >= 0 } == true
            })
            snapshot = response
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            failed = true
        } finally { loading = false }
    }
    val formatter = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }
    ChatforiaSectionCard(title = stringResource(R.string.usage_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (loading) CircularProgressIndicator()
            if (failed) Text(stringResource(R.string.usage_error), color = MaterialTheme.colorScheme.error)
            snapshot?.let { data ->
                usageMeters.forEach { (key, label, divisor) ->
                    data.usage[key]?.let { meter ->
                        val remaining = minOf(meter.remaining, meter.limit - meter.used).coerceAtLeast(0)
                        Text(stringResource(label), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.usage_balance,
                            formatter.format(floor(remaining.toDouble() / divisor * 10) / 10),
                            formatter.format(meter.limit.toDouble() / divisor)))
                    }
                }
                val date = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                    .withLocale(Locale.getDefault()).withZone(ZoneOffset.UTC)
                    .format(Instant.parse(data.resetAt))
                Text(stringResource(R.string.usage_reset, date))
            }
            Text(stringResource(R.string.usage_explanation))
            Text(stringResource(R.string.usage_call_explanation))
            Text(stringResource(R.string.usage_exhaustion))
            OutlinedButton(onClick = { revision++ }, enabled = !loading) {
                Text(stringResource(R.string.usage_refresh))
            }
        }
    }
}
