package com.example.dailytrack_mobile.presentation.screens.home

import com.example.dailytrack_mobile.presentation.screens.money.AccountInfo

data class HomeState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val accounts: List<AccountInfo> = emptyList()
) {
    val totalBankBalance: Double
        get() = accounts
            .filter { it.balanceTracked }
            .sumOf { it.displayBalance }
}
