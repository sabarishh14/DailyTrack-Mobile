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
                    _state.update { it.copy(selectedTheme = AppTheme.BLUE) }
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
                // Handle navigation back (can be hoisted to the Screen composable)
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