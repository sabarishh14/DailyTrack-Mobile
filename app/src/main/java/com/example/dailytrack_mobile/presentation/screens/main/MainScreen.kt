package com.example.dailytrack_mobile.presentation.screens.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailytrack_mobile.data.remote.dto.MediaSearchResultDto
import com.example.dailytrack_mobile.presentation.navigation.Routes
import com.example.dailytrack_mobile.presentation.navigation.components.BottomNavBar
import com.example.dailytrack_mobile.presentation.screens.activities.ActivitiesScreen
import com.example.dailytrack_mobile.presentation.screens.home.HomeScreen
import com.example.dailytrack_mobile.presentation.screens.invest.InvestmentsScreen
import com.example.dailytrack_mobile.presentation.screens.money.MoneyScreen
import com.example.dailytrack_mobile.presentation.screens.sabdekho.SabdekhoScreen
import com.example.dailytrack_mobile.presentation.screens.forms.*
import com.example.dailytrack_mobile.presentation.screens.main.components.AddActionSheet
import com.example.dailytrack_mobile.presentation.util.Dimens
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit
) {
    var currentRoute by remember { mutableStateOf(Routes.Home.route) }
    var showAddSheet by remember { mutableStateOf(false) }
    var isCurrentFormDirty by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var pendingRoute by remember { mutableStateOf<String?>(null) }
    var preselectedMediaForAddMovie by remember { mutableStateOf<MediaSearchResultDto?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val dims = Dimens.current

    val formRoutes = remember {
        setOf(
            Routes.AddMoney.route,
            Routes.AddActivity.route,
            Routes.AddMovie.route,
            Routes.AddAsset.route,
            Routes.AddInvestment.route,
            Routes.SyncBroker.route
        )
    }

    val isFormScreen = currentRoute in formRoutes

    // Centralized safe navigation that checks for unsaved changes
    fun navigateSafely(targetRoute: String) {
        if (currentRoute == targetRoute) return

        if (isFormScreen && isCurrentFormDirty) {
            pendingRoute = targetRoute
            showDiscardDialog = true
        } else {
            isCurrentFormDirty = false
            currentRoute = targetRoute
        }
    }

    // Handles form save completion
    fun onFormSaved(message: String, destinationRoute: String = Routes.Home.route) {
        isCurrentFormDirty = false
        preselectedMediaForAddMovie = null
        currentRoute = destinationRoute
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    // Intercept system/hardware back gesture when not on Home screen
    BackHandler(enabled = currentRoute != Routes.Home.route) {
        navigateSafely(Routes.Home.route)
    }

    val screenTitle = when (currentRoute) {
        Routes.Home.route -> "Home"
        Routes.Money.route -> "Money"
        Routes.Activities.route -> "Activities"
        Routes.Investments.route -> "Investments"
        Routes.Sabdekho.route -> "Sabdekho"
        Routes.AddMoney.route -> "Add Money"
        Routes.AddActivity.route -> "Add Activity"
        Routes.AddMovie.route -> "Add Movie"
        Routes.AddAsset.route -> "Add Asset"
        Routes.AddInvestment.route -> "Add Investment"
        Routes.SyncBroker.route -> "Syncing Broker"
        else -> "DailyTrack"
    }

    if (showAddSheet) {
        AddActionSheet(
            onActionSelected = { route ->
                if (route == Routes.AddMovie.route) {
                    preselectedMediaForAddMovie = null
                }
                navigateSafely(route)
            },
            onDismiss = {
                showAddSheet = false
            }
        )
    }

    // Unsaved changes confirmation dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = {
                showDiscardDialog = false
                pendingRoute = null
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(dims.iconSizeLarge)
                )
            },
            title = {
                Text(
                    text = "Discard Unsaved Details?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "The entered details are not saved and will be lost. Are you sure you want to go back?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardDialog = false
                        isCurrentFormDirty = false
                        preselectedMediaForAddMovie = null
                        val destination = pendingRoute ?: Routes.Home.route
                        pendingRoute = null
                        currentRoute = destination
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(dims.buttonCornerRadius)
                ) {
                    Text("Discard", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showDiscardDialog = false
                        pendingRoute = null
                    },
                    shape = RoundedCornerShape(dims.buttonCornerRadius)
                ) {
                    Text("Keep Editing", style = MaterialTheme.typography.labelLarge)
                }
            },
            shape = RoundedCornerShape(dims.cardCornerRadius),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    navigationIcon = {
                        if (currentRoute != Routes.Home.route) {
                            IconButton(onClick = { navigateSafely(Routes.Home.route) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Home",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(dims.iconSizeMedium)
                                )
                            }
                        }
                    },
                    title = {
                        Text(
                            text = screenTitle,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(dims.iconSizeMedium)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier.statusBarsPadding()
                )
                // Subtle divider between topbar and canvas
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            }
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = currentRoute,
                onNavigate = { targetRoute ->
                    navigateSafely(targetRoute)
                }
            )
        },
        floatingActionButton = {
            if (!isFormScreen) {
                FloatingActionButton(
                    onClick = { showAddSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        modifier = Modifier.size(dims.iconSizeLarge)
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = if (isFormScreen) dims.screenBottomPadding else dims.screenBottomPadding + 56.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentRoute) {
                Routes.Home.route -> HomeScreen()
                Routes.Money.route -> MoneyScreen()
                Routes.Activities.route -> ActivitiesScreen()
                Routes.Investments.route -> InvestmentsScreen()
                Routes.Sabdekho.route -> SabdekhoScreen(
                    onNavigateToAddMovie = { media ->
                        preselectedMediaForAddMovie = media
                        navigateSafely(Routes.AddMovie.route)
                    }
                )
                Routes.AddMoney.route -> AddMoneyScreen(
                    onDirtyStateChanged = { isCurrentFormDirty = it },
                    onSaveSuccess = { onFormSaved("Transaction saved successfully!", Routes.Money.route) }
                )
                Routes.AddActivity.route -> AddActivityScreen(
                    onDirtyStateChanged = { isCurrentFormDirty = it },
                    onSaveSuccess = { onFormSaved("Activity logged successfully!", Routes.Activities.route) }
                )
                Routes.AddMovie.route -> AddMovieScreen(
                    initialMedia = preselectedMediaForAddMovie,
                    onDirtyStateChanged = { isCurrentFormDirty = it },
                    onSaveSuccess = { onFormSaved("Title added successfully!", Routes.Sabdekho.route) }
                )
                Routes.AddAsset.route -> AddAssetScreen(
                    onDirtyStateChanged = { isCurrentFormDirty = it },
                    onSaveSuccess = { onFormSaved("Asset saved successfully!", Routes.Investments.route) }
                )
                Routes.AddInvestment.route -> AddInvestmentScreen(
                    onDirtyStateChanged = { isCurrentFormDirty = it },
                    onSaveSuccess = { onFormSaved("Investment recorded successfully!", Routes.Investments.route) }
                )
                Routes.SyncBroker.route -> SyncBrokerScreen(
                    onNavigateBack = { navigateSafely(Routes.Home.route) }
                )
            }
        }
    }
}
