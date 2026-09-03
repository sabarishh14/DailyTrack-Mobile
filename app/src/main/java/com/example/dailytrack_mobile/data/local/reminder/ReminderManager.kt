package com.example.dailytrack_mobile.data.local.reminder

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.example.dailytrack_mobile.data.local.datastore.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek

class ReminderManager(private val context: Context) {

    companion object {
        val KEY_REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val KEY_REMINDER_TIME = stringPreferencesKey("reminder_time")
        val KEY_REMINDER_DAYS = stringSetPreferencesKey("reminder_days")

        const val DEFAULT_TIME = "21:00"
        val DEFAULT_DAYS: Set<DayOfWeek> = DayOfWeek.values().toSet()
    }

    val isReminderEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_REMINDER_ENABLED] ?: false
    }

    val reminderTimeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_REMINDER_TIME] ?: DEFAULT_TIME
    }

    val reminderDaysFlow: Flow<Set<DayOfWeek>> = context.dataStore.data.map { preferences ->
        val rawDays = preferences[KEY_REMINDER_DAYS]
        if (rawDays == null) {
            DEFAULT_DAYS
        } else {
            rawDays.mapNotNull { name ->
                try {
                    DayOfWeek.valueOf(name)
                } catch (e: Exception) {
                    null
                }
            }.toSet()
        }
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_REMINDER_ENABLED] = enabled
        }
    }

    suspend fun setReminderTime(time: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_REMINDER_TIME] = time
        }
    }

    suspend fun setReminderDays(days: Set<DayOfWeek>) {
        context.dataStore.edit { preferences ->
            preferences[KEY_REMINDER_DAYS] = days.map { it.name }.toSet()
        }
    }
}
