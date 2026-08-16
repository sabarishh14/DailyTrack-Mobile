package com.example.dailytrack_mobile.presentation.screens.money

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MoneyVM : ViewModel() {

    private val _state = MutableStateFlow(MoneyState())
    val state: StateFlow<MoneyState> = _state.asStateFlow()

    fun onAction(action: MoneyAction) {
        when (action) {
            is MoneyAction.SelectTab -> _state.update { it.copy(selectedTab = action.index) }
            is MoneyAction.UpdateSearchQuery -> _state.update { it.copy(searchQuery = action.query) }
            is MoneyAction.SelectCategory -> _state.update { it.copy(selectedCategory = action.category) }
        }
    }
}
