package com.example.dailytrack_mobile.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun DailyTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    appTheme: AppTheme = AppTheme.YELLOW,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled by default so theme switcher works on API 31+
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> when (appTheme) {
            AppTheme.YELLOW -> YellowThemeColors.darkScheme
            AppTheme.GREEN -> GreenThemeColors.darkScheme
            AppTheme.TEAL -> TealThemeColors.darkScheme
            AppTheme.PURPLE -> PurpleThemeColors.darkScheme
        }
        else -> when (appTheme) {
            AppTheme.YELLOW -> YellowThemeColors.lightScheme
            AppTheme.GREEN -> GreenThemeColors.lightScheme
            AppTheme.TEAL -> TealThemeColors.lightScheme
            AppTheme.PURPLE -> PurpleThemeColors.lightScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
