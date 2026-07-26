package com.example.dailytrack_mobile.presentation.screens.settings

import com.example.dailytrack_mobile.presentation.theme.AppTheme

data class SettingsState(
    val selectedTheme: AppTheme = AppTheme.YELLOW, // Default theme
    val appVersion: String = "1.0.0",            // App metadata
    val developerName: String = "Your Name"      // App metadata
)