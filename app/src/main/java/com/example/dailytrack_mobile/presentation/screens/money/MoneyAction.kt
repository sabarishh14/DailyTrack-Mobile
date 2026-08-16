package com.example.dailytrack_mobile.presentation.screens.money

sealed class MoneyAction {
    data class SelectTab(val index: Int) : MoneyAction()
    data class UpdateSearchQuery(val query: String) : MoneyAction()
    data class SelectCategory(val category: String) : MoneyAction()

    // Filter Sheet actions
    data class SetFilterSheetVisible(val visible: Boolean) : MoneyAction()
    data class ApplyAnalysisFilters(val filterState: AnalysisFilterState) : MoneyAction()
    object ResetAnalysisFilters : MoneyAction()
    data class RemoveCategoryFilter(val category: String) : MoneyAction()
    data class RemoveAccountFilter(val account: String) : MoneyAction()
    data class RemoveTypeFilter(val type: TransactionType) : MoneyAction()
    data class ToggleQuickPreset(val preset: QuickFilterPreset) : MoneyAction()
    object ClearFinancialYearFilter : MoneyAction()
    object ClearDateRangeFilter : MoneyAction()
}

