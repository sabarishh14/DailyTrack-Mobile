package com.example.dailytrack_mobile.presentation.screens.money

sealed class MoneyAction {
    data class SelectTab(val index: Int) : MoneyAction()
    data class UpdateSearchQuery(val query: String) : MoneyAction()
    data class SelectCategory(val category: String) : MoneyAction()
}
