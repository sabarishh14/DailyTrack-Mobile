package com.example.dailytrack_mobile.presentation.screens.settings

import com.example.dailytrack_mobile.data.local.security.LockType
import com.example.dailytrack_mobile.presentation.theme.AppTheme
import com.example.dailytrack_mobile.presentation.theme.ThemeMode

import java.time.DayOfWeek

data class SettingsState(
    val selectedTheme: AppTheme = AppTheme.YELLOW,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val withAmoled: Boolean = false,
    val appVersion: String = "v1.0.0",
    val developerName: String = "Sabarish SB",
    val isAppLockEnabled: Boolean = false,
    val lockType: LockType = LockType.SYSTEM,
    val isBiometricWithPinEnabled: Boolean = true,
    val hasCustomPin: Boolean = false,
    val isDemoModeEnabled: Boolean = false,
    val isSyncing: Boolean = false,
    val syncStatusMessage: String? = null,
    val isLastSyncSuccess: Boolean? = null,
    val syncStepDescription: String? = null,
    val isRefreshingServerStatus: Boolean = false,
    val serverStatusResult: Boolean? = null,
    val isReminderEnabled: Boolean = false,
    val reminderTime: String = "21:00",
    val reminderDays: Set<DayOfWeek> = DayOfWeek.values().toSet(),
    val loggedInUserEmail: String? = null,
    val loggedInUserName: String? = null,
    val isUserAdmin: Boolean = false
)