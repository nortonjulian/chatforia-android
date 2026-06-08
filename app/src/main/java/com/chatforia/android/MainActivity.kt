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
import com.chatforia.android.tenor.TenorRepository
import com.chatforia.android.upload.UploadRepository
import com.chatforia.android.messages.MessageQueueStorage
import com.chatforia.android.calls.IncomingCallSheet
import com.chatforia.android.calls.AndroidCallState
import com.chatforia.android.calls.AndroidCallManager
import com.chatforia.android.calls.CallService
import com.chatforia.android.calls.VideoCallRepository
import com.chatforia.android.calls.CallHistoryRepository
import com.chatforia.android.calls.CallsViewModel
import com.chatforia.android.voicemail.VoicemailRepository
import com.chatforia.android.voicemail.VoicemailViewModel
import com.chatforia.android.crypto.LinkedDevicesRepository
import com.chatforia.android.crypto.LinkedDevicesViewModel
import com.chatforia.android.calls.AudioCallScreen
import com.chatforia.android.calls.VideoCallScreen
import com.chatforia.android.calls.TwilioVoiceManager
import com.chatforia.android.calls.TwilioVideoManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.chatforia.android.calls.CallPermissionHelper
import androidx.compose.material3.MaterialTheme
import com.chatforia.android.random.RandomChatViewModel
import androidx.compose.runtime.SideEffect

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
                                try {
                                    val googleAuthClient =
                                        GoogleAuthClient(applicationContext)

                                    val idToken =
                                        googleAuthClient.getIdToken()

                                    authViewModel.loginWithGoogle(idToken)
                                } catch (e: Exception) {
                                    authViewModel.setError(
                                        "Google sign-in is not available on this device. Try email login instead."
                                    )
                                }
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
                            onUserUpdated = { updatedUser ->
                                authViewModel.replaceCurrentUser(updatedUser)
                            },
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
    onUserUpdated: (UserDto) -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember {
        mutableStateOf(AppTab.CHATS)
    }

    LaunchedEffect(user.theme) {
        ChatforiaColors.applyTheme(user.theme ?: "dawn")
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

    val tenorRepository =
        remember {
            TenorRepository(apiClient)
        }

    val settingsRepository = remember {
        SettingsRepository(apiClient)
    }

    val callService = remember {
        CallService(apiClient)
    }

    val videoCallRepository = remember {
        VideoCallRepository(apiClient)
    }

    val callHistoryRepository = remember {
        CallHistoryRepository(apiClient)
    }

    val voicemailRepository = remember {
        VoicemailRepository(apiClient)
    }

    val linkedDevicesRepository = remember {
        LinkedDevicesRepository(apiClient)
    }

    val context = LocalContext.current

    val uploadRepository =
        remember {
            UploadRepository(
                apiClient = apiClient,
                context = context
            )
        }

    val chatThreadViewModel =
        remember {
            ChatThreadViewModel(
                repository = messagesRepository,
                keyStorage = KeyStorage(context),
                queueStorage = MessageQueueStorage(context)
            )
        }

    val socketManager =
        remember {
            SocketManager()
        }

    val randomChatViewModel =
        remember(user.id) {
            RandomChatViewModel(
                socketManager = socketManager,
                currentUserId = user.id
            )
        }

    val callsViewModel =
        remember {
            CallsViewModel(
                callHistoryRepository = callHistoryRepository,
                callService = callService,
                socketManager = socketManager
            )
        }

    val voicemailViewModel =
        remember {
            VoicemailViewModel(
                repository = voicemailRepository,
                socketManager = socketManager
            )
        }

    val linkedDevicesViewModel =
        remember {
            LinkedDevicesViewModel(
                linkedDevicesRepository
            )
        }

    val androidCallManager =
        remember(context) {
            AndroidCallManager(
                socketManager = socketManager,
                callService = callService,
                videoRepository = videoCallRepository,
                voiceManager = TwilioVoiceManager(context),
                videoManager = TwilioVideoManager(context)
            )
        }

    val callState by
    androidCallManager.state.collectAsState()

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


    when (val state = callState) {

        is AndroidCallState.Failed -> {
            Text(
                text = state.message,
                color = MaterialTheme.colorScheme.error
            )
        }

        is AndroidCallState.Ringing -> {
            IncomingCallSheet(
                payload = state.payload,
                onAccept = {
                    androidCallManager.acceptIncoming(user)
                },
                onDecline = {
                    androidCallManager.declineIncoming()
                }
            )
        }

        is AndroidCallState.Active -> {

            if (state.session.isVideo) {

                VideoCallScreen(
                    session = state.session,

                    onToggleMute = {
                        androidCallManager.toggleMute()
                    },

                    onToggleCamera = {
                        androidCallManager.toggleCamera()
                    },

                    onFlipCamera = {
                        androidCallManager.flipCamera()
                    },

                    onEndCall = {
                        androidCallManager.endCall()
                    }
                )

                return
            }

            AudioCallScreen(
                session = state.session,

                onToggleMute = {
                    androidCallManager.toggleMute()
                },

                onToggleSpeaker = {
                    androidCallManager.toggleSpeaker()
                },

                onEndCall = {
                    androidCallManager.endCall()
                }
            )

            return
        }

        else -> Unit
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
                        randomChatViewModel = randomChatViewModel,
                        currentUserId = user.id,
                        currentUsername = user.username,
                        currentUser = user,
                        androidCallManager = androidCallManager,
                        socketManager = socketManager,
                        tenorRepository = tenorRepository,
                        uploadRepository = uploadRepository,
                        startChatViewModel = startChatViewModel,
                        apiClient = apiClient
                    )

                AppTab.CALLS ->
                    CallsScreen(
                        callsViewModel = callsViewModel,
                        voicemailViewModel = voicemailViewModel,
                        onDialNumber = { number ->
                            androidCallManager.startPhoneCall(number)
                        }
                    )

                AppTab.CONTACTS ->
                    ContactsScreen(
                        viewModel = contactsViewModel,
                        startChatViewModel = startChatViewModel,
                        inviteRepository = inviteRepository,
                        threadViewModel = chatThreadViewModel,
                        currentUserId = user.id,
                        currentUsername = user.username,
                        currentUser = user,
                        androidCallManager = androidCallManager,
                        socketManager = socketManager,
                        tenorRepository = tenorRepository,
                        uploadRepository = uploadRepository
                    )
                AppTab.PROFILE ->
                    ProfileScreen(
                        user = user,
                        apiClient = apiClient,
                        authRepository = authRepository,
                        settingsRepository = settingsRepository,
                        linkedDevicesViewModel = linkedDevicesViewModel,
                        onUserUpdated = onUserUpdated,
                        onLogout = onLogout
                    )
            }
        }
    }
}