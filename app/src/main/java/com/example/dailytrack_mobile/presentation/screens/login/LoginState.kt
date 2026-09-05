package com.example.dailytrack_mobile.presentation.screens.login

data class LoginState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoginSuccess: Boolean = false
)

sealed interface LoginAction {
    data class OnGoogleTokenReceived(
        val firebaseIdToken: String,
        val email: String,
        val displayName: String?,
        val photoUrl: String?
    ) : LoginAction
    data class OnLoginFailed(val message: String) : LoginAction
    object OnClearError : LoginAction
    object OnContinueDemoMode : LoginAction
}
