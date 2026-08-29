package com.example.dailytrack_mobile.presentation.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailytrack_mobile.presentation.theme.AppTheme
import com.example.dailytrack_mobile.presentation.theme.GreenThemeColors
import com.example.dailytrack_mobile.presentation.theme.PurpleThemeColors
import com.example.dailytrack_mobile.presentation.theme.TealThemeColors
import com.example.dailytrack_mobile.presentation.theme.YellowThemeColors
import com.example.dailytrack_mobile.presentation.util.Dimens

// ─────────────────────────────────────────────────────────────────────────────
// Theme preview helper
// ─────────────────────────────────────────────────────────────────────────────

private data class ThemePreviewColors(
    val top: Color,
    val bottomLeft: Color,
    val bottomRight: Color
)

private fun previewColorsFor(theme: AppTheme): ThemePreviewColors = when (theme) {
    AppTheme.YELLOW -> ThemePreviewColors(
        top = YellowThemeColors.lightScheme.primaryContainer,
        bottomLeft = YellowThemeColors.lightScheme.secondaryContainer,
        bottomRight = YellowThemeColors.lightScheme.tertiaryContainer
    )
    AppTheme.GREEN -> ThemePreviewColors(
        top = GreenThemeColors.lightScheme.primaryContainer,
        bottomLeft = GreenThemeColors.lightScheme.secondaryContainer,
        bottomRight = GreenThemeColors.lightScheme.tertiaryContainer
    )
    AppTheme.TEAL -> ThemePreviewColors(
        top = TealThemeColors.lightScheme.primaryContainer,
        bottomLeft = TealThemeColors.lightScheme.secondaryContainer,
        bottomRight = TealThemeColors.lightScheme.tertiaryContainer
    )
    AppTheme.PURPLE -> ThemePreviewColors(
        top = PurpleThemeColors.lightScheme.primaryContainer,
        bottomLeft = PurpleThemeColors.lightScheme.secondaryContainer,
        bottomRight = PurpleThemeColors.lightScheme.tertiaryContainer
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Main Settings Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit
) {
    var currentSubScreen by remember { mutableStateOf<String?>(null) }

    if (currentSubScreen == "AppLockSettings") {
        AppLockSettingsScreen(
            state = state,
            onAction = onAction,
            onNavigateBack = { currentSubScreen = null }
        )
        return
    }

    BackHandler {
        onAction(SettingsAction.OnBackClicked)
    }

    var searchQuery by remember { mutableStateOf("") }
    val dims = Dimens.current

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(SettingsAction.OnBackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(dims.iconSizeMedium)
                        )
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = dims.screenHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(dims.sectionSpacing),
            contentPadding = PaddingValues(bottom = dims.screenBottomPadding)
        ) {

            // ── Search Bar ──────────────────────────────────────────────────
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search settings...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(dims.iconSizeMedium)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dims.searchBarHeight)
                )
            }

            // ── About / App Header ──────────────────────────────────────────
            item {
                AboutHeader(
                    appVersion = state.appVersion,
                    developerName = state.developerName
                )
            }

            // ── Group 1: Appearance ─────────────────────────────────────────
            item {
                SettingsSectionLabel(title = "Appearance")
                Spacer(Modifier.height(dims.itemSpacingMedium))
                SettingsCard {
                    ThemeSectionInCard(
                        selectedTheme = state.selectedTheme,
                        onThemeSelected = { onAction(SettingsAction.OnThemeChanged(it)) }
                    )
                }
            }

            // ── Group 2: Privacy & Security ─────────────────────────────────
            item {
                SettingsSectionLabel(title = "Privacy & Security")
                Spacer(Modifier.height(dims.itemSpacingMedium))
                SettingsCard {
                    SettingsToggleItem(
                        icon = Icons.Default.Lock,
                        title = "App Lock",
                        subtitle = if (state.isAppLockEnabled) {
                            if (state.lockType == com.example.dailytrack_mobile.data.local.security.LockType.SYSTEM)
                                "Same as screen lock"
                            else
                                "Custom PIN"
                        } else {
                            "Disabled"
                        },
                        checked = state.isAppLockEnabled,
                        onCheckedChange = { enabled ->
                            onAction(SettingsAction.OnAppLockToggled(enabled))
                            if (enabled) {
                                currentSubScreen = "AppLockSettings"
                            }
                        },
                        onClickRow = {
                            currentSubScreen = "AppLockSettings"
                        }
                    )
                    if (state.isAppLockEnabled) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 56.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                        SettingsNavItem(
                            icon = Icons.Default.Security,
                            title = "Lock options",
                            subtitle = "Change lock method or PIN",
                            onClick = { currentSubScreen = "AppLockSettings" }
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                    SettingsToggleItem(
                        icon = Icons.Default.VisibilityOff,
                        title = "Hide Balances on Startup",
                        checked = state.isHideBalancesOnStartup,
                        onCheckedChange = { onAction(SettingsAction.OnHideBalancesToggled(it)) }
                    )
                }
            }

            // ── Group 3: Data & Sync ────────────────────────────────────────
            item {
                SettingsSectionLabel(title = "Data & Sync")
                Spacer(Modifier.height(dims.itemSpacingMedium))
                SettingsCard {
                    SettingsClickItem(
                        icon = Icons.Default.Sync,
                        title = "Force Sync",
                        onClick = { onAction(SettingsAction.OnForceSyncClicked) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                    SettingsClickItem(
                        icon = Icons.Default.Cloud,
                        title = "Server Status",
                        onClick = { onAction(SettingsAction.OnServerStatusClicked) }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// About / App Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AboutHeader(
    appVersion: String,
    developerName: String
) {
    val dims = Dimens.current
    Card(
        shape = RoundedCornerShape(dims.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dims.cardInnerPadding, vertical = dims.itemSpacingLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon
            Surface(
                shape = RoundedCornerShape(dims.buttonCornerRadius),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 0.dp,
                modifier = Modifier.size(dims.avatarSizeLarge - 4.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "App Icon",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(dims.iconSizeLarge)
                    )
                }
            }

            Spacer(Modifier.width(dims.itemSpacingLarge))

            // App name + version
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DailyTrack",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = appVersion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Developer / info circular button
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(dims.avatarSizeSmall + 4.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Developer: $developerName",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(dims.iconSizeMedium)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section label
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsSectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Settings Card wrapper
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    val dims = Dimens.current
    Card(
        shape = RoundedCornerShape(dims.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Settings row: nav item (Chevron trailing)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SettingsNavItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    val dims = Dimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = dims.cardInnerPadding - 4.dp, vertical = dims.itemSpacingLarge - 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(dims.iconSizeMedium)
        )
        Spacer(Modifier.width(dims.itemSpacingLarge))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(dims.iconSizeSmall + 2.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Settings row: clickable item (no trailing icon)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsClickItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    val dims = Dimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = dims.cardInnerPadding - 4.dp, vertical = dims.itemSpacingLarge - 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(dims.iconSizeMedium)
        )
        Spacer(Modifier.width(dims.itemSpacingLarge))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClickRow: (() -> Unit)? = null
) {
    val dims = Dimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (onClickRow != null) {
                    onClickRow()
                } else {
                    onCheckedChange(!checked)
                }
            }
            .padding(horizontal = dims.cardInnerPadding - 4.dp, vertical = dims.itemSpacingMedium + 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(dims.iconSizeMedium)
        )
        Spacer(Modifier.width(dims.itemSpacingLarge))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Theme Section – tabs + theme circles (inline inside card)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ThemeSectionInCard(
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Wallpaper colors", "Basic colors")
    val dims = Dimens.current

    Column(
        modifier = Modifier.padding(dims.cardInnerPadding - 4.dp),
        verticalArrangement = Arrangement.spacedBy(dims.itemSpacingLarge)
    ) {
        // Tab row
        Surface(
            shape = RoundedCornerShape(dims.buttonCornerRadius),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    val isActive = selectedTab == index
                    val bgColor by animateColorAsState(
                        targetValue = if (isActive)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            Color.Transparent,
                        animationSpec = tween(250),
                        label = "tabBg"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isActive)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(250),
                        label = "tabText"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(dims.buttonCornerRadius - 2.dp))
                            .background(bgColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { selectedTab = index }
                            .padding(vertical = dims.itemSpacingLarge - 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                            color = textColor
                        )
                    }
                }
            }
        }

        // Tab content
        when (selectedTab) {
            0 -> WallpaperColorsContent(selectedTheme, onThemeSelected)
            1 -> BasicColorsContent()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Wallpaper colors tab – shows the 4 theme circles with responsive spacing & sizes
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WallpaperColorsContent(
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit
) {
    val dims = Dimens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dims.themeCircleSpacing, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppTheme.entries.forEach { theme ->
            ThemeCircleOption(
                theme = theme,
                isSelected = selectedTheme == theme,
                onClick = { onThemeSelected(theme) },
                modifier = Modifier.weight(1f, fill = false)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Basic colors tab – empty placeholder
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BasicColorsContent() {
    val dims = Dimens.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(dims.themeCircleSize),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Coming soon",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Individual theme circle – split-circle preview + checkmark
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ThemeCircleOption(
    theme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dims = Dimens.current
    val colors = previewColorsFor(theme)
    val checkColor = MaterialTheme.colorScheme.onPrimary
    val checkBgColor = MaterialTheme.colorScheme.primary

    Surface(
        shape = RoundedCornerShape(dims.cardCornerRadius - 2.dp),
        color = if (isSelected)
            MaterialTheme.colorScheme.surfaceContainerHighest
        else
            MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = modifier
            .size(dims.themeCircleSize)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(dims.themeCircleCanvasSize)) {
                drawThemeCircle(colors)
            }
            if (isSelected) {
                Surface(
                    shape = CircleShape,
                    color = checkBgColor,
                    modifier = Modifier
                        .size(dims.themeCircleCheckSize)
                        .align(Alignment.Center)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Selected",
                        tint = checkColor,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Canvas helpers – 3-segment circle drawing
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Draws the 3-segment circle that mirrors the Android 12 wallpaper-color picker.
 *
 * Layout:
 * ┌──────────────┐
 * │     top      │
 * │  (primary)   │
 * ├──────┬───────┤
 * │  bL  │  bR   │
 * │(sec) │(tert) │
 * └──────┴───────┘
 */
private fun DrawScope.drawThemeCircle(colors: ThemePreviewColors) {
    val radius = size.minDimension / 2f
    val center = Offset(radius, radius)
    val gap = 1.5f

    val midX = size.width / 2f
    val midY = size.height / 2f

    drawArcSegment(
        color = colors.top,
        center = center,
        radius = radius,
        clipRect = Rect(0f, 0f, size.width, midY - gap)
    )
    drawArcSegment(
        color = colors.bottomLeft,
        center = center,
        radius = radius,
        clipRect = Rect(0f, midY + gap, midX - gap, size.height)
    )
    drawArcSegment(
        color = colors.bottomRight,
        center = center,
        radius = radius,
        clipRect = Rect(midX + gap, midY + gap, size.width, size.height)
    )
}

private fun DrawScope.drawArcSegment(
    color: Color,
    center: Offset,
    radius: Float,
    clipRect: Rect
) {
    val path = Path().apply {
        addOval(Rect(center = center, radius = radius))
    }
    drawContext.canvas.save()
    drawContext.canvas.clipRect(
        left = clipRect.left,
        top = clipRect.top,
        right = clipRect.right,
        bottom = clipRect.bottom
    )
    drawPath(path = path, color = color)
    drawContext.canvas.restore()
}
