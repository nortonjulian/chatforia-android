package com.chatforia.android.auth

import com.chatforia.android.crypto.AccountKeyService
import com.chatforia.android.notifications.PushTokenRegisterer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun bootstrap_withoutUserSetsLoggedOut() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeAuthSessionRepository()
            repository.bootstrapUser = null

            val viewModel = createViewModel(repository)

            viewModel.bootstrap()

            advanceUntilIdle()

            assertEquals(AuthState.LoggedOut, viewModel.state.value)
        }

    @Test
    fun bootstrap_withNormalUserSetsLoggedInAndRegistersPushToken() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeAuthSessionRepository()
            val pushRegisterer = FakePushTokenRegisterer()

            val user = user(
                id = 1,
                username = "julian",
                preferredLanguage = "en"
            )

            repository.bootstrapUser = user
            repository.fetchMeUser = user

            val viewModel =
                createViewModel(
                    repository = repository,
                    pushRegisterer = pushRegisterer
                )

            viewModel.bootstrap()

            advanceUntilIdle()

            val state = viewModel.state.value

            assertTrue(state is AuthState.LoggedIn)
            assertEquals(1, (state as AuthState.LoggedIn).user.id)
            assertEquals(1, pushRegisterer.registerCallCount)
        }

    @Test
    fun bootstrap_withMissingLanguageSetsNeedsOnboarding() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeAuthSessionRepository()

            val user = user(
                id = 2,
                username = "julian",
                preferredLanguage = null
            )

            repository.bootstrapUser = user
            repository.fetchMeUser = user

            val viewModel = createViewModel(repository)

            viewModel.bootstrap()

            advanceUntilIdle()

            val state = viewModel.state.value

            assertTrue(state is AuthState.NeedsOnboarding)
            assertEquals(2, (state as AuthState.NeedsOnboarding).user.id)
        }

    @Test
    fun bootstrap_withTemporaryUsernameSetsNeedsOnboarding() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeAuthSessionRepository()

            val user = user(
                id = 3,
                username = "user_12345",
                preferredLanguage = "en"
            )

            repository.bootstrapUser = user
            repository.fetchMeUser = user

            val viewModel = createViewModel(repository)

            viewModel.bootstrap()

            advanceUntilIdle()

            val state = viewModel.state.value

            assertTrue(state is AuthState.NeedsOnboarding)
            assertEquals(3, (state as AuthState.NeedsOnboarding).user.id)
        }

    @Test
    fun login_successSetsLoggedInAndRegistersPushToken() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeAuthSessionRepository()
            val pushRegisterer = FakePushTokenRegisterer()

            val user = user(
                id = 4,
                username = "login_user",
                preferredLanguage = "en"
            )

            repository.loginUser = user
            repository.fetchMeUser = user

            val viewModel =
                createViewModel(
                    repository = repository,
                    pushRegisterer = pushRegisterer
                )

            viewModel.login(
                identifier = "login@example.com",
                password = "password123"
            )

            advanceUntilIdle()

            assertEquals(
                listOf("login@example.com" to "password123"),
                repository.loginCalls
            )

            val state = viewModel.state.value

            assertTrue(state is AuthState.LoggedIn)
            assertEquals(4, (state as AuthState.LoggedIn).user.id)
            assertEquals(1, pushRegisterer.registerCallCount)
        }

    @Test
    fun loginWithGoogle_successSetsLoggedIn() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeAuthSessionRepository()

            val user = user(
                id = 5,
                username = "google_user",
                preferredLanguage = "en"
            )

            repository.googleLoginUser = user
            repository.fetchMeUser = user

            val viewModel = createViewModel(repository)

            viewModel.loginWithGoogle("google-token")

            advanceUntilIdle()

            assertEquals(listOf("google-token"), repository.googleLoginCalls)

            val state = viewModel.state.value

            assertTrue(state is AuthState.LoggedIn)
            assertEquals(5, (state as AuthState.LoggedIn).user.id)
        }

    @Test
    fun logoutClearsRepositoryAndSetsLoggedOut() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeAuthSessionRepository()
            val viewModel = createViewModel(repository)

            viewModel.logout()

            advanceUntilIdle()

            assertTrue(repository.logoutCalled)
            assertEquals(AuthState.LoggedOut, viewModel.state.value)
        }

    @Test
    fun loginWithExternalTokenSavesTokenAndBootstraps() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeAuthSessionRepository()

            val user = user(
                id = 6,
                username = "external_user",
                preferredLanguage = "en"
            )

            repository.bootstrapUser = user
            repository.fetchMeUser = user

            val viewModel = createViewModel(repository)

            viewModel.loginWithExternalToken("external-token")

            advanceUntilIdle()

            assertEquals("external-token", repository.savedExternalToken)

            val state = viewModel.state.value

            assertTrue(state is AuthState.LoggedIn)
            assertEquals(6, (state as AuthState.LoggedIn).user.id)
        }

    @Test
    fun encryptionKeyFailureSetsNeedsKeyRestore() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeAuthSessionRepository()
            val accountKeyService = FakeAccountKeyService()
            accountKeyService.ensureShouldFail = true

            val user = user(
                id = 7,
                username = "restore_user",
                preferredLanguage = "en",
                publicKey = "server-public-key"
            )

            repository.loginUser = user

            val viewModel =
                createViewModel(
                    repository = repository,
                    accountKeyService = accountKeyService
                )

            viewModel.login(
                identifier = "restore@example.com",
                password = "password123"
            )

            advanceUntilIdle()

            val state = viewModel.state.value

            assertTrue(state is AuthState.NeedsKeyRestore)
            assertEquals(7, (state as AuthState.NeedsKeyRestore).user.id)
            assertEquals("Key restore boom", state.message)
        }

    @Test
    fun markOnboardingCompleteSetsLoggedIn() {
        val repository = FakeAuthSessionRepository()
        val viewModel = createViewModel(repository)

        val user = user(
            id = 8,
            username = "complete_user",
            preferredLanguage = "en"
        )

        viewModel.markOnboardingComplete(user)

        val state = viewModel.state.value

        assertTrue(state is AuthState.LoggedIn)
        assertEquals(8, (state as AuthState.LoggedIn).user.id)
    }

    @Test
    fun showRegistrationSetsRegistering() {
        val repository = FakeAuthSessionRepository()
        val viewModel = createViewModel(repository)

        viewModel.showRegistration()

        assertEquals(AuthState.Registering, viewModel.state.value)
    }

    @Test
    fun showLoginSetsLoggedOut() {
        val repository = FakeAuthSessionRepository()
        val viewModel = createViewModel(repository)

        viewModel.showLogin()

        assertEquals(AuthState.LoggedOut, viewModel.state.value)
    }

    private fun createViewModel(
        repository: FakeAuthSessionRepository,
        accountKeyService: FakeAccountKeyService = FakeAccountKeyService(),
        pushRegisterer: FakePushTokenRegisterer? = null
    ): AuthViewModel {
        return AuthViewModel(
            repository = repository,
            accountKeyManager = accountKeyService,
            pushTokenRegistrar = pushRegisterer,
            pushDispatcher = mainDispatcherRule.testDispatcher,
            autoBootstrap = false
        )
    }

    private fun user(
        id: Int,
        username: String,
        preferredLanguage: String?,
        publicKey: String? = null
    ): UserDto {
        return UserDto(
            id = id,
            email = "$username@example.com",
            username = username,
            publicKey = publicKey,
            preferredLanguage = preferredLanguage,
            uiLanguage = preferredLanguage,
            theme = "dawn"
        )
    }

    private class FakeAuthSessionRepository : AuthSessionRepository {
        var bootstrapUser: UserDto? = null
        var loginUser: UserDto? = null
        var googleLoginUser: UserDto? = null
        var fetchMeUser: UserDto? = null

        val loginCalls = mutableListOf<Pair<String, String>>()
        val googleLoginCalls = mutableListOf<String>()

        var savedExternalToken: String? = null
        var logoutCalled = false
        val rotatedKeys = mutableListOf<String>()

        override suspend fun login(
            identifier: String,
            password: String
        ): UserDto {
            loginCalls.add(identifier to password)
            return loginUser ?: error("No login user configured.")
        }

        override suspend fun loginWithGoogle(
            idToken: String
        ): UserDto {
            googleLoginCalls.add(idToken)
            return googleLoginUser ?: error("No Google login user configured.")
        }

        override suspend fun fetchMe(): UserDto {
            return fetchMeUser ?: error("No fetchMe user configured.")
        }

        override suspend fun rotateEncryptionKey(
            publicKey: String
        ) {
            rotatedKeys.add(publicKey)
        }

        override suspend fun bootstrap(): UserDto? {
            return bootstrapUser
        }

        override fun saveExternalToken(
            token: String
        ) {
            savedExternalToken = token
        }

        override fun logout() {
            logoutCalled = true
        }
    }

    private class FakeAccountKeyService : AccountKeyService {
        var ensureShouldFail = false
        var resetShouldFail = false

        val ensuredServerKeys = mutableListOf<String?>()
        var resetCalled = false

        override suspend fun ensureLocalKeysExist(
            serverPublicKey: String?,
            uploadPublicKey: suspend (String) -> Unit
        ) {
            if (ensureShouldFail) {
                throw Exception("Key restore boom")
            }

            ensuredServerKeys.add(serverPublicKey)

            if (serverPublicKey.isNullOrBlank()) {
                uploadPublicKey("generated-public-key")
            }
        }

        override suspend fun resetAccountEncryption(
            uploadPublicKey: suspend (String) -> Unit
        ) {
            if (resetShouldFail) {
                throw Exception("Reset boom")
            }

            resetCalled = true
            uploadPublicKey("reset-public-key")
        }
    }

    private class FakePushTokenRegisterer : PushTokenRegisterer {
        var registerCallCount = 0

        override suspend fun registerCurrentFcmToken() {
            registerCallCount++
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {

    override fun starting(
        description: Description
    ) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(
        description: Description
    ) {
        Dispatchers.resetMain()
    }
}