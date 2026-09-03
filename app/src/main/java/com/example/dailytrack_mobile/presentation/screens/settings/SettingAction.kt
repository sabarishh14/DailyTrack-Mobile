package com.example.dailytrack_mobile.presentation.screens.settings

import com.example.dailytrack_mobile.data.local.security.LockType
import com.example.dailytrack_mobile.presentation.theme.AppTheme
import com.example.dailytrack_mobile.presentation.theme.ThemeMode

sealed interface SettingsAction {
    data class OnThemeChanged(val newTheme: AppTheme) : SettingsAction
    data class OnThemeModeChanged(val newMode: ThemeMode) : SettingsAction
    data class OnAmoledToggled(val enabled: Boolean) : SettingsAction
    object OnBackClicked : SettingsAction
    data class OnAppLockToggled(val enabled: Boolean) : SettingsAction
    data class OnLockTypeSelected(val lockType: LockType) : SettingsAction
    data class OnSaveCustomPin(val pin: String) : SettingsAction
    data class OnBiometricWithPinToggled(val enabled: Boolean) : SettingsAction
    data class OnDemoModeToggled(val enabled: Boolean) : SettingsAction
    object OnResetDemoDataClicked : SettingsAction
    object OnForceSyncClicked : SettingsAction
    object OnServerStatusClicked : SettingsAction
}