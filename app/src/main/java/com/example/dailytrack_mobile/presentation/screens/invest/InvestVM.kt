package com.example.dailytrack_mobile.presentation.screens.invest

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class InvestVM : ViewModel() {

    private val _state = MutableStateFlow(InvestState())
    val state: StateFlow<InvestState> = _state.asStateFlow()

    fun onAction(action: InvestAction) {
        when (action) {
            is InvestAction.SelectTab -> _state.update { it.copy(selectedTab = action.tab) }
        }
    }
}
