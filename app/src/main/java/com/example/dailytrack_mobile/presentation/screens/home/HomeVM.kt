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
        loadAccounts()
    }

    private fun loadAccounts() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            repository.getAccounts()
                .onSuccess { accounts ->
                    _state.update { current ->
                        current.copy(
                            isLoading = false,
                            accounts = accounts.map { dto ->
                                AccountInfo(
                                    account = dto.account,
                                    balance = dto.balance,
                                    realBalance = dto.realBalance,
                                    balanceTracked = dto.balanceTracked
                                )
                            }
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load accounts"
                        )
                    }
                }
        }
    }

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.Refresh -> loadAccounts()
        }
    }
}
