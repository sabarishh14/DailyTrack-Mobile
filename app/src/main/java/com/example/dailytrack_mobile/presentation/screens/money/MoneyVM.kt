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
                    analysisFilterState = action.filterState.copy(activeDatePreset = null),
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

            is MoneyAction.ToggleQuickPreset -> _state.update { current ->
                val filters = current.analysisFilterState
                when (action.preset) {
                    QuickFilterPreset.LAST_30_DAYS -> {
                        if (filters.activeDatePreset == QuickFilterPreset.LAST_30_DAYS) {
                            current.copy(
                                analysisFilterState = filters.copy(
                                    customDateRange = null,
                                    activeDatePreset = null
                                )
                            )
                        } else {
                            val now = System.currentTimeMillis()
                            val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)
                            current.copy(
                                analysisFilterState = filters.copy(
                                    customDateRange = Pair(thirtyDaysAgo, now),
                                    financialYear = null,
                                    activeDatePreset = QuickFilterPreset.LAST_30_DAYS
                                )
                            )
                        }
                    }
                    QuickFilterPreset.THIS_MONTH -> {
                        if (filters.activeDatePreset == QuickFilterPreset.THIS_MONTH) {
                            current.copy(
                                analysisFilterState = filters.copy(
                                    customDateRange = null,
                                    activeDatePreset = null
                                )
                            )
                        } else {
                            val calendar = java.util.Calendar.getInstance().apply {
                                set(java.util.Calendar.DAY_OF_MONTH, 1)
                                set(java.util.Calendar.HOUR_OF_DAY, 0)
                                set(java.util.Calendar.MINUTE, 0)
                                set(java.util.Calendar.SECOND, 0)
                                set(java.util.Calendar.MILLISECOND, 0)
                            }
                            val startOfMonth = calendar.timeInMillis
                            val now = System.currentTimeMillis()
                            current.copy(
                                analysisFilterState = filters.copy(
                                    customDateRange = Pair(startOfMonth, now),
                                    financialYear = null,
                                    activeDatePreset = QuickFilterPreset.THIS_MONTH
                                )
                            )
                        }
                    }
                    QuickFilterPreset.EXPENSES_ONLY -> {
                        val isExpensesOnly = filters.selectedTypes == setOf(TransactionType.DEBIT)
                        current.copy(
                            analysisFilterState = filters.copy(
                                selectedTypes = if (isExpensesOnly) emptySet() else setOf(TransactionType.DEBIT)
                            )
                        )
                    }
                }
            }

            is MoneyAction.ClearFinancialYearFilter -> _state.update { current ->
                current.copy(
                    analysisFilterState = current.analysisFilterState.copy(financialYear = null)
                )
            }

            is MoneyAction.ClearDateRangeFilter -> _state.update { current ->
                current.copy(
                    analysisFilterState = current.analysisFilterState.copy(
                        customDateRange = null,
                        activeDatePreset = null
                    )
                )
            }
        }
    }
}

