package com.example.dailytrack_mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dailytrack_mobile.data.local.datastore.ThemeManager
import com.example.dailytrack_mobile.presentation.screens.main.MainScreen
import com.example.dailytrack_mobile.presentation.screens.settings.SettingsAction
import com.example.dailytrack_mobile.presentation.screens.settings.SettingsScreen
import com.example.dailytrack_mobile.presentation.screens.settings.SettingsVM
import com.example.dailytrack_mobile.presentation.screens.settings.SettingsVMFactory
import com.example.dailytrack_mobile.presentation.theme.DailyTrackTheme
import com.example.dailytrack_mobile.presentation.util.ProvideAppDimensions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize ThemeManager manually for now
        val themeManager = ThemeManager(applicationContext)
        val viewModelFactory = SettingsVMFactory(themeManager)

        setContent {
            // Provide SettingsVM
            val settingsVM: SettingsVM = viewModel(factory = viewModelFactory)
            
            // Collect the settings state to get the currently selected theme
            val state by settingsVM.state.collectAsState()

            // Simple navigation state
            var currentScreen by rememberSaveable { mutableStateOf("Main") }

            // Pass the state's selected theme into the DailyTrackTheme
            DailyTrackTheme(appTheme = state.selectedTheme) {
                ProvideAppDimensions {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
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