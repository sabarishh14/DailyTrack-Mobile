package com.example.dailytrack_mobile

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dailytrack_mobile.data.local.datastore.DemoModeManager
import com.example.dailytrack_mobile.data.local.datastore.ThemeManager
import com.example.dailytrack_mobile.data.local.demo.DemoDataManager
import com.example.dailytrack_mobile.data.local.security.AppLockManager
import com.example.dailytrack_mobile.presentation.screens.lock.AppLockScreen
import com.example.dailytrack_mobile.presentation.screens.main.MainScreen
import com.example.dailytrack_mobile.presentation.screens.settings.SettingsAction
import com.example.dailytrack_mobile.presentation.screens.settings.SettingsScreen
import com.example.dailytrack_mobile.presentation.screens.settings.SettingsVM
import com.example.dailytrack_mobile.presentation.screens.settings.SettingsVMFactory
import com.example.dailytrack_mobile.presentation.theme.DailyTrackTheme
import com.example.dailytrack_mobile.presentation.util.ProvideAppDimensions
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize Managers manually
        val themeManager = ThemeManager(applicationContext)
        val appLockManager = AppLockManager(applicationContext)
        val demoModeManager = DemoModeManager(applicationContext)
        val demoDataManager = DemoDataManager(applicationContext, demoModeManager)
        val viewModelFactory = SettingsVMFactory(themeManager, appLockManager, demoModeManager, demoDataManager)

        setContent {
            // Provide SettingsVM
            val settingsVM: SettingsVM = viewModel(factory = viewModelFactory)
            
            // Collect the settings state to get the currently selected theme and app lock
            val state by settingsVM.state.collectAsState()

            // Simple navigation state
            var currentScreen by rememberSaveable { mutableStateOf("Main") }

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

            // Pass the state's selected theme into the DailyTrackTheme
            DailyTrackTheme(appTheme = state.selectedTheme) {
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
                                    onNavigateToSettings = { currentScreen = "Settings" }
                                )
                            } else {
                                SettingsScreen(
                                    state = state,
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