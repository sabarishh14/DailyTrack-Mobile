package com.example.dailytrack_mobile.presentation.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentActivity
import com.example.dailytrack_mobile.data.local.security.AppLockManager
import com.example.dailytrack_mobile.data.local.security.LockType
import com.example.dailytrack_mobile.presentation.screens.lock.components.PinVerifyDialog
import com.example.dailytrack_mobile.presentation.theme.AppTheme
import com.example.dailytrack_mobile.presentation.theme.DtOgThemeColors
import com.example.dailytrack_mobile.presentation.theme.GreenThemeColors
import com.example.dailytrack_mobile.presentation.theme.JuneOledThemeColors
import com.example.dailytrack_mobile.presentation.theme.PurpleThemeColors
import com.example.dailytrack_mobile.presentation.theme.TealThemeColors
import com.example.dailytrack_mobile.presentation.theme.ThemeMode
import com.example.dailytrack_mobile.presentation.theme.YellowThemeColors
import com.example.dailytrack_mobile.presentation.util.BiometricHelper
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
    AppTheme.JUNE_OLED -> ThemePreviewColors(
        top = JuneOledThemeColors.lightScheme.primaryContainer,
        bottomLeft = JuneOledThemeColors.lightScheme.secondaryContainer,
        bottomRight = JuneOledThemeColors.lightScheme.tertiaryContainer
    )
    AppTheme.DT_OG -> ThemePreviewColors(
        top = Color(0xFF6366F1),
        bottomLeft = Color(0xFF06B6D4),
        bottomRight = Color(0xFF8B5CF6)
    )
}


private data class SettingsCategoryItem(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val keywords: List<String>
)

// ─────────────────────────────────────────────────────────────────────────────
// Main Settings Screen – Root with sub-screen routing
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    appLockManager: AppLockManager? = null,
    onAction: (SettingsAction) -> Unit
) {
    val context = LocalContext.current
    val effectiveLockManager = appLockManager ?: remember { AppLockManager(context) }
    var currentSubScreen by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val categories = remember(state.isAppLockEnabled, state.appVersion) {
        listOf(
            SettingsCategoryItem(
                id = "General",
                icon = Icons.Default.Tune,
                title = "General",
                subtitle = "Demo mode, sample data, preferences",
                keywords = listOf("demo", "sample", "test", "data", "reset", "general", "preferences")
            ),
            SettingsCategoryItem(
                id = "Reminders",
                icon = Icons.Default.Alarm,
                title = "Reminders",
                subtitle = "Daily tracking reminders & schedule",
                keywords = listOf("reminder", "reminders", "schedule", "scheduler", "notification", "notifications", "alarm", "daily", "alert", "time")
            ),
            SettingsCategoryItem(
                id = "Appearance",
                icon = Icons.Default.Palette,
                title = "Appearance",
                subtitle = "Theme, dark mode, colors",
                keywords = listOf("theme", "dark", "light", "amoled", "oled", "true black", "color", "wallpaper", "appearance", "palette")
            ),
            SettingsCategoryItem(
                id = "PrivacySecurity",
                icon = Icons.Default.Lock,
                title = "Privacy & Security",
                subtitle = if (state.isAppLockEnabled) "App lock enabled" else "App lock disabled",
                keywords = listOf("lock", "pin", "fingerprint", "biometric", "security", "privacy", "password", "passcode")
            ),
            SettingsCategoryItem(
                id = "Sync",
                icon = Icons.Default.Sync,
                title = "Sync",
                subtitle = "Force sync, server status",
                keywords = listOf("sync", "server", "cloud", "api", "refresh", "status", "hydrate")
            ),
            SettingsCategoryItem(
                id = "About",
                icon = Icons.Default.Info,
                title = "About",
                subtitle = state.appVersion,
                keywords = listOf("about", "version", "developer", "info", "app")
            )
        )
    }

    val filteredCategories = remember(searchQuery, categories) {
        if (searchQuery.isBlank()) {
            categories
        } else {
            val q = searchQuery.trim()
            categories.filter { cat ->
                cat.title.contains(q, ignoreCase = true) ||
                cat.subtitle.contains(q, ignoreCase = true) ||
                cat.keywords.any { it.contains(q, ignoreCase = true) }
            }
        }
    }

    // Material 3 Loading Dialog during Force Sync (global overlay)
    if (state.isSyncing) {
        Dialog(
            onDismissRequest = { /* Non-dismissible while in-flight */ },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Material 3 Expressive LoadingIndicator
                    LoadingIndicator(
                        modifier = Modifier.size(56.dp)
                    )

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = "Syncing All Pages",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = state.syncStepDescription ?: "Re-hydrating accounts, transactions, investments, activities, and media...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(20.dp))

                    // Material 3 Linear progress indicator for continuous visual rhythm
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                }
            }
        }
    }

    // ── Sub-screen routing ───────────────────────────────────────────────────
    when (currentSubScreen) {
        "AppLockSettings" -> {
            AppLockSettingsScreen(
                state = state,
                onAction = onAction,
                onNavigateBack = { currentSubScreen = "PrivacySecurity" }
            )
            return
        }
        "General" -> {
            GeneralSettingsSubScreen(
                state = state,
                appLockManager = effectiveLockManager,
                onAction = onAction,
                onNavigateBack = { currentSubScreen = null }
            )
            return
        }
        "Reminders" -> {
            RemindersSettingsScreen(
                state = state,
                onAction = onAction,
                onNavigateBack = { currentSubScreen = null }
            )
            return
        }
        "Appearance" -> {
            AppearanceSettingsSubScreen(
                state = state,
                onAction = onAction,
                onNavigateBack = { currentSubScreen = null }
            )
            return
        }
        "PrivacySecurity" -> {
            PrivacySecuritySettingsSubScreen(
                state = state,
                onAction = onAction,
                onNavigateToAppLock = { currentSubScreen = "AppLockSettings" },
                onNavigateBack = { currentSubScreen = null }
            )
            return
        }
        "Sync" -> {
            SyncSettingsSubScreen(
                state = state,
                onAction = onAction,
                onNavigateBack = { currentSubScreen = null }
            )
            return
        }
        "About" -> {
            AboutSettingsSubScreen(
                state = state,
                onNavigateBack = { currentSubScreen = null }
            )
            return
        }
    }

    // ── Root settings screen ─────────────────────────────────────────────────
    BackHandler {
        onAction(SettingsAction.OnBackClicked)
    }

    val dims = Dimens.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = dims.screenHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
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
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(dims.iconSizeSmall + 4.dp)
                                )
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(dims.cardCornerRadius),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dims.searchBarHeight)
                )
            }


            // ── Settings Categories ─────────────────────────────────────────
            item {
                if (filteredCategories.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No settings match \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        filteredCategories.forEach { category ->
                            SettingsCard {
                                SettingsNavItem(
                                    icon = category.icon,
                                    title = category.title,
                                    subtitle = category.subtitle,
                                    onClick = { currentSubScreen = category.id }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// General Settings Sub-Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GeneralSettingsSubScreen(
    state: SettingsState,
    appLockManager: AppLockManager,
    onAction: (SettingsAction) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var showPinVerifyForDemoMode by remember { mutableStateOf(false) }

    if (showPinVerifyForDemoMode) {
        PinVerifyDialog(
            appLockManager = appLockManager,
            title = "Disable Demo Mode",
            subtitle = "Enter your 4-digit PIN to switch to live data",
            showBiometricOption = state.isBiometricWithPinEnabled,
            onSuccess = {
                showPinVerifyForDemoMode = false
                onAction(SettingsAction.OnDemoModeToggled(false))
            },
            onDismiss = { showPinVerifyForDemoMode = false }
        )
    }

    BackHandler { onNavigateBack() }
    val dims = Dimens.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = "General",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(dims.iconSizeMedium)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = dims.screenHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = dims.screenBottomPadding)
        ) {
            // ── Data Preferences ────────────────────────────────────────────
            item {
                SettingsSectionLabel("Data Preferences")
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsCard {
                        SettingsToggleItem(
                            icon = Icons.Default.Science,
                            title = "Demo Mode",
                            subtitle = "Hydrates screens with realistic sample data stored locally",
                            checked = state.isDemoModeEnabled,
                            onCheckedChange = { requestedState ->
                                if (!requestedState) {
                                    // Turning Demo Mode OFF → verify PIN / Biometric if App Lock is enabled
                                    if (state.isAppLockEnabled) {
                                        if (state.lockType == LockType.SYSTEM) {
                                            val activity = context as? FragmentActivity
                                            if (activity != null) {
                                                BiometricHelper.showBiometricPrompt(
                                                    activity = activity,
                                                    title = "Disable Demo Mode",
                                                    subtitle = "Verify your identity to switch to live data",
                                                    allowDeviceCredential = true,
                                                    onSuccess = {
                                                        onAction(SettingsAction.OnDemoModeToggled(false))
                                                    }
                                                )
                                            } else {
                                                onAction(SettingsAction.OnDemoModeToggled(false))
                                            }
                                        } else {
                                            // LockType.PIN → Show PIN verification dialog (with biometric fallback if enabled)
                                            showPinVerifyForDemoMode = true
                                        }
                                    } else {
                                        // App Lock not enabled → directly turn off
                                        onAction(SettingsAction.OnDemoModeToggled(false))
                                    }
                                } else {
                                    // Turning Demo Mode ON → directly turn on
                                    onAction(SettingsAction.OnDemoModeToggled(true))
                                }
                            }
                        )
                    }
                    if (state.isDemoModeEnabled) {
                        SettingsCard {
                            SettingsClickItem(
                                icon = Icons.Default.RestartAlt,
                                title = "Reset Demo Data",
                                subtitle = "Restore default sample transactions & portfolio",
                                onClick = { onAction(SettingsAction.OnResetDemoDataClicked) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Appearance Settings Sub-Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceSettingsSubScreen(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }
    val dims = Dimens.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val isDarkTheme = when (state.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = "Appearance",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(dims.iconSizeMedium)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = dims.screenHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = dims.screenBottomPadding)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsCard {
                        ThemeModeSelector(
                            currentMode = state.themeMode,
                            onModeSelected = { onAction(SettingsAction.OnThemeModeChanged(it)) }
                        )
                    }

                    AnimatedVisibility(
                        visible = isDarkTheme,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        SettingsCard {
                            SettingsToggleItem(
                                icon = Icons.Default.DarkMode,
                                title = "True black (OLED)",
                                subtitle = "Pure black background for dark theme",
                                checked = state.withAmoled,
                                onCheckedChange = { onAction(SettingsAction.OnAmoledToggled(it)) }
                            )
                        }
                    }

                    SettingsCard {
                        ThemeSectionInCard(
                            selectedTheme = state.selectedTheme,
                            onThemeSelected = { onAction(SettingsAction.OnThemeChanged(it)) }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Privacy & Security Settings Sub-Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrivacySecuritySettingsSubScreen(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    onNavigateToAppLock: () -> Unit,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }
    val dims = Dimens.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = "Privacy & Security",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(dims.iconSizeMedium)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = dims.screenHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = dims.screenBottomPadding)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsCard {
                        SettingsToggleItem(
                            icon = Icons.Default.Lock,
                            title = "App Lock",
                            subtitle = if (state.isAppLockEnabled) {
                                if (state.lockType == LockType.SYSTEM)
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
                                    onNavigateToAppLock()
                                }
                            },
                            onClickRow = {
                                onNavigateToAppLock()
                            }
                        )
                    }
                    if (state.isAppLockEnabled) {
                        SettingsCard {
                            SettingsNavItem(
                                icon = Icons.Default.Security,
                                title = "Lock options",
                                subtitle = "Change lock method or PIN",
                                onClick = { onNavigateToAppLock() }
                            )
                        }
                    }
                }
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// Sync Settings Sub-Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SyncSettingsSubScreen(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }
    val dims = Dimens.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = "Sync",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(dims.iconSizeMedium)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = dims.screenHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = dims.screenBottomPadding)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val syncSubtitle = when {
                        state.isSyncing -> "Syncing all pages..."
                        state.syncStatusMessage != null -> state.syncStatusMessage
                        else -> "Re-hydrate all pages by calling APIs again"
                    }
                    val syncSubtitleColor = when {
                        state.isSyncing -> MaterialTheme.colorScheme.primary
                        state.isLastSyncSuccess == true -> Color(0xFF2ECC71)
                        state.isLastSyncSuccess == false -> MaterialTheme.colorScheme.error
                        else -> null
                    }
                    SettingsCard {
                        SettingsClickItem(
                            icon = Icons.Default.Sync,
                            title = "Force Sync",
                            subtitle = syncSubtitle,
                            subtitleColor = syncSubtitleColor,
                            enabled = !state.isSyncing,
                            trailing = {
                                if (state.isSyncing) {
                                    LoadingIndicator(
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            },
                            onClick = { onAction(SettingsAction.OnForceSyncClicked) }
                        )
                    }
                    SettingsCard {
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
}

// ─────────────────────────────────────────────────────────────────────────────
// About Settings Sub-Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutSettingsSubScreen(
    state: SettingsState,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }
    val dims = Dimens.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(dims.iconSizeMedium)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = dims.screenHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = dims.screenBottomPadding)
        ) {
            item {
                AboutHeader(
                    appVersion = state.appVersion
                )
            }

            item {
                // Developer Card
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 82.dp)
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar on the left
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Developer: ${state.developerName}",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(18.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.developerName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 17.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Developer . Owner . Ideator",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
    appVersion: String
) {
    val dims = Dimens.current
    Card(
        shape = RoundedCornerShape(dims.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dims.cardCornerRadius))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 0.dp,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "App Icon",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(Modifier.width(18.dp))

            // App name + version
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DailyTrack",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = appVersion,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val dims = Dimens.current
    Card(
        shape = RoundedCornerShape(dims.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dims.cardCornerRadius))
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
            .defaultMinSize(minHeight = 82.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
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
    subtitleColor: Color? = null,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val dims = Dimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 82.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 0.12f else 0.05f),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp
                ),
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                    color = subtitleColor ?: MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(dims.itemSpacingSmall))
            trailing()
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
            .defaultMinSize(minHeight = 82.dp)
            .clickable {
                if (onClickRow != null) {
                    onClickRow()
                } else {
                    onCheckedChange(!checked)
                }
            }
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Theme Mode Selector – System / Light / Dark segmented controls
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ThemeModeSelector(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit
) {
    val dims = Dimens.current
    val modes = listOf(
        Triple(ThemeMode.SYSTEM, "System", Icons.Default.PhoneAndroid),
        Triple(ThemeMode.LIGHT, "Light", Icons.Default.LightMode),
        Triple(ThemeMode.DARK, "Dark", Icons.Default.DarkMode)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Spacer(Modifier.width(18.dp))
            Column {
                Text(
                    text = "App Theme",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = when (currentMode) {
                        ThemeMode.SYSTEM -> "Follow system settings"
                        ThemeMode.LIGHT -> "Always light"
                        ThemeMode.DARK -> "Always dark"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

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
                modes.forEach { (mode, title, icon) ->
                    val isSelected = currentMode == mode
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            Color.Transparent,
                        animationSpec = tween(250),
                        label = "modeBg"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(250),
                        label = "modeContent"
                    )

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(dims.buttonCornerRadius - 2.dp))
                            .background(bgColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onModeSelected(mode) }
                            .padding(vertical = dims.itemSpacingMedium + 2.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = contentColor
                        )
                    }
                }
            }
        }
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
    val moreThemes = remember { listOf(AppTheme.DT_OG) }
    var selectedTab by remember {
        mutableIntStateOf(if (selectedTheme in moreThemes) 1 else 0)
    }
    val tabs = listOf("Wallpaper colors", "More themes")
    val dims = Dimens.current

    Column(
        modifier = Modifier.padding(20.dp),
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
                            .padding(vertical = 14.dp),
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
            1 -> MoreThemesContent(selectedTheme, onThemeSelected)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Wallpaper colors tab – shows the theme circles with responsive spacing & sizes
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WallpaperColorsContent(
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit
) {
    val dims = Dimens.current
    val wallpaperThemes = listOf(
        AppTheme.YELLOW,
        AppTheme.GREEN,
        AppTheme.TEAL,
        AppTheme.PURPLE,
        AppTheme.JUNE_OLED
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(dims.themeCircleSpacing, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        wallpaperThemes.forEach { theme ->
            ThemeCircleOption(
                theme = theme,
                isSelected = selectedTheme == theme,
                onClick = { onThemeSelected(theme) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// More themes tab – shows custom/additional themes like DT_OG
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MoreThemesContent(
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit
) {
    val dims = Dimens.current
    val moreThemes = listOf(AppTheme.DT_OG)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(dims.themeCircleSpacing, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        moreThemes.forEach { theme ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ThemeCircleOption(
                    theme = theme,
                    isSelected = selectedTheme == theme,
                    onClick = { onThemeSelected(theme) }
                )
                Text(
                    text = "DT_OG",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (selectedTheme == theme) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selectedTheme == theme)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
