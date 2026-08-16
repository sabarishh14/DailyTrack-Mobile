package com.example.dailytrack_mobile.presentation.screens.settings

import com.example.dailytrack_mobile.presentation.theme.AppTheme

sealed interface SettingsAction {
    data class OnThemeChanged(val newTheme: AppTheme) : SettingsAction
    object OnBackClicked : SettingsAction
    data class OnAppLockToggled(val enabled: Boolean) : SettingsAction
    data class OnHideBalancesToggled(val enabled: Boolean) : SettingsAction
    object OnForceSyncClicked : SettingsAction
    object OnServerStatusClicked : SettingsAction
}