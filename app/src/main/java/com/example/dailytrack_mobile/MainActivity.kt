package com.example.dailytrack_mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.dailytrack_mobile.data.local.security.AppLockManager
import com.example.dailytrack_mobile.presentation.navigation.Routes
import com.example.dailytrack_mobile.presentation.screens.lock.AppLockScreen
import com.example.dailytrack_mobile.presentation.screens.main.MainScreen
import com.example.dailytrack_mobile.presentation.screens.settings.SettingsAction
import com.example.dailytrack_mobile.presentation.screens.settings.SettingsScreen
import com.example.dailytrack_mobile.presentation.screens.settings.SettingsVM
import com.example.dailytrack_mobile.presentation.theme.DailyTrackTheme
import com.example.dailytrack_mobile.presentation.util.ProvideAppDimensions
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var appLockManager: AppLockManager

    private var pendingDeepLinkRoute by mutableStateOf<String?>(null)

    private fun extractDeepLinkRoute(intent: Intent?): String? {
        val uri = intent?.data ?: return null
        return when {
            (uri.scheme == "dailytrack" && uri.host == "add_money") ||
            (uri.scheme == "myapp" && uri.host == "input_form") -> Routes.AddMoney.route
            else -> null
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val route = extractDeepLinkRoute(intent)
        if (route != null) {
            pendingDeepLinkRoute = route
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingDeepLinkRoute = extractDeepLinkRoute(intent)

        setContent {
            // Provide SettingsVM via Hilt
            val settingsVM: SettingsVM = hiltViewModel()
            
            // Collect the settings state to get the currently selected theme and app lock
            val state by settingsVM.state.collectAsState()

            // Simple navigation state
            var currentScreen by rememberSaveable { mutableStateOf("Main") }

            // Switch to Main screen if a deep link is received while viewing Settings
            LaunchedEffect(pendingDeepLinkRoute) {
                if (pendingDeepLinkRoute != null) {
                    currentScreen = "Main"
                }
            }

            // App Lock State
            var isAppLocked by rememberSaveable { mutableStateOf(false) }
            var hasInitializedLock by remember { mutableStateOf(false) }

            // Initial lock check when app lock is loaded
            LaunchedEffect(state.isAppLockEnabled) {
                if (!hasInitializedLock && state.isAppLockEnabled) {
                    isAppLocked = true
                    hasInitializedLock = true
                }
            }

            // Lock the app when it goes to background (ON_STOP)
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner, state.isAppLockEnabled) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP && state.isAppLockEnabled) {
                        isAppLocked = true
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            // Pass the state's theme configurations into DailyTrackTheme
            DailyTrackTheme(
                themeMode = state.themeMode,
                appTheme = state.selectedTheme,
                withAmoled = state.withAmoled
            ) {
                ProvideAppDimensions {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (isAppLocked && state.isAppLockEnabled) {
                            AppLockScreen(
                                appLockManager = appLockManager,
                                onUnlocked = { isAppLocked = false }
                            )
                        } else {
                            if (currentScreen == "Main") {
                                MainScreen(
                                    onNavigateToSettings = { currentScreen = "Settings" },
                                    targetRoute = pendingDeepLinkRoute,
                                    onRouteConsumed = { pendingDeepLinkRoute = null }
                                )
                            } else {
                                SettingsScreen(
                                    state = state,
                                    appLockManager = appLockManager,
                                    onAction = { action ->
                                        settingsVM.onAction(action)
                                        if (action is SettingsAction.OnBackClicked) {
                                            currentScreen = "Main"
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}