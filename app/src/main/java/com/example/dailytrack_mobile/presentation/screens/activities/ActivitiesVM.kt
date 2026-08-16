package com.example.dailytrack_mobile.presentation.screens.activities

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ActivitiesVM : ViewModel() {

    private val _state = MutableStateFlow(ActivitiesState())
    val state = _state.asStateFlow()

    fun onAction(action: ActivitiesAction) {
        when (action) {
            is ActivitiesAction.OnMonthChanged -> {
                _state.update { it.copy(selectedMonth = action.month, selectedYear = action.year) }
                // TODO: Load data for the selected month/year from database when wired up
            }
        }
    }
}
