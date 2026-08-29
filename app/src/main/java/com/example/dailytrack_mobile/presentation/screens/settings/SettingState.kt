package com.example.dailytrack_mobile.presentation.screens.settings

import com.example.dailytrack_mobile.data.local.security.LockType
import com.example.dailytrack_mobile.presentation.theme.AppTheme

data class SettingsState(
    val selectedTheme: AppTheme = AppTheme.YELLOW,
    val appVersion: String = "v1.0.0",
    val developerName: String = "Your Name",
    val isAppLockEnabled: Boolean = false,
    val lockType: LockType = LockType.SYSTEM,
    val isBiometricWithPinEnabled: Boolean = true,
    val hasCustomPin: Boolean = false,
    val isHideBalancesOnStartup: Boolean = false
)