package com.example.dailytrack_mobile.data.local.auth

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore by preferencesDataStore(name = "auth_prefs")

@Singleton
class AuthManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        val KEY_AUTH_TOKEN = stringPreferencesKey("auth_jwt_token")
        val KEY_USER_EMAIL = stringPreferencesKey("auth_user_email")
        val KEY_USER_NAME = stringPreferencesKey("auth_user_name")
        val KEY_USER_PHOTO = stringPreferencesKey("auth_user_photo")
        val KEY_IS_ADMIN = booleanPreferencesKey("auth_is_admin")
    }

    val authTokenFlow: Flow<String?> = context.authDataStore.data.map { preferences ->
        preferences[KEY_AUTH_TOKEN]
    }

    val userEmailFlow: Flow<String?> = context.authDataStore.data.map { preferences ->
        preferences[KEY_USER_EMAIL]
    }

    val userNameFlow: Flow<String?> = context.authDataStore.data.map { preferences ->
        preferences[KEY_USER_NAME]
    }

    val isAdminFlow: Flow<Boolean> = context.authDataStore.data.map { preferences ->
        preferences[KEY_IS_ADMIN] ?: false
    }

    val isLoggedInFlow: StateFlow<Boolean?> = context.authDataStore.data.map { preferences ->
        val token = preferences[KEY_AUTH_TOKEN]
        !token.isNullOrBlank()
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    @Volatile
    private var inMemoryToken: String? = null

    init {
        // Cache token in memory for synchronous OkHttp interceptor reads
        scope.launch {
            authTokenFlow.collect { token ->
                inMemoryToken = token
            }
        }
    }

    fun getCachedToken(): String? = inMemoryToken

    suspend fun saveSession(
        token: String,
        email: String,
        name: String? = null,
        photoUrl: String? = null,
        isAdmin: Boolean = false
    ) {
        inMemoryToken = token
        context.authDataStore.edit { preferences ->
            preferences[KEY_AUTH_TOKEN] = token
            preferences[KEY_USER_EMAIL] = email
            if (name != null) preferences[KEY_USER_NAME] = name
            if (photoUrl != null) preferences[KEY_USER_PHOTO] = photoUrl
            preferences[KEY_IS_ADMIN] = isAdmin
        }
    }

    suspend fun clearSession() {
        inMemoryToken = null
        context.authDataStore.edit { preferences ->
            preferences.remove(KEY_AUTH_TOKEN)
            preferences.remove(KEY_USER_EMAIL)
            preferences.remove(KEY_USER_NAME)
            preferences.remove(KEY_USER_PHOTO)
            preferences.remove(KEY_IS_ADMIN)
        }
    }
}
