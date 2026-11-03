package com.chatforia.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chatforia.android.ui.components.ChatforiaSectionCard
import com.chatforia.android.ui.theme.ChatforiaColors
import com.chatforia.android.ui.theme.ChatforiaTheme
import com.chatforia.android.ui.theme.ThemePreferenceStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI

class LicenseReaderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ChatforiaColors.applyTheme(ThemePreferenceStorage(this).readTheme())
        val dark = ChatforiaColors.screenBackground.luminance() <= 0.5f
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark },
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark }
        )
        setContent { ChatforiaTheme { LicenseReader(onClose = { finish() }) } }
    }
}

internal data class LicenseNotice(val name: String, val text: String)

// Metadata offsets count UTF-8 bytes, not characters. Reject corrupt ranges rather
// than displaying the wrong attribution under a library's name.
internal fun parseLicenseNotices(metadata: String, bytes: ByteArray): List<LicenseNotice> =
    metadata.lineSequence().filter { it.isNotBlank() }.map { line ->
        val match = requireNotNull(Regex("^(\\d+):(\\d+) (.+)$").matchEntire(line))
        val offset = match.groupValues[1].toLong()
        val length = match.groupValues[2].toLong()
        require(offset <= bytes.size && length <= bytes.size.toLong() - offset)
        LicenseNotice(match.groupValues[3], String(bytes, offset.toInt(), length.toInt(), Charsets.UTF_8))
    }.toList()

@Composable
private fun LicenseReader(onClose: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var notices by remember { mutableStateOf<List<LicenseNotice>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        try {
            notices = withContext(Dispatchers.IO) {
                val resources = context.resources
                fun read(name: String): ByteArray {
                    val id = resources.getIdentifier(name, "raw", context.packageName)
                    require(id != 0)
                    return resources.openRawResource(id).use { it.readBytes() }
                }
                parseLicenseNotices(read("third_party_license_metadata").toString(Charsets.UTF_8), read("third_party_licenses"))
            }
        } catch (_: java.io.IOException) { failed = true }
        catch (_: IllegalArgumentException) { failed = true }
        catch (_: android.content.res.Resources.NotFoundException) { failed = true }
    }
    val notice = selected?.let { notices?.getOrNull(it) }
    BackHandler(enabled = selected != null) { selected = null }
    Column(Modifier.fillMaxSize().background(ChatforiaColors.screenBackground).safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (selected != null) selected = null else onClose() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.license_back), tint = ChatforiaColors.primaryText)
            }
            Text(stringResource(R.string.open_source_licenses), style = MaterialTheme.typography.titleLarge, color = ChatforiaColors.primaryText)
        }
        Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            when {
                failed -> Text(stringResource(R.string.license_unavailable), Modifier.padding(20.dp), color = ChatforiaColors.primaryText)
                notices == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                notice != null -> key(selected) {
                    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        item { Text(notice.name, style = MaterialTheme.typography.headlineSmall, color = ChatforiaColors.primaryText) }
                        item {
                            ChatforiaSectionCard {
                                SelectionContainer {
                                    Text(notice.text, Modifier.padding(vertical = 16.dp), style = MaterialTheme.typography.bodyMedium, color = ChatforiaColors.primaryText)
                                }
                            }
                        }
                        val url = notice.text.trim()
                        val isUrl = runCatching { URI(url).let { it.scheme in listOf("https", "http") && !it.host.isNullOrBlank() } }.getOrDefault(false)
                        if (isUrl) item {
                            Text(stringResource(R.string.license_link_explanation), color = ChatforiaColors.secondaryText)
                            var linkFailed by remember { mutableStateOf(false) }
                            TextButton(onClick = { linkFailed = runCatching { uriHandler.openUri(url) }.isFailure }) {
                                Text(stringResource(R.string.license_open_link))
                            }
                            if (linkFailed) Text(stringResource(R.string.license_link_failed), color = ChatforiaColors.primaryText)
                        }
                    }
                }
                notices!!.isEmpty() -> Text(stringResource(R.string.license_unavailable), Modifier.padding(20.dp), color = ChatforiaColors.primaryText)
                else -> LazyColumn(state = listState, contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { Text(stringResource(R.string.license_intro), color = ChatforiaColors.secondaryText, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp)) }
                    itemsIndexed(notices!!) { index, entry ->
                        ChatforiaSectionCard {
                            Row(Modifier.fillMaxWidth().clickable { selected = index }.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(entry.name, Modifier.weight(1f), color = ChatforiaColors.primaryText, style = MaterialTheme.typography.bodyLarge)
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = ChatforiaColors.secondaryText)
                            }
                        }
                    }
                }
            }
        }
    }
}
