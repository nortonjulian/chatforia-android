package com.chatforia.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.chatforia.android.ui.theme.ChatforiaTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItemDefaults
import com.chatforia.android.ui.theme.ChatforiaColors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {
            ChatforiaTheme {
                ChatforiaApp()
            }
        }
    }
}

@Composable
fun ChatforiaApp() {
    var selectedTab by remember { mutableStateOf("Chats") }

    Scaffold(

        bottomBar = {
            NavigationBar(
                containerColor = ChatforiaColors.cardBackground
            ) {

                NavigationBarItem(
                    selected = selectedTab == "Chats",
                    onClick = { selectedTab = "Chats" },

                    label = { Text("Chats") },

                    icon = {
                        Icon(
                            Icons.Default.Chat,
                            contentDescription = "Chats"
                        )
                    },

                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ChatforiaColors.accent,
                        selectedTextColor = ChatforiaColors.accent,
                        indicatorColor = ChatforiaColors.highlightedSurface,

                        unselectedIconColor = ChatforiaColors.secondaryText,
                        unselectedTextColor = ChatforiaColors.secondaryText
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == "Calls",
                    onClick = { selectedTab = "Calls" },

                    label = { Text("Calls") },

                    icon = {
                        Icon(Icons.Default.Call, contentDescription = "Calls")
                    },

                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ChatforiaColors.accent,
                        selectedTextColor = ChatforiaColors.accent,
                        indicatorColor = ChatforiaColors.highlightedSurface,

                        unselectedIconColor = ChatforiaColors.secondaryText,
                        unselectedTextColor = ChatforiaColors.secondaryText
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == "Contacts",
                    onClick = { selectedTab = "Contacts" },

                    label = { Text("Contacts") },

                    icon = {
                        Icon(Icons.Default.Contacts, contentDescription = "Contacts")
                    },

                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ChatforiaColors.accent,
                        selectedTextColor = ChatforiaColors.accent,
                        indicatorColor = ChatforiaColors.highlightedSurface,

                        unselectedIconColor = ChatforiaColors.secondaryText,
                        unselectedTextColor = ChatforiaColors.secondaryText
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == "Profile",
                    onClick = { selectedTab = "Profile" },

                    label = { Text("Profile") },

                    icon = {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    },

                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ChatforiaColors.accent,
                        selectedTextColor = ChatforiaColors.accent,
                        indicatorColor = ChatforiaColors.highlightedSurface,

                        unselectedIconColor = ChatforiaColors.secondaryText,
                        unselectedTextColor = ChatforiaColors.secondaryText
                    )
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                "Chats" -> ChatsScreen()
                "Calls" -> CallsScreen()
                "Contacts" -> ContactsScreen()
                "Profile" -> ProfileScreen()
            }
        }
    }
}