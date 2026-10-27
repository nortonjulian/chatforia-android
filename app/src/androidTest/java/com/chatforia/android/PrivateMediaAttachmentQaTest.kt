package com.chatforia.android

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import com.chatforia.android.messages.AttachmentDto
import com.chatforia.android.messages.MessageAttachmentsView
import com.chatforia.android.messages.PrivateMediaUrlRepository
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.ApiTransport
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL

/**
 * Opt-in local media QA only; no Chatforia login, production API or providers.
 *
 * Run using the separate PrivateMediaQaRunner, against the explicitly selected disposable Android emulator only.
 * The host runner injects a disposable QA JWT as an instrumentation argument.
 * Never log credentials or signed URLs.
 */
class PrivateMediaAttachmentQaTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun realPrivateMediaGrantAndNativeAttachmentComponents() {
        val args = InstrumentationRegistry.getArguments()
        val token = args.getString("privateMediaQaToken")
            ?: error("Missing disposable private-media QA token")
        val imageId = args.getString("privateMediaQaImageId")?.toIntOrNull()
            ?: error("Missing private-media QA image id")
        val audioId = args.getString("privateMediaQaAudioId")?.toIntOrNull()
            ?: error("Missing private-media QA audio id")
        val base = "http://10.0.2.2:5502"

        val transport = object : ApiTransport {
            override fun sendRaw(request: ApiRequest): String {
                check(request.requiresAuth)
                check(request.path.startsWith("private-media/message/"))
                val connection = URL("$base/${request.path}").openConnection() as HttpURLConnection
                try {
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("Authorization", "Bearer $token")
                    connection.connectTimeout = 4000
                    connection.readTimeout = 5000
                    check(connection.responseCode == 200) {
                        "Local QA media grant unavailable"
                    }
                    return connection.inputStream.bufferedReader().use { it.readText() }
                } finally {
                    connection.disconnect()
                }
            }
        }

        // Exercise exactly the same repository used by production message rows,
        // but with a scoped QA base URL and an isolated in-process auth transport.
        val secure = PrivateMediaUrlRepository(
            api = transport,
            apiBaseUrl = base
        )
        val authorizedImage = runBlocking {
            secure.authorize(imageId, "media/qa-image.png")
        }
        val authorizedAudio = runBlocking {
            secure.authorize(audioId, "media/qa-tone.wav")
        }
        assertTrue(authorizedImage?.startsWith("$base/private-media/file?token=") == true)
        assertTrue(authorizedAudio?.startsWith("$base/private-media/file?token=") == true)

        compose.setContent {
            MaterialTheme {
                MessageAttachmentsView(
                    attachments = listOf(
                        AttachmentDto(
                            id = imageId,
                            kind = "IMAGE",
                            url = "media/qa-image.png",
                            thumbUrl = "thumbs/qa-thumb.png",
                            mimeType = "image/png"
                        ),
                        AttachmentDto(
                            id = audioId,
                            kind = "AUDIO",
                            url = "media/qa-tone.wav",
                            mimeType = "audio/wav",
                            caption = "QA voice note"
                        )
                    ),
                    isMine = false,
                    secureMedia = secure,
                    viewerId = 1
                )
            }
        }

        // The real attachment view resolves full/thumb authorization using
        // produceState on Dispatchers.IO. Compose rendering can finish before
        // the audio media grant completes, so wait for both native components.
        compose.waitUntil(timeoutMillis = 15_000L) {
            compose.onAllNodesWithContentDescription("Image attachment")
                .fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodesWithText("QA voice note")
                    .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Image attachment").assertExists()
        compose.onNodeWithText("QA voice note").assertExists()
        compose.onNodeWithContentDescription("Play voice note").assertExists()
        // This verifies authorized URLs and native Compose UI rendering;
        // native audio playback and video decoding still require separate QA.
    }
}
