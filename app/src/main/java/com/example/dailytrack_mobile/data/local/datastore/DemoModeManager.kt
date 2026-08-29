package com.example.dailytrack_mobile.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class DemoModeManager(private val context: Context) {

    companion object {
        val KEY_DEMO_MODE_ENABLED = booleanPreferencesKey("demo_mode_enabled")
    }

    val isDemoModeEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_DEMO_MODE_ENABLED] ?: false
    }

    suspend fun setDemoModeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DEMO_MODE_ENABLED] = enabled
        }
    }

    suspend fun isDemoModeEnabled(): Boolean {
        val preferences = context.dataStore.data.first()
        return preferences[KEY_DEMO_MODE_ENABLED] ?: false
    }
}
