package com.chatforia.android.auth

import analytics.FakeAnalyticsTracker
import com.chatforia.android.crypto.AccountKeyService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelAnalyticsTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun login_identifiesUserAndCapturesPasswordLoginEvent() = runTest {
        val user =
            UserDto(
                id = 123,
                username = "julian",
                preferredLanguage = "en",
                plan = "free",
                publicKey = "server-public-key"
            )

        val repository =
            FakeAuthSessionRepository(
                loginUser = user,
                fetchMeUser = user
            )

        val analytics = FakeAnalyticsTracker()

        val viewModel =
            AuthViewModel(
                repository = repository,
                accountKeyManager = FakeAccountKeyService(),
                pushDispatcher = testDispatcher,
                autoBootstrap = false,
                analytics = analytics
            )

        viewModel.login(
            identifier = "julian",
            password = "password123"
        )

        assertEquals(listOf(123), analytics.identifiedUsers)

        assertEquals(
            "account logged in",
            analytics.events.first().name
        )

        assertEquals(
            "password",
            analytics.events.first().properties["method"]
        )

        assertTrue(viewModel.state.value is AuthState.LoggedIn)
    }

    @Test
    fun loginWithGoogle_identifiesUserAndCapturesGoogleLoginEvent() = runTest {
        val user =
            UserDto(
                id = 456,
                username = "google_user",
                preferredLanguage = "es",
                plan = "premium",
                publicKey = "server-public-key"
            )

        val repository =
            FakeAuthSessionRepository(
                googleUser = user,
                fetchMeUser = user
            )

        val analytics = FakeAnalyticsTracker()

        val viewModel =
            AuthViewModel(
                repository = repository,
                accountKeyManager = FakeAccountKeyService(),
                pushDispatcher = testDispatcher,
                autoBootstrap = false,
                analytics = analytics
            )

        viewModel.loginWithGoogle("fake-google-token")

        assertEquals(listOf(456), analytics.identifiedUsers)

        assertEquals(
            "account logged in",
            analytics.events.first().name
        )

        assertEquals(
            "google",
            analytics.events.first().properties["method"]
        )
    }

    @Test
    fun logout_capturesLogoutEventAndResetsAnalytics() {
        val repository = FakeAuthSessionRepository()
        val analytics = FakeAnalyticsTracker()

        val viewModel =
            AuthViewModel(
                repository = repository,
                accountKeyManager = FakeAccountKeyService(),
                pushDispatcher = testDispatcher,
                autoBootstrap = false,
                analytics = analytics
            )

        viewModel.logout()

        assertEquals(
            "account logged out",
            analytics.events.first().name
        )

        assertTrue(analytics.resetCalled)
        assertTrue(repository.logoutCalled)
        assertTrue(viewModel.state.value is AuthState.LoggedOut)
    }
}

private class FakeAuthSessionRepository(
    private val loginUser: UserDto =
        UserDto(
            id = 1,
            username = "test_user",
            preferredLanguage = "en",
            publicKey = "server-public-key"
        ),

    private val googleUser: UserDto =
        UserDto(
            id = 2,
            username = "google_user",
            preferredLanguage = "en",
            publicKey = "server-public-key"
        ),

    private val fetchMeUser: UserDto =
        UserDto(
            id = 1,
            username = "test_user",
            preferredLanguage = "en",
            publicKey = "server-public-key"
        )
) : AuthSessionRepository {

    var logoutCalled = false

    override suspend fun login(
        identifier: String,
        password: String
    ): UserDto {
        return loginUser
    }

    override suspend fun loginWithGoogle(
        idToken: String
    ): UserDto {
        return googleUser
    }

    override suspend fun fetchMe(): UserDto {
        return fetchMeUser
    }

    override suspend fun rotateEncryptionKey(
        publicKey: String
    ) {
        // No-op for test.
    }

    override suspend fun bootstrap(): UserDto? {
        return null
    }

    override fun saveExternalToken(
        token: String
    ) {
        // No-op for test.
    }

    override fun logout() {
        logoutCalled = true
    }
}

private class FakeAccountKeyService : AccountKeyService {

    override suspend fun ensureLocalKeysExist(
        serverPublicKey: String?,
        uploadPublicKey: suspend (String) -> Unit
    ) {
        // No-op for test.
    }

    override suspend fun resetAccountEncryption(
        uploadPublicKey: suspend (String) -> Unit
    ) {
        uploadPublicKey("new-public-key")
    }
}