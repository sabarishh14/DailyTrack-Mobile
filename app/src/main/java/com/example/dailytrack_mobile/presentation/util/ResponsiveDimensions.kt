package com.example.dailytrack_mobile.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// Screen size tiers
// ─────────────────────────────────────────────────────────────────────────────

enum class ScreenSizeTier {
    COMPACT,   // < 360dp  (small / old phones)
    STANDARD,  // 360–410dp (most modern phones)
    EXPANDED   // > 410dp  (large phones, foldables)
}

fun screenSizeTierOf(screenWidthDp: Int): ScreenSizeTier = when {
    screenWidthDp < 360 -> ScreenSizeTier.COMPACT
    screenWidthDp <= 410 -> ScreenSizeTier.STANDARD
    else -> ScreenSizeTier.EXPANDED
}

// ─────────────────────────────────────────────────────────────────────────────
// Dimension definitions
// ─────────────────────────────────────────────────────────────────────────────

@Immutable
data class AppDimensions(
    // Screen-level padding
    val screenHorizontalPadding: Dp,
    val screenTopPadding: Dp,
    val screenBottomPadding: Dp,

    // Section / card spacing
    val sectionSpacing: Dp,
    val cardInnerPadding: Dp,
    val cardCornerRadius: Dp,

    // Item spacing
    val itemSpacingSmall: Dp,
    val itemSpacingMedium: Dp,
    val itemSpacingLarge: Dp,

    // Icon sizes
    val iconSizeSmall: Dp,
    val iconSizeMedium: Dp,
    val iconSizeLarge: Dp,
    val iconSizeXLarge: Dp,

    // Avatar / circle sizes
    val avatarSizeSmall: Dp,
    val avatarSizeMedium: Dp,
    val avatarSizeLarge: Dp,

    // Button dimensions
    val buttonHeight: Dp,
    val buttonCornerRadius: Dp,

    // Font sizes
    val fontSizeDisplayLarge: TextUnit,
    val fontSizeDisplayMedium: TextUnit,
    val fontSizeHeadlineLarge: TextUnit,
    val fontSizeHeadlineMedium: TextUnit,
    val fontSizeTitleLarge: TextUnit,
    val fontSizeTitleMedium: TextUnit,
    val fontSizeBody: TextUnit,
    val fontSizeLabel: TextUnit,
    val fontSizeLabelSmall: TextUnit,

    // Stat card specific
    val statCardValueFontSize: TextUnit,
    val statCardPaddingVertical: Dp,
    val statCardPaddingHorizontal: Dp,

    // Chart specific
    val donutChartSize: Dp,
    val sparklineHeight: Dp,

    // Bottom nav specific
    val bottomNavIconSize: Dp,
    val bottomNavLabelFontSize: TextUnit,
    val bottomNavVerticalPadding: Dp,

    // Theme picker specific
    val themeCircleSize: Dp,
    val themeCircleCanvasSize: Dp,
    val themeCircleCheckSize: Dp,
    val themeCircleSpacing: Dp,

    // Form specific
    val formHorizontalPadding: Dp,
    val formVerticalSpacing: Dp,
    val formSectionLabelFontSize: TextUnit,

    // Action sheet specific
    val actionIconCircleSize: Dp,
    val actionIconSize: Dp,

    // Media card specific (Sabdekho)
    val mediaCardHeight2Col: Dp,
    val mediaCardHeight3Col: Dp,
    val mediaCardHeight4Col: Dp,

    // Search bar
    val searchBarHeight: Dp,

    // Divider vertical line
    val dividerLineHeight: Dp,

    // Investment summary mini card padding
    val miniCardPaddingHorizontal: Dp,
    val miniCardPaddingVertical: Dp,
)

// ─────────────────────────────────────────────────────────────────────────────
// Dimension presets per tier
// ─────────────────────────────────────────────────────────────────────────────

private val CompactDimensions = AppDimensions(
    screenHorizontalPadding = 12.dp,
    screenTopPadding = 14.dp,
    screenBottomPadding = 24.dp,
    sectionSpacing = 16.dp,
    cardInnerPadding = 14.dp,
    cardCornerRadius = 16.dp,
    itemSpacingSmall = 4.dp,
    itemSpacingMedium = 8.dp,
    itemSpacingLarge = 12.dp,
    iconSizeSmall = 14.dp,
    iconSizeMedium = 20.dp,
    iconSizeLarge = 28.dp,
    iconSizeXLarge = 36.dp,
    avatarSizeSmall = 28.dp,
    avatarSizeMedium = 32.dp,
    avatarSizeLarge = 48.dp,
    buttonHeight = 44.dp,
    buttonCornerRadius = 12.dp,
    fontSizeDisplayLarge = 32.sp,
    fontSizeDisplayMedium = 28.sp,
    fontSizeHeadlineLarge = 22.sp,
    fontSizeHeadlineMedium = 18.sp,
    fontSizeTitleLarge = 16.sp,
    fontSizeTitleMedium = 14.sp,
    fontSizeBody = 13.sp,
    fontSizeLabel = 11.sp,
    fontSizeLabelSmall = 10.sp,
    statCardValueFontSize = 28.sp,
    statCardPaddingVertical = 14.dp,
    statCardPaddingHorizontal = 8.dp,
    donutChartSize = 160.dp,
    sparklineHeight = 100.dp,
    bottomNavIconSize = 20.dp,
    bottomNavLabelFontSize = 10.sp,
    bottomNavVerticalPadding = 4.dp,
    themeCircleSize = 60.dp,
    themeCircleCanvasSize = 42.dp,
    themeCircleCheckSize = 20.dp,
    themeCircleSpacing = 8.dp,
    formHorizontalPadding = 14.dp,
    formVerticalSpacing = 18.dp,
    formSectionLabelFontSize = 12.sp,
    actionIconCircleSize = 44.dp,
    actionIconSize = 22.dp,
    mediaCardHeight2Col = 220.dp,
    mediaCardHeight3Col = 150.dp,
    mediaCardHeight4Col = 110.dp,
    searchBarHeight = 48.dp,
    dividerLineHeight = 30.dp,
    miniCardPaddingHorizontal = 10.dp,
    miniCardPaddingVertical = 10.dp,
)

private val StandardDimensions = AppDimensions(
    screenHorizontalPadding = 16.dp,
    screenTopPadding = 20.dp,
    screenBottomPadding = 32.dp,
    sectionSpacing = 20.dp,
    cardInnerPadding = 20.dp,
    cardCornerRadius = 20.dp,
    itemSpacingSmall = 6.dp,
    itemSpacingMedium = 10.dp,
    itemSpacingLarge = 16.dp,
    iconSizeSmall = 16.dp,
    iconSizeMedium = 24.dp,
    iconSizeLarge = 32.dp,
    iconSizeXLarge = 42.dp,
    avatarSizeSmall = 34.dp,
    avatarSizeMedium = 36.dp,
    avatarSizeLarge = 56.dp,
    buttonHeight = 52.dp,
    buttonCornerRadius = 14.dp,
    fontSizeDisplayLarge = 38.sp,
    fontSizeDisplayMedium = 32.sp,
    fontSizeHeadlineLarge = 26.sp,
    fontSizeHeadlineMedium = 22.sp,
    fontSizeTitleLarge = 20.sp,
    fontSizeTitleMedium = 16.sp,
    fontSizeBody = 14.sp,
    fontSizeLabel = 12.sp,
    fontSizeLabelSmall = 11.sp,
    statCardValueFontSize = 36.sp,
    statCardPaddingVertical = 20.dp,
    statCardPaddingHorizontal = 12.dp,
    donutChartSize = 200.dp,
    sparklineHeight = 120.dp,
    bottomNavIconSize = 24.dp,
    bottomNavLabelFontSize = 11.sp,
    bottomNavVerticalPadding = 6.dp,
    themeCircleSize = 80.dp,
    themeCircleCanvasSize = 56.dp,
    themeCircleCheckSize = 24.dp,
    themeCircleSpacing = 12.dp,
    formHorizontalPadding = 20.dp,
    formVerticalSpacing = 24.dp,
    formSectionLabelFontSize = 14.sp,
    actionIconCircleSize = 56.dp,
    actionIconSize = 28.dp,
    mediaCardHeight2Col = 260.dp,
    mediaCardHeight3Col = 180.dp,
    mediaCardHeight4Col = 130.dp,
    searchBarHeight = 56.dp,
    dividerLineHeight = 36.dp,
    miniCardPaddingHorizontal = 12.dp,
    miniCardPaddingVertical = 14.dp,
)

private val ExpandedDimensions = AppDimensions(
    screenHorizontalPadding = 20.dp,
    screenTopPadding = 24.dp,
    screenBottomPadding = 36.dp,
    sectionSpacing = 24.dp,
    cardInnerPadding = 24.dp,
    cardCornerRadius = 22.dp,
    itemSpacingSmall = 8.dp,
    itemSpacingMedium = 12.dp,
    itemSpacingLarge = 20.dp,
    iconSizeSmall = 18.dp,
    iconSizeMedium = 26.dp,
    iconSizeLarge = 36.dp,
    iconSizeXLarge = 48.dp,
    avatarSizeSmall = 38.dp,
    avatarSizeMedium = 40.dp,
    avatarSizeLarge = 64.dp,
    buttonHeight = 56.dp,
    buttonCornerRadius = 16.dp,
    fontSizeDisplayLarge = 42.sp,
    fontSizeDisplayMedium = 36.sp,
    fontSizeHeadlineLarge = 28.sp,
    fontSizeHeadlineMedium = 24.sp,
    fontSizeTitleLarge = 22.sp,
    fontSizeTitleMedium = 18.sp,
    fontSizeBody = 15.sp,
    fontSizeLabel = 13.sp,
    fontSizeLabelSmall = 12.sp,
    statCardValueFontSize = 40.sp,
    statCardPaddingVertical = 24.dp,
    statCardPaddingHorizontal = 16.dp,
    donutChartSize = 240.dp,
    sparklineHeight = 140.dp,
    bottomNavIconSize = 26.dp,
    bottomNavLabelFontSize = 12.sp,
    bottomNavVerticalPadding = 8.dp,
    themeCircleSize = 90.dp,
    themeCircleCanvasSize = 64.dp,
    themeCircleCheckSize = 26.dp,
    themeCircleSpacing = 16.dp,
    formHorizontalPadding = 24.dp,
    formVerticalSpacing = 28.dp,
    formSectionLabelFontSize = 15.sp,
    actionIconCircleSize = 64.dp,
    actionIconSize = 32.dp,
    mediaCardHeight2Col = 300.dp,
    mediaCardHeight3Col = 210.dp,
    mediaCardHeight4Col = 150.dp,
    searchBarHeight = 56.dp,
    dividerLineHeight = 40.dp,
    miniCardPaddingHorizontal = 14.dp,
    miniCardPaddingVertical = 16.dp,
)

fun dimensionsFor(tier: ScreenSizeTier): AppDimensions = when (tier) {
    ScreenSizeTier.COMPACT -> CompactDimensions
    ScreenSizeTier.STANDARD -> StandardDimensions
    ScreenSizeTier.EXPANDED -> ExpandedDimensions
}

// ─────────────────────────────────────────────────────────────────────────────
// CompositionLocal
// ─────────────────────────────────────────────────────────────────────────────

val LocalAppDimensions = staticCompositionLocalOf { StandardDimensions }

/**
 * Wrap your root content in this to auto-detect the screen tier and
 * provide the correct dimensions via [LocalAppDimensions].
 */
@Composable
fun ProvideAppDimensions(content: @Composable () -> Unit) {
    val config = LocalConfiguration.current
    val tier = screenSizeTierOf(config.screenWidthDp)
    val dimensions = dimensionsFor(tier)
    CompositionLocalProvider(LocalAppDimensions provides dimensions) {
        content()
    }
}

/**
 * Shorthand accessor for current dimensions.
 */
object Dimens {
    val current: AppDimensions
        @Composable
        get() = LocalAppDimensions.current
}
