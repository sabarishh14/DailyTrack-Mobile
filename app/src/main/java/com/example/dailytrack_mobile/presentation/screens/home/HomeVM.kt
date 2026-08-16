package com.example.dailytrack_mobile.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailytrack_mobile.data.repository.MoneyRepository
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
    private val repository: MoneyRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        loadAccountsAndTransactions()
    }

    private fun loadAccountsAndTransactions() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val accountsResult = repository.getAccounts()
            val monthStr = String.format("%04d-%02d", _state.value.selectedYear, _state.value.selectedMonth.value)
            val transactionsResult = repository.getTransactions(limit = 1000, month = monthStr)

            if (accountsResult.isSuccess && transactionsResult.isSuccess) {
                val accounts = accountsResult.getOrThrow().map { dto ->
                    AccountInfo(
                        account = dto.account,
                        balance = dto.balance,
                        realBalance = dto.realBalance,
                        balanceTracked = dto.balanceTracked
                    )
                }
                
                val transactions = transactionsResult.getOrThrow().transactions
                
                val income = mutableMapOf<String, Double>()
                val expense = mutableMapOf<String, Double>()
                
                accounts.forEach { 
                    income[it.account] = 0.0
                    expense[it.account] = 0.0
                }
                
                transactions.forEach { t ->
                    if (t.type == "Credit") {
                        income[t.account] = (income[t.account] ?: 0.0) + t.amount
                    } else if (t.type == "Debit") {
                        expense[t.account] = (expense[t.account] ?: 0.0) + t.amount
                    }
                }
                
                _state.update {
                    it.copy(
                        isLoading = false,
                        accounts = accounts,
                        incomeByAccount = income,
                        expenseByAccount = expense
                    )
                }
            } else {
                val errorMessage = accountsResult.exceptionOrNull()?.message 
                    ?: transactionsResult.exceptionOrNull()?.message 
                    ?: "Failed to load data"
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = errorMessage
                    )
                }
            }
        }
    }

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.Refresh -> loadAccountsAndTransactions()
            is HomeAction.DateSelected -> {
                _state.update { it.copy(selectedMonth = action.month, selectedYear = action.year) }
                loadAccountsAndTransactions()
            }
        }
    }
}
