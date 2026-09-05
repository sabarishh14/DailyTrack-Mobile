package com.example.dailytrack_mobile.data.repository

import com.example.dailytrack_mobile.data.local.auth.AuthManager
import com.example.dailytrack_mobile.data.remote.api.DailyTrackApi
import com.example.dailytrack_mobile.data.remote.dto.FirebaseLoginRequestDto
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: DailyTrackApi,
    private val authManager: AuthManager
) {
    val isLoggedInFlow: StateFlow<Boolean?> = authManager.isLoggedInFlow
    val userEmailFlow: Flow<String?> = authManager.userEmailFlow
    val userNameFlow: Flow<String?> = authManager.userNameFlow
    val isAdminFlow: Flow<Boolean> = authManager.isAdminFlow

    suspend fun loginWithFirebaseToken(
        idToken: String,
        email: String,
        name: String? = null,
        photoUrl: String? = null
    ): Result<Boolean> {
        return try {
            val response = api.firebaseLogin(FirebaseLoginRequestDto(id_token = idToken))
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success && !body.token.isNullOrBlank()) {
                    authManager.saveSession(
                        token = body.token,
                        email = email,
                        name = name,
                        photoUrl = photoUrl,
                        isAdmin = body.isAdmin ?: false
                    )
                    Result.success(true)
                } else {
                    Result.failure(Exception(body?.message ?: "Login failed. You may not be an authorized user."))
                }
            } else {
                val errorMsg = when (response.code()) {
                    403 -> "Access denied. Your Google account ($email) is not authorized."
                    401 -> "Invalid credentials or expired Google session."
                    else -> "Server error (${response.code()}). Please try again."
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (_: Exception) { }
        authManager.clearSession()
    }
}
