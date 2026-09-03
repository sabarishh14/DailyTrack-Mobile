package com.example.dailytrack_mobile.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property to create the DataStore instance
val Context.dataStore by preferencesDataStore(name = "settings")

class ThemeManager(private val context: Context) {
    companion object {
        val THEME_KEY = stringPreferencesKey("app_theme")
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val AMOLED_KEY = booleanPreferencesKey("with_amoled")
    }

    // Get the theme from local storage (Defaults to YELLOW)
    val themeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_KEY] ?: "YELLOW" 
    }

    // Get the theme mode from local storage (Defaults to SYSTEM)
    val themeModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE_KEY] ?: "SYSTEM"
    }

    // Get the amoled / true black state from local storage (Defaults to false)
    val amoledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AMOLED_KEY] ?: false
    }

    // Save the new theme to local storage
    suspend fun saveTheme(themeName: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = themeName
        }
    }

    // Save the theme mode (SYSTEM, LIGHT, DARK)
    suspend fun saveThemeMode(themeMode: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = themeMode
        }
    }

    // Save the amoled / true black state
    suspend fun saveAmoled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AMOLED_KEY] = enabled
        }
    }
}

