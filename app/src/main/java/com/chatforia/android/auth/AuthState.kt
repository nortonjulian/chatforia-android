package com.chatforia.android.auth

sealed interface AuthState {

    data object Loading : AuthState

    data object LoggedOut : AuthState

    data class LoggedIn(
        val user: UserDto
    ) : AuthState

    data class NeedsOnboarding(
        val user: UserDto
    ) : AuthState
}