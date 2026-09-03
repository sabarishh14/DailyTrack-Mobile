package com.example.dailytrack_mobile.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailytrack_mobile.data.local.datastore.DemoModeManager
import com.example.dailytrack_mobile.data.repository.MoneyRepository
import com.example.dailytrack_mobile.data.repository.InvestmentsRepository
import com.example.dailytrack_mobile.presentation.screens.money.AccountInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeVM @Inject constructor(
    private val repository: MoneyRepository,
    private val investmentsRepository: InvestmentsRepository,
    private val demoModeManager: DemoModeManager
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            demoModeManager.isDemoModeEnabledFlow.collect {
                loadAccountsAndTransactions()
            }
        }
        viewModelScope.launch {
            repository.dataUpdateFlow.collect {
                loadAccountsAndTransactions()
            }
        }
    }

    private fun loadAccountsAndTransactions(forceRefresh: Boolean = false) {
        if (forceRefresh) {
            _state.update { it.copy(isRefreshing = true, errorMessage = null) }
        } else {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
        }
        viewModelScope.launch {
            val accountsResult = repository.getAccounts(forceRefresh = forceRefresh)
            val monthStr = String.format("%04d-%02d", _state.value.selectedYear, _state.value.selectedMonth.value)
            val transactionsResult = repository.getTransactions(limit = 1000, month = monthStr, forceRefresh = forceRefresh)
            val investmentsResult = investmentsRepository.getFullPortfolio(forceRefresh = forceRefresh)

            if (accountsResult.isSuccess && transactionsResult.isSuccess) {
                val accounts = accountsResult.getOrThrow().map { dto ->
                    AccountInfo(
                        account = dto.account,
                        balance = dto.balance,
                        realBalance = dto.realBalance,
                        balanceTracked = dto.balanceTracked
                    )
                }.sortedByDescending { it.realBalance != null }
                
                val transactions = transactionsResult.getOrThrow().transactions
                
                val income = mutableMapOf<String, Double>()
                val expense = mutableMapOf<String, Double>()
                
                transactions.forEach { t ->
                    if (t.type == "Credit") {
                        val key = if (t.account.isNotBlank()) t.account else "Uncategorized"
                        income[key] = (income[key] ?: 0.0) + t.amount
                    } else if (t.type == "Debit") {
                        val key = if (t.account.isNotBlank()) t.account else "Uncategorized"
                        expense[key] = (expense[key] ?: 0.0) + t.amount
                    }
                }
                
                val portfolioData = investmentsResult.getOrNull()
                val latestSnapshot = portfolioData?.snapshots?.firstOrNull()
                val totalInvested = latestSnapshot?.grandTotalInv ?: 0.0
                val totalCurrent = latestSnapshot?.grandTotalCurr ?: 0.0
                
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        accounts = accounts,
                        incomeByCategory = income,
                        expenseByCategory = expense,
                        investmentTotalInvested = totalInvested,
                        investmentTotalCurrent = totalCurrent
                    )
                }
            } else {
                val errorMessage = accountsResult.exceptionOrNull()?.message 
                    ?: transactionsResult.exceptionOrNull()?.message 
                    ?: "Failed to load data"
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = errorMessage
                    )
                }
            }
        }
    }

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.Refresh -> loadAccountsAndTransactions(forceRefresh = action.forceRefresh)
            is HomeAction.DateSelected -> {
                _state.update { it.copy(selectedMonth = action.month, selectedYear = action.year) }
                loadAccountsAndTransactions()
            }
        }
    }
}
