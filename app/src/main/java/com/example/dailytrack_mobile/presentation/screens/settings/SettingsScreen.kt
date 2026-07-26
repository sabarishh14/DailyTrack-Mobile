package com.example.dailytrack_mobile.presentation.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailytrack_mobile.presentation.theme.AppTheme
import com.example.dailytrack_mobile.presentation.theme.YellowThemeColors
import com.example.dailytrack_mobile.presentation.theme.GreenThemeColors
import com.example.dailytrack_mobile.presentation.theme.TealThemeColors
import com.example.dailytrack_mobile.presentation.theme.PurpleThemeColors

/**
 * Data class holding the 3 representative colors for each theme's circle preview.
 *  - top          : upper semicircle (primary)
 *  - bottomLeft   : lower-left quarter (secondary)
 *  - bottomRight  : lower-right quarter (tertiary)
 */
private data class ThemePreviewColors(
    val top: Color,
    val bottomLeft: Color,
    val bottomRight: Color
)

/** Maps each AppTheme to its 3 preview colors derived from the light color scheme. */
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { onAction(SettingsAction.OnBackClicked) }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ThemeSection(
                selectedTheme = state.selectedTheme,
                onThemeSelected = { newTheme ->
                    onAction(SettingsAction.OnThemeChanged(newTheme))
                }
            )

            HorizontalDivider()

            AboutSection(
                appVersion = state.appVersion,
                developerName = state.developerName
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Theme Section – "Wallpaper colors" / "Basic colors" tabs + theme circles
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ThemeSection(
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Wallpaper colors", "Basic colors")

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Tab row
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                            .clip(RoundedCornerShape(12.dp))
                            .background(bgColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { selectedTab = index }
                            .padding(vertical = 12.dp),
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
// Wallpaper colors tab – shows the 4 theme circles in a scrollable row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WallpaperColorsContent(
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        AppTheme.entries.forEach { theme ->
            ThemeCircleOption(
                theme = theme,
                isSelected = selectedTheme == theme,
                onClick = { onThemeSelected(theme) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Basic colors tab – empty placeholder
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BasicColorsContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
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
    onClick: () -> Unit
) {
    val colors = previewColorsFor(theme)
    val checkColor = MaterialTheme.colorScheme.onPrimary
    val checkBgColor = MaterialTheme.colorScheme.primary

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected)
            MaterialTheme.colorScheme.surfaceContainerHighest
        else
            MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = Modifier
            .size(80.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            // The 3-segment circle preview
            Canvas(modifier = Modifier.size(56.dp)) {
                drawThemeCircle(colors)
            }
            // Checkmark badge
            if (isSelected) {
                Surface(
                    shape = CircleShape,
                    color = checkBgColor,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Selected",
                        tint = checkColor,
                        modifier = Modifier
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Draws the 3-segment circle that mirrors the Android 12 wallpaper-color picker.
 *
 * Layout (conceptual):
 * ┌──────────────┐
 * │     top      │
 * │  (primary)   │
 * ├──────┬───────┤
 * │  bL  │  bR   │
 * │(sec) │(tert) │
 * └──────┴───────┘
 *
 * Top semicircle = primary, bottom-left quarter = secondary, bottom-right quarter = tertiary.
 */
private fun DrawScope.drawThemeCircle(colors: ThemePreviewColors) {
    val radius = size.minDimension / 2f
    val center = Offset(radius, radius)
    val gap = 1.5f // small gap between segments

    val midX = size.width / 2f
    val midY = size.height / 2f

    // Top semicircle (primary)
    drawArcSegment(
        color = colors.top,
        center = center,
        radius = radius,
        clipRect = Rect(0f, 0f, size.width, midY - gap)
    )

    // Bottom-left quarter (secondary)
    drawArcSegment(
        color = colors.bottomLeft,
        center = center,
        radius = radius,
        clipRect = Rect(0f, midY + gap, midX - gap, size.height)
    )

    // Bottom-right quarter (tertiary)
    drawArcSegment(
        color = colors.bottomRight,
        center = center,
        radius = radius,
        clipRect = Rect(midX + gap, midY + gap, size.width, size.height)
    )
}

/**
 * Draws a filled arc (portion of a circle) clipped to the given rectangle.
 */
private fun DrawScope.drawArcSegment(
    color: Color,
    center: Offset,
    radius: Float,
    clipRect: Rect
) {
    val path = Path().apply {
        // Build a full circle path
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

// ─────────────────────────────────────────────────────────────────────────────
// About Section (unchanged)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AboutSection(
    appVersion: String,
    developerName: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "About",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Version", style = MaterialTheme.typography.bodyLarge)
                    Text(appVersion, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Developer", style = MaterialTheme.typography.bodyLarge)
                    Text(developerName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
