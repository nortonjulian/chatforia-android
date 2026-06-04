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
import com.chatforia.android.auth.AuthState
import com.chatforia.android.auth.LoginScreen
import com.chatforia.android.auth.*
import com.chatforia.android.network.ApiClient
import com.chatforia.android.auth.GoogleAuthClient
import com.chatforia.android.chats.ChatsScreen
import com.chatforia.android.chats.ChatsRepository
import com.chatforia.android.chats.ChatsViewModel
import com.chatforia.android.messages.MessagesRepository
import com.chatforia.android.messages.ChatThreadViewModel
import com.chatforia.android.socket.SocketManager
import com.chatforia.android.crypto.KeyStorage
import androidx.compose.ui.platform.LocalContext
import com.chatforia.android.crypto.AccountKeyManager
import com.chatforia.android.contacts.ContactsRepository
import com.chatforia.android.contacts.ContactsViewModel
import com.chatforia.android.contacts.InviteRepository
import com.chatforia.android.chats.StartChatViewModel

enum class AppTab {
    CHATS,
    CALLS,
    CONTACTS,
    PROFILE
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {

            ChatforiaTheme {

                val tokenStorage =
                    remember {
                        TokenStorage(applicationContext)
                    }

                val apiClient =
                    remember {
                        ApiClient(tokenStorage)
                    }

                val repository =
                    remember {
                        AuthRepository(
                            apiClient,
                            tokenStorage
                        )
                    }

                val keyStorage =
                    remember {
                        KeyStorage(applicationContext)
                    }

                val accountKeyManager =
                    remember {
                        AccountKeyManager(keyStorage)
                    }

                val authViewModel =
                    remember {
                        AuthViewModel(
                            repository = repository,
                            accountKeyManager = accountKeyManager
                        )
                    }

                val authState by
                authViewModel.state.collectAsState()

                when (authState) {

                    AuthState.Loading ->
                        Text("Loading...")

                    AuthState.LoggedOut ->
                        LoginScreen(
                            onLogin = { identifier, password ->
                                authViewModel.login(identifier, password)
                            },
                            onGoogleLogin = {
                                val googleAuthClient =
                                    GoogleAuthClient(applicationContext)

                                val idToken =
                                    googleAuthClient.getIdToken()

                                authViewModel.loginWithGoogle(idToken)
                            },
                            onResetEncryption = { identifier, password ->
                                authViewModel.resetEncryptionAndLogin(identifier, password)
                            }
                        )

                    is AuthState.LoggedIn -> {
                        val loggedInState =
                            authState as AuthState.LoggedIn

                        ChatforiaApp(
                            user = loggedInState.user,
                            apiClient = apiClient,
                            tokenStorage = tokenStorage,
                            authRepository = repository,
                            onLogout = {
                                authViewModel.logout()
                            }
                        )
                    }

                    is AuthState.NeedsOnboarding -> {
                        val onboardingState =
                            authState as AuthState.NeedsOnboarding

                        val settingsRepository =
                            remember {
                                SettingsRepository(apiClient)
                            }

                        OnboardingScreen(
                            user = onboardingState.user,
                            settingsRepository = settingsRepository,
                            onUserUpdated = { updatedUser ->
                                authViewModel.replaceCurrentUser(updatedUser)
                            },
                            onComplete = { completedUser ->
                                authViewModel.markOnboardingComplete(completedUser)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatforiaApp(
    user: UserDto,
    apiClient: ApiClient,
    tokenStorage: TokenStorage,
    authRepository: AuthRepository,
    onLogout: () -> Unit
) {
    var selectedTab by remember {
        mutableStateOf(AppTab.CHATS)
    }

    val chatsRepository =
        remember {
            ChatsRepository(apiClient)
        }

    val chatsViewModel =
        remember {
            ChatsViewModel(chatsRepository)
        }

    val contactsRepository =
        remember {
            ContactsRepository(apiClient)
        }

    val contactsViewModel =
        remember {
            ContactsViewModel(contactsRepository)
        }

    val startChatViewModel =
        remember {
            StartChatViewModel(contactsRepository)
        }

    val inviteRepository =
        remember {
            InviteRepository(apiClient)
        }

    val messagesRepository =
        remember {
            MessagesRepository(apiClient)
        }

    val context = LocalContext.current

    val chatThreadViewModel =
        remember {
            ChatThreadViewModel(
                repository = messagesRepository,
                keyStorage = KeyStorage(context)
            )
        }

    val socketManager =
        remember {
            SocketManager()
        }

    LaunchedEffect(user.id) {
        val token = tokenStorage.read()

        if (!token.isNullOrBlank()) {
            socketManager.connect(token)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            socketManager.disconnect()
        }
    }

    Scaffold(

        bottomBar = {
            NavigationBar(
                containerColor = ChatforiaColors.cardBackground
            ) {

                NavigationBarItem(
                    selected = selectedTab == AppTab.CHATS,
                    onClick = {
                        selectedTab = AppTab.CHATS
                    },

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
                    selected = selectedTab == AppTab.CALLS,
                    onClick = { selectedTab = AppTab.CALLS },

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
                    selected = selectedTab == AppTab.CONTACTS,
                    onClick = { selectedTab = AppTab.CONTACTS },

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
                    selected = selectedTab == AppTab.PROFILE,
                    onClick = { selectedTab = AppTab.PROFILE },

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

                AppTab.CHATS ->
                    ChatsScreen(
                        viewModel = chatsViewModel,
                        threadViewModel = chatThreadViewModel,
                        currentUserId = user.id,
                        currentUsername = user.username,
                        socketManager = socketManager
                    )

                AppTab.CALLS ->
                    CallsScreen()

                AppTab.CONTACTS ->
                    ContactsScreen(
                        viewModel = contactsViewModel,
                        startChatViewModel = startChatViewModel,
                        inviteRepository = inviteRepository,
                        threadViewModel = chatThreadViewModel,
                        currentUserId = user.id,
                        currentUsername = user.username,
                        socketManager = socketManager
                    )
                AppTab.PROFILE ->
                    ProfileScreen(
                        user = user,
                        apiClient = apiClient,
                        authRepository = authRepository,
                        onLogout = onLogout
                    )
            }
        }
    }
}