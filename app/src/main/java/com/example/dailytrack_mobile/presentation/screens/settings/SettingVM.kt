package com.example.dailytrack_mobile.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dailytrack_mobile.data.local.datastore.ThemeManager
import com.example.dailytrack_mobile.presentation.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsVM(private val themeManager: ThemeManager) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    init {
        // Listen for theme changes from DataStore on startup
        viewModelScope.launch {
            themeManager.themeFlow.collect { savedThemeName ->
                try {
                    _state.update { it.copy(selectedTheme = AppTheme.valueOf(savedThemeName)) }
                } catch (e: Exception) {
                    _state.update { it.copy(selectedTheme = AppTheme.YELLOW) }
                }
            }
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.OnThemeChanged -> {
                viewModelScope.launch {
                    themeManager.saveTheme(action.newTheme.name)
                }
            }
            is SettingsAction.OnBackClicked -> {
                // Navigation is hoisted to the Screen composable
            }
            is SettingsAction.OnAppLockToggled -> {
                _state.update { it.copy(isAppLockEnabled = action.enabled) }
                // TODO: persist app lock preference
            }
            is SettingsAction.OnHideBalancesToggled -> {
                _state.update { it.copy(isHideBalancesOnStartup = action.enabled) }
                // TODO: persist hide-balances preference
            }
            is SettingsAction.OnForceSyncClicked -> {
                // TODO: trigger sync logic
            }
            is SettingsAction.OnServerStatusClicked -> {
                // TODO: navigate to or show server status
            }
        }
    }
}

// Simple Factory for creating the ViewModel manually (since we aren't using Hilt for ThemeManager here)
class SettingsVMFactory(private val themeManager: ThemeManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsVM::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsVM(themeManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}