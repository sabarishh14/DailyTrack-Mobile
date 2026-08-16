package com.example.dailytrack_mobile.presentation.screens.home

import com.example.dailytrack_mobile.presentation.screens.money.AccountInfo
import java.time.Month
import java.time.LocalDate

data class HomeState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val accounts: List<AccountInfo> = emptyList(),
    val selectedMonth: Month = LocalDate.now().month,
    val selectedYear: Int = LocalDate.now().year,
    val incomeByAccount: Map<String, Double> = emptyMap(),
    val expenseByAccount: Map<String, Double> = emptyMap()
) {
    val totalBankBalance: Double
        get() = accounts
            .filter { it.balanceTracked }
            .sumOf { it.displayBalance }
}
