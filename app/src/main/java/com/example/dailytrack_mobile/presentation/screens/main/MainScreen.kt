package com.example.dailytrack_mobile.presentation.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit
) {
    var currentRoute by remember { mutableStateOf(Routes.Home.route) }
    var showAddSheet by remember { mutableStateOf(false) }
    val dims = Dimens.current

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
        Routes.SyncBroker.route -> "Syncing"
        else -> "DailyTrack"
    }

    if (showAddSheet) {
        AddActionSheet(
            onActionSelected = { route ->
                currentRoute = route
            },
            onDismiss = {
                showAddSheet = false
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = screenTitle,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
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
                onNavigate = { route -> currentRoute = route }
            )
        },
        floatingActionButton = {
            val isFormScreen = currentRoute in listOf(
                Routes.AddMoney.route,
                Routes.AddActivity.route,
                Routes.AddMovie.route,
                Routes.AddAsset.route,
                Routes.AddInvestment.route,
                Routes.SyncBroker.route
            )
            if (!isFormScreen) {
                FloatingActionButton(
                    onClick = { showAddSheet = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        modifier = Modifier.size(dims.iconSizeLarge)
                    )
                }
            }
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
                Routes.Sabdekho.route -> SabdekhoScreen()
                Routes.AddMoney.route -> AddMoneyScreen()
                Routes.AddActivity.route -> AddActivityScreen()
                Routes.AddMovie.route -> AddMovieScreen()
                Routes.AddAsset.route -> AddAssetScreen()
                Routes.AddInvestment.route -> AddInvestmentScreen()
                Routes.SyncBroker.route -> SyncBrokerScreen()
            }
        }
    }
}
