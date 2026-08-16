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

            is MoneyAction.SetFilterSheetVisible -> _state.update {
                it.copy(isFilterSheetVisible = action.visible)
            }

            is MoneyAction.ApplyAnalysisFilters -> _state.update {
                it.copy(
                    analysisFilterState = action.filterState,
                    isFilterSheetVisible = false
                )
            }

            is MoneyAction.ResetAnalysisFilters -> _state.update {
                it.copy(analysisFilterState = AnalysisFilterState())
            }

            is MoneyAction.RemoveCategoryFilter -> _state.update { current ->
                val updatedCats = current.analysisFilterState.categoryFilters.toMutableMap().apply {
                    remove(action.category)
                }
                current.copy(
                    analysisFilterState = current.analysisFilterState.copy(categoryFilters = updatedCats)
                )
            }

            is MoneyAction.RemoveAccountFilter -> _state.update { current ->
                val updatedAccs = current.analysisFilterState.accountFilters.toMutableMap().apply {
                    remove(action.account)
                }
                current.copy(
                    analysisFilterState = current.analysisFilterState.copy(accountFilters = updatedAccs)
                )
            }

            is MoneyAction.RemoveTypeFilter -> _state.update { current ->
                val updatedTypes = current.analysisFilterState.selectedTypes.toMutableSet().apply {
                    remove(action.type)
                }
                current.copy(
                    analysisFilterState = current.analysisFilterState.copy(selectedTypes = updatedTypes)
                )
            }

            is MoneyAction.RemoveVisibilityFilter -> _state.update { current ->
                val updatedVis = current.analysisFilterState.selectedVisibilities.toMutableSet().apply {
                    remove(action.visibility)
                }
                current.copy(
                    analysisFilterState = current.analysisFilterState.copy(selectedVisibilities = updatedVis)
                )
            }

            is MoneyAction.ClearFinancialYearFilter -> _state.update { current ->
                current.copy(
                    analysisFilterState = current.analysisFilterState.copy(financialYear = null)
                )
            }

            is MoneyAction.ClearDateRangeFilter -> _state.update { current ->
                current.copy(
                    analysisFilterState = current.analysisFilterState.copy(customDateRange = null)
                )
            }
        }
    }
}

