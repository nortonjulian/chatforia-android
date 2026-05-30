package com.chatforia.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.chatforia.android.ui.theme.ChatforiaTheme
import androidx.compose.foundation.layout.padding

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            ChatforiaTheme {
                ChatforiaApp()
            }
        }
    }
}

@Composable
fun ChatforiaApp() {

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->

        Text(
            text = "Welcome to Chatforia Android 🚀",
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewChatforiaApp() {
    ChatforiaTheme {
        ChatforiaApp()
    }
}