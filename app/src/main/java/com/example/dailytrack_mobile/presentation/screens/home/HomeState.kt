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
    val incomeByCategory: Map<String, Double> = emptyMap(),
    val expenseByCategory: Map<String, Double> = emptyMap(),
    val investmentTotalInvested: Double = 0.0,
    val investmentTotalCurrent: Double = 0.0
) {
    val totalBankBalance: Double
        get() = accounts
            .filter { it.balanceTracked }
            .sumOf { it.displayBalance }
}
