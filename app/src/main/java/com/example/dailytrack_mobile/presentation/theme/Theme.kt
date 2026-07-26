package com.example.dailytrack_mobile.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val BlueDarkColorScheme = darkColorScheme(
    primary = BluePrimaryDark,
    onPrimary = BlueOnPrimaryDark,
    primaryContainer = BluePrimaryContainerDark,
    secondary = BlueSecondaryDark,
    background = BlueBackgroundDark,
    surface = BlueBackgroundDark
)

private val BlueLightColorScheme = lightColorScheme(
    primary = BluePrimaryLight,
    onPrimary = BlueOnPrimaryLight,
    primaryContainer = BluePrimaryContainerLight,
    secondary = BlueSecondaryLight,
    background = BlueBackgroundLight,
    surface = BlueBackgroundLight
)

private val PurpleDarkColorScheme = darkColorScheme(
    primary = PurplePrimaryDark,
    onPrimary = PurpleOnPrimaryDark,
    primaryContainer = PurplePrimaryContainerDark,
    secondary = PurpleSecondaryDark,
    background = PurpleBackgroundDark,
    surface = PurpleBackgroundDark
)

private val PurpleLightColorScheme = lightColorScheme(
    primary = PurplePrimaryLight,
    onPrimary = PurpleOnPrimaryLight,
    primaryContainer = PurplePrimaryContainerLight,
    secondary = PurpleSecondaryLight,
    background = PurpleBackgroundLight,
    surface = PurpleBackgroundLight
)

private val GreenDarkColorScheme = darkColorScheme(
    primary = GreenPrimaryDark,
    onPrimary = GreenOnPrimaryDark,
    primaryContainer = GreenPrimaryContainerDark,
    secondary = GreenSecondaryDark,
    background = GreenBackgroundDark,
    surface = GreenBackgroundDark
)

private val GreenLightColorScheme = lightColorScheme(
    primary = GreenPrimaryLight,
    onPrimary = GreenOnPrimaryLight,
    primaryContainer = GreenPrimaryContainerLight,
    secondary = GreenSecondaryLight,
    background = GreenBackgroundLight,
    surface = GreenBackgroundLight
)

@Composable
fun DailyTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    appTheme: AppTheme = AppTheme.BLUE,
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
            AppTheme.BLUE -> BlueDarkColorScheme
            AppTheme.PURPLE -> PurpleDarkColorScheme
            AppTheme.GREEN -> GreenDarkColorScheme
        }
        else -> when (appTheme) {
            AppTheme.BLUE -> BlueLightColorScheme
            AppTheme.PURPLE -> PurpleLightColorScheme
            AppTheme.GREEN -> GreenLightColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
