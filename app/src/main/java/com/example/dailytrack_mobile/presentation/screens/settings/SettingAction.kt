package com.example.dailytrack_mobile.presentation.screens.settings

import com.example.dailytrack_mobile.presentation.theme.AppTheme

sealed interface SettingsAction {
    data class OnThemeChanged(val newTheme: AppTheme) : SettingsAction
    object OnBackClicked : SettingsAction
}