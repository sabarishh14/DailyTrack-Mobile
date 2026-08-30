package com.example.dailytrack_mobile.presentation.screens.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailytrack_mobile.data.local.datastore.DemoModeManager
import com.example.dailytrack_mobile.data.repository.MoneyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MoneyVM @Inject constructor(
    private val repository: MoneyRepository,
    private val demoModeManager: DemoModeManager
) : ViewModel() {

    private val _state = MutableStateFlow(MoneyState())
    val state: StateFlow<MoneyState> = _state.asStateFlow()

    companion object {
        private const val PAGE_SIZE = 50
    }

    private var loadJob: kotlinx.coroutines.Job? = null
    private var progressiveFetchJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            demoModeManager.isDemoModeEnabledFlow.collect {
                loadInitialData()
            }
        }
        viewModelScope.launch {
            repository.dataUpdateFlow.collect {
                loadInitialData()
            }
        }
    }

    private fun loadInitialData() {
        loadJob?.cancel()
        progressiveFetchJob?.cancel()
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        loadJob = viewModelScope.launch {
            // Launch all three in parallel
            val accountsDeferred = async { repository.getAccounts() }
            val transactionsDeferred = async { repository.getTransactions(limit = PAGE_SIZE, offset = 0) }
            val categoriesDeferred = async { repository.getCategories() }

            val accountsResult = accountsDeferred.await()
            val transactionsResult = transactionsDeferred.await()
            val categoriesResult = categoriesDeferred.await()

            _state.update { current ->
                var updated = current.copy(isLoading = false)

                // Process accounts
                accountsResult.onSuccess { accounts ->
                    updated = updated.copy(
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

                // Process transactions
                transactionsResult.onSuccess { response ->
                    updated = updated.copy(
                        transactions = response.transactions.map { it.toDomain() },
                        currentOffset = response.offset + response.limit,
                        hasMore = response.hasMore,
                        totalTransactionCount = response.total
                    )
                }.onFailure { error ->
                    updated = updated.copy(errorMessage = error.message ?: "Failed to load transactions")
                }

                // Process categories
                categoriesResult.onSuccess { categories ->
                    updated = updated.copy(apiCategories = categories)
                }

                updated
            }

            // Fire off a background task to progressively fetch the REST of the transactions
            // for the spending analyser, exactly like the Web app does.
            if (_state.value.hasMore) {
                fetchAllTransactionsProgressively()
            }
        }
    }

    private fun fetchAllTransactionsProgressively() {
        progressiveFetchJob?.cancel()
        progressiveFetchJob = viewModelScope.launch {
            var hasMore = _state.value.hasMore
            var offset = _state.value.currentOffset

            while (hasMore) {
                val result = repository.getTransactions(limit = 500, offset = offset).getOrNull()
                if (result == null || result.transactions.isEmpty()) break

                _state.update { current ->
                    // Make sure not to add duplicates
                    val existingIds = current.transactions.map { it.id }.toSet()
                    val newTxs = result.transactions.map { it.toDomain() }.filter { !existingIds.contains(it.id) }
                    
                    current.copy(
                        transactions = current.transactions + newTxs,
                        currentOffset = result.offset + result.limit,
                        hasMore = result.hasMore,
                        totalTransactionCount = result.total
                    )
                }
                hasMore = result.hasMore
                offset += 500
            }
        }
    }

    private fun loadMoreTransactions() {
        // We now fetch all transactions progressively in the background on init,
        // so manual pagination scrolling is no longer needed.
    }

    fun onAction(action: MoneyAction) {
        when (action) {
            is MoneyAction.SelectTab -> _state.update { it.copy(selectedTab = action.index) }
            is MoneyAction.UpdateSearchQuery -> _state.update { it.copy(searchQuery = action.query) }
            is MoneyAction.SelectCategory -> _state.update { it.copy(selectedCategory = action.category) }

            is MoneyAction.Refresh -> loadInitialData()
            is MoneyAction.LoadMore -> loadMoreTransactions()

            is MoneyAction.SetFilterSheetVisible -> _state.update {
                it.copy(isFilterSheetVisible = action.visible)
            }

            is MoneyAction.ApplyAnalysisFilters -> _state.update {
                it.copy(
                    analysisFilterState = action.filterState.copy(activeDatePreset = null),
                    isFilterSheetVisible = false
                )
            }

            is MoneyAction.UpdateAnalysisFilterState -> _state.update {
                it.copy(analysisFilterState = action.filterState)
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
                    QuickFilterPreset.INCOME_ONLY -> {
                        val isIncomeOnly = filters.selectedTypes == setOf(TransactionType.CREDIT)
                        current.copy(
                            analysisFilterState = filters.copy(
                                selectedTypes = if (isIncomeOnly) emptySet() else setOf(TransactionType.CREDIT)
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

            is MoneyAction.ShowTransactionDetail -> _state.update {
                it.copy(detailTransaction = action.transaction, editingTransaction = null, deletingTransaction = null)
            }

            is MoneyAction.ShowEditDialog -> _state.update {
                it.copy(editingTransaction = action.transaction, detailTransaction = null, deletingTransaction = null)
            }

            is MoneyAction.ShowDeleteConfirmation -> _state.update {
                it.copy(deletingTransaction = action.transaction, detailTransaction = null)
            }

            is MoneyAction.DismissDialogs -> _state.update {
                it.copy(detailTransaction = null, editingTransaction = null, deletingTransaction = null)
            }

            is MoneyAction.ClearActionMessage -> _state.update {
                it.copy(actionMessage = null)
            }

            is MoneyAction.UpdateTransaction -> {
                viewModelScope.launch {
                    _state.update { it.copy(isUpdating = true) }
                    val result = repository.updateTransaction(
                        id = action.id,
                        type = action.type,
                        category = action.category,
                        amount = action.amount,
                        note = action.note,
                        accountName = action.accountName,
                        date = action.date,
                        excludeAnalytics = action.excludeAnalytics
                    )

                    result.onSuccess {
                        _state.update {
                            it.copy(
                                isUpdating = false,
                                editingTransaction = null,
                                actionMessage = "Transaction updated successfully"
                            )
                        }
                        loadInitialData()
                    }.onFailure { error ->
                        _state.update {
                            it.copy(
                                isUpdating = false,
                                errorMessage = error.message ?: "Failed to update transaction"
                            )
                        }
                    }
                }
            }

            is MoneyAction.DeleteTransaction -> {
                viewModelScope.launch {
                    _state.update { it.copy(isDeleting = true) }
                    val result = repository.deleteTransaction(action.id)

                    result.onSuccess {
                        _state.update {
                            it.copy(
                                isDeleting = false,
                                deletingTransaction = null,
                                editingTransaction = null,
                                actionMessage = "Transaction deleted successfully"
                            )
                        }
                        loadInitialData()
                    }.onFailure { error ->
                        _state.update {
                            it.copy(
                                isDeleting = false,
                                errorMessage = error.message ?: "Failed to delete transaction"
                            )
                        }
                    }
                }
            }

            is MoneyAction.ToggleExcludeAnalytics -> {
                viewModelScope.launch {
                    val tx = _state.value.transactions.find { it.id == action.id } ?: return@launch
                    val newExcluded = !action.currentExcluded
                    
                    // Optimistic update
                    _state.update { current ->
                        current.copy(
                            transactions = current.transactions.map {
                                if (it.id == action.id) it.copy(isExcluded = newExcluded) else it
                            }
                        )
                    }

                    val result = repository.updateTransaction(
                        id = tx.id,
                        type = tx.rawType.ifEmpty { if (tx.type == TransactionType.CREDIT) "Credit" else "Debit" },
                        category = tx.category,
                        amount = tx.amount,
                        note = tx.note,
                        accountName = tx.bank,
                        date = tx.rawDate.ifEmpty { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(tx.timestampMillis)) },
                        excludeAnalytics = newExcluded
                    )

                    result.onFailure {
                        // Revert on failure
                        _state.update { current ->
                            current.copy(
                                transactions = current.transactions.map {
                                    if (it.id == action.id) it.copy(isExcluded = action.currentExcluded) else it
                                },
                                errorMessage = "Failed to update Spending Analyser setting"
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DTO → Domain mapping
// ─────────────────────────────────────────────────────────────────────────────

private val apiDateParser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
private val displayDateFormat = SimpleDateFormat("MMM dd", Locale.US)

private fun com.example.dailytrack_mobile.data.remote.dto.TransactionDto.toDomain(): Transaction {
    val dateOnly = if (date.contains("T")) date.substringBefore("T") else date
    val parsedDate = try { apiDateParser.parse(dateOnly) } catch (_: Exception) { null }
    val displayDate = parsedDate?.let { displayDateFormat.format(it) } ?: dateOnly
    val timestampMs = parsedDate?.time ?: System.currentTimeMillis()

    val normalizedType = if (type.equals("Credit", ignoreCase = true) || type.equals("Income", ignoreCase = true)) "Credit" else "Debit"
    val txType = when (normalizedType) {
        "Credit" -> TransactionType.CREDIT
        else -> TransactionType.DEBIT
    }

    return Transaction(
        id = id,
        title = if (!description.isNullOrBlank()) description else heading,
        description = if (!description.isNullOrBlank()) heading else null,
        note = description,
        date = displayDate,
        bank = account,
        amount = amount,
        type = txType,
        category = heading,
        emoji = CategoryEmojis.forCategory(heading),
        isExcluded = excludeAnalytics,
        timestampMillis = timestampMs,
        monthStr = month,
        rawDate = dateOnly,
        rawType = normalizedType,
        split = split?.let { s ->
            SplitInfo(
                id = s.id,
                totalAmount = s.totalAmount,
                members = s.members.map { m ->
                    SplitMember(name = m.name, amount = m.amount, paid = m.paid)
                }
            )
        }
    )
}
