package com.example.dailytrack_mobile.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dailytrack_mobile.data.local.datastore.DemoModeManager
import com.example.dailytrack_mobile.data.local.datastore.ThemeManager
import com.example.dailytrack_mobile.data.local.demo.DemoDataManager
import com.example.dailytrack_mobile.data.local.security.AppLockManager
import com.example.dailytrack_mobile.presentation.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsVM(
    private val themeManager: ThemeManager,
    private val appLockManager: AppLockManager,
    private val demoModeManager: DemoModeManager,
    private val demoDataManager: DemoDataManager
) : ViewModel() {

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

        // Listen for app lock settings changes
        viewModelScope.launch {
            appLockManager.isAppLockEnabledFlow.collect { enabled ->
                _state.update { it.copy(isAppLockEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            appLockManager.lockTypeFlow.collect { lockType ->
                _state.update { it.copy(lockType = lockType) }
            }
        }

        viewModelScope.launch {
            appLockManager.isBiometricWithPinEnabledFlow.collect { enabled ->
                _state.update { it.copy(isBiometricWithPinEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            appLockManager.hasCustomPinFlow.collect { hasPin ->
                _state.update { it.copy(hasCustomPin = hasPin) }
            }
        }

        // Listen for Demo Mode changes
        viewModelScope.launch {
            demoModeManager.isDemoModeEnabledFlow.collect { enabled ->
                _state.update { it.copy(isDemoModeEnabled = enabled) }
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
                viewModelScope.launch {
                    appLockManager.setAppLockEnabled(action.enabled)
                }
            }
            is SettingsAction.OnLockTypeSelected -> {
                viewModelScope.launch {
                    appLockManager.setLockType(action.lockType)
                }
            }
            is SettingsAction.OnSaveCustomPin -> {
                viewModelScope.launch {
                    appLockManager.savePin(action.pin)
                    appLockManager.setAppLockEnabled(true)
                }
            }
            is SettingsAction.OnBiometricWithPinToggled -> {
                viewModelScope.launch {
                    appLockManager.setBiometricWithPinEnabled(action.enabled)
                }
            }
            is SettingsAction.OnDemoModeToggled -> {
                viewModelScope.launch {
                    demoModeManager.setDemoModeEnabled(action.enabled)
                }
            }
            is SettingsAction.OnResetDemoDataClicked -> {
                viewModelScope.launch {
                    demoDataManager.resetDemoData()
                }
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

class SettingsVMFactory(
    private val themeManager: ThemeManager,
    private val appLockManager: AppLockManager,
    private val demoModeManager: DemoModeManager,
    private val demoDataManager: DemoDataManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsVM::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsVM(themeManager, appLockManager, demoModeManager, demoDataManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}