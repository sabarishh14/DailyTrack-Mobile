package com.example.dailytrack_mobile.presentation.screens.money

import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.time.Month
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// Data models
// ─────────────────────────────────────────────────────────────────────────────

enum class TransactionType(val displayName: String) {
    DEBIT("Debit"),
    CREDIT("Credit"),
    SAVINGS("Savings"),
    INVESTMENT("Investment")
}

enum class ItemFilterStatus {
    NEUTRAL,
    INCLUDED,
    EXCLUDED
}

enum class FilterMode {
    INCLUDE,
    EXCLUDE
}

enum class QuickFilterPreset {
    THIS_MONTH,
    LAST_MONTH,
    EXPENSES_ONLY,
    INCOME_ONLY
}

val DEFAULT_CANONICAL_ACCOUNTS = listOf(
    "Cash", "KOTAK", "IDBI", "FEDERAL", "CUB", "INDIAN", "ICICI", "HDFC", "SBI", "Axis", "CC-PINNACLE 6360"
)

fun sortAccountsCanonical(accounts: List<String>): List<String> {
    return accounts.sortedWith(
        compareBy(
            { account ->
                val index = DEFAULT_CANONICAL_ACCOUNTS.indexOfFirst { it.trim().equals(account.trim(), ignoreCase = true) }
                if (index == -1) Int.MAX_VALUE else index
            },
            { it.lowercase() }
        )
    )
}

data class SplitMember(
    val name: String,
    val amount: Double,
    val paid: Boolean
)

data class SplitInfo(
    val id: Long,
    val totalAmount: Double,
    val members: List<SplitMember>
)

data class AccountInfo(
    val account: String,
    val balance: Double,
    val realBalance: Double?,
    val balanceTracked: Boolean
) {
    /** Returns the verified balance if available, otherwise the ledger balance */
    val displayBalance: Double get() = realBalance ?: balance
}

data class Transaction(
    val id: Long = 0L,
    val title: String,
    val description: String? = null,
    val note: String? = null,
    val date: String,
    val bank: String,
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val emoji: String,
    val isExcluded: Boolean = false,
    val timestampMillis: Long = System.currentTimeMillis(),
    val monthStr: String = "",
    val rawDate: String = "",
    val rawType: String = "",
    val split: SplitInfo? = null
) {
    val isSavings: Boolean
        get() = type == TransactionType.SAVINGS ||
                rawType.equals("Savings", ignoreCase = true) ||
                rawType.equals("Saving", ignoreCase = true) ||
                category.equals("Savings", ignoreCase = true) ||
                category.equals("Saving", ignoreCase = true)

    val isInvestment: Boolean
        get() = type == TransactionType.INVESTMENT ||
                rawType.equals("Investment", ignoreCase = true) ||
                rawType.equals("Investments", ignoreCase = true) ||
                category.equals("Investment", ignoreCase = true) ||
                category.equals("Investments", ignoreCase = true)
}

data class SpendingCategory(
    val name: String,
    val amount: Double,
    val color: Color
)

// ─────────────────────────────────────────────────────────────────────────────
// Filter State
// ─────────────────────────────────────────────────────────────────────────────

data class AnalysisFilterState(
    // Standard Binary Filters
    val selectedTypes: Set<TransactionType> = emptySet(), // Empty means neutral (all included)

    // Complex Filters (Category & Account Include/Exclude)
    val categoryFilters: Map<String, ItemFilterStatus> = emptyMap(),
    val accountFilters: Map<String, ItemFilterStatus> = emptyMap(),

    // Date & Time
    val financialYear: String? = null, // e.g. "FY 2025-26"
    val customDateRange: Pair<Long?, Long?>? = null, // Pair of Start & End epoch millis
    val selectedMonth: Month? = null,
    val selectedYear: Int? = null,
    val activeDatePreset: QuickFilterPreset? = null
) {
    val activeFilterCount: Int
        get() {
            var count = 0
            if (selectedTypes.isNotEmpty()) count += selectedTypes.size
            count += categoryFilters.count { it.value != ItemFilterStatus.NEUTRAL }
            count += accountFilters.count { it.value != ItemFilterStatus.NEUTRAL }
            if (!financialYear.isNullOrBlank() && financialYear != "All Time") count += 1
            if (customDateRange != null && (customDateRange.first != null || customDateRange.second != null)) count += 1
            return count
        }

    val hasActiveFilters: Boolean
        get() = activeFilterCount > 0

    fun formattedDateRange(): String? {
        if (selectedMonth != null && selectedYear != null) {
            val monthName = selectedMonth.getDisplayName(TextStyle.FULL, Locale.getDefault())
            return "$monthName $selectedYear"
        }
        val range = customDateRange ?: return null
        val start = range.first
        val end = range.second
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return when {
            start != null && end != null -> "${sdf.format(Date(start))} - ${sdf.format(Date(end))}"
            start != null -> "From ${sdf.format(Date(start))}"
            end != null -> "Until ${sdf.format(Date(end))}"
            else -> null
        }
    }
}

/**
 * Calculates epoch millis for the exact start (00:00:00.000) and end (23:59:59.999) of a given [month] and [year].
 */
fun getMonthRangeMillis(month: Month, year: Int): Pair<Long, Long> {
    val zone = java.time.ZoneId.systemDefault()
    val startOfMonth = java.time.LocalDate.of(year, month, 1)
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli()

    val endOfMonth = java.time.LocalDate.of(year, month, month.length(java.time.Year.isLeap(year.toLong())))
        .atTime(java.time.LocalTime.MAX)
        .atZone(zone)
        .toInstant()
        .toEpochMilli()

    return Pair(startOfMonth, endOfMonth)
}

// ─────────────────────────────────────────────────────────────────────────────
// Chart accent palette (theme-agnostic, category-specific colours)
// ─────────────────────────────────────────────────────────────────────────────

object ChartColors {
    val Food          = Color(0xFFF5A623)   // amber
    val Bills         = Color(0xFF4A90D9)   // blue
    val Shopping      = Color(0xFF9B59B6)   // purple
    val Transport     = Color(0xFF1ABC9C)   // cyan-green
    val Health        = Color(0xFFE91E63)   // pink
    val Entertainment = Color(0xFFFF7043)   // orange
    val Cinema        = Color(0xFFE040FB)   // magenta
    val DailyNeed     = Color(0xFF8D6E63)   // brown
    val Education     = Color(0xFF42A5F5)   // light blue
    val Investment    = Color(0xFF66BB6A)   // green
    val Savings       = Color(0xFF00ACC1)   // cyan/teal
    val Salary        = Color(0xFF26A69A)   // teal
    val IncomeGreen   = Color(0xFF2ECC71)   // emerald green
    val ExpenseRed    = Color(0xFFE74C3C)   // red

    fun forCategory(category: String): Color = when (category) {
        "Food"          -> Food
        "Bills"         -> Bills
        "Shopping"      -> Shopping
        "Transport"     -> Transport
        "Health"        -> Health
        "Entertainment" -> Entertainment
        "Cinema"        -> Cinema
        "Daily Need"    -> DailyNeed
        "Education"     -> Education
        "Investment"    -> Investment
        "Savings"       -> Savings
        "Salary"        -> Salary
        "Income"        -> IncomeGreen
        else            -> Color(0xFF78909C) // blue-grey fallback
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Category Emoji Map
// ─────────────────────────────────────────────────────────────────────────────

object CategoryEmojis {
    private val map = mapOf(
        "Food" to "🍔",
        "Bills" to "📺",
        "Shopping" to "📦",
        "Transport" to "⛽",
        "Health" to "💊",
        "Entertainment" to "🎮",
        "Cinema" to "🎬",
        "Daily Need" to "🛒",
        "Education" to "📚",
        "Investment" to "📈",
        "Salary" to "💰",
        "Income" to "💰",
        "Savings" to "🏦"
    )

    fun forCategory(category: String): String = map[category] ?: "💳"
}

// ─────────────────────────────────────────────────────────────────────────────
// State
// ─────────────────────────────────────────────────────────────────────────────

data class MoneyState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val selectedTab: Int = 0,               // 0 = Analysis, 1 = List
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val isFilterSheetVisible: Boolean = false,
    val analysisFilterState: AnalysisFilterState = AnalysisFilterState(
        activeDatePreset = QuickFilterPreset.THIS_MONTH,
        customDateRange = Pair(
            java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.DAY_OF_MONTH, 1)
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis,
            System.currentTimeMillis()
        ),
        selectedTypes = setOf(TransactionType.DEBIT)
    ),

    // API-driven data
    val transactions: List<Transaction> = emptyList(),
    val accounts: List<AccountInfo> = emptyList(),
    val apiCategories: List<String> = emptyList(),

    // Dialog & Edit/Delete/Detail States
    val detailTransaction: Transaction? = null,
    val editingTransaction: Transaction? = null,
    val deletingTransaction: Transaction? = null,
    val isUpdating: Boolean = false,
    val isDeleting: Boolean = false,
    val actionMessage: String? = null,

    // Multi-Selection & Bulk Operations
    val selectedTransactionIds: Set<Long> = emptySet(),
    val showBulkEditSheet: Boolean = false,
    val showBulkDeleteConfirm: Boolean = false,
    val isBulkUpdating: Boolean = false,
    val isBulkDeleting: Boolean = false,

    // Pagination
    val currentOffset: Int = 0,
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val totalTransactionCount: Int = 0
) {
    // ── Computed properties ──────────────────────────────────────────

    val isSelectionMode: Boolean get() = selectedTransactionIds.isNotEmpty()
    val selectedTransactions: List<Transaction>
        get() = transactions.filter { it.id in selectedTransactionIds }

    val totalIncome: Double
        get() = transactions
            .filter { it.type == TransactionType.CREDIT }
            .sumOf { it.amount }

    val totalExpenses: Double
        get() = transactions
            .filter { it.type == TransactionType.DEBIT && !it.isExcluded && !it.isSavings }
            .sumOf { it.amount }

    val spendingCategories: List<SpendingCategory>
        get() {
            val debits = transactions.filter { it.type == TransactionType.DEBIT && !it.isExcluded && !it.isSavings }
            return debits.groupBy { it.category }
                .map { (cat, txs) ->
                    SpendingCategory(
                        name = cat,
                        amount = txs.sumOf { it.amount },
                        color = ChartColors.forCategory(cat)
                    )
                }
                .sortedByDescending { it.amount }
        }

    val allAvailableCategories: List<String>
        get() = if (apiCategories.isNotEmpty()) apiCategories
                else transactions.map { it.category }.distinct().sorted()

    val mostUsedCategories: List<String>
        get() {
            val freqMap = transactions.groupBy { it.category }.mapValues { it.value.size }
            val baseList = if (apiCategories.isNotEmpty()) apiCategories
                           else transactions.map { it.category }.distinct()
            return baseList.sortedByDescending { freqMap[it] ?: 0 }
        }

    val allAvailableAccounts: List<String>
        get() {
            val raw = if (accounts.isNotEmpty()) accounts.map { it.account }
                      else transactions.map { it.bank }.distinct()
            val list = if (raw.isNotEmpty()) raw else DEFAULT_CANONICAL_ACCOUNTS
            return sortAccountsCanonical(list)
        }

    val filteredTransactions: List<Transaction>
        get() {
            val filters = analysisFilterState
            val isExpensesOnly = filters.selectedTypes == setOf(TransactionType.DEBIT)
            var list = transactions.filter { tx ->
                // Binary: Type filter
                if (filters.selectedTypes.isNotEmpty() && !filters.selectedTypes.contains(tx.type)) {
                    return@filter false
                }

                // If Expenses Only is selected, exclude Savings unless explicitly selected in category filter
                val isSavingsExplicitlySelected = filters.categoryFilters[tx.category] == ItemFilterStatus.INCLUDED || selectedCategory == tx.category
                if (isExpensesOnly && tx.isSavings && !isSavingsExplicitlySelected) {
                    return@filter false
                }

                // Category Include / Exclude
                val incCategories = filters.categoryFilters.filter { it.value == ItemFilterStatus.INCLUDED }.keys
                val excCategories = filters.categoryFilters.filter { it.value == ItemFilterStatus.EXCLUDED }.keys
                if (excCategories.contains(tx.category)) return@filter false
                if (incCategories.isNotEmpty() && !incCategories.contains(tx.category)) return@filter false

                // Account Include / Exclude
                val incAccounts = filters.accountFilters.filter { it.value == ItemFilterStatus.INCLUDED }.keys
                val excAccounts = filters.accountFilters.filter { it.value == ItemFilterStatus.EXCLUDED }.keys
                if (excAccounts.contains(tx.bank)) return@filter false
                if (incAccounts.isNotEmpty() && !incAccounts.contains(tx.bank)) return@filter false

                // Date Range
                filters.customDateRange?.let { range ->
                    val start = range.first
                    val end = range.second
                    if (start != null && tx.timestampMillis < start) return@filter false
                    if (end != null && tx.timestampMillis > end) return@filter false
                }

                true
            }

            if (selectedCategory != "All") {
                list = list.filter { it.category == selectedCategory }
            }

            if (searchQuery.isNotBlank()) {
                val q = searchQuery.lowercase().trim()
                list = list.filter {
                    it.title.lowercase().contains(q) ||
                    (it.description?.lowercase()?.contains(q) == true) ||
                    it.bank.lowercase().contains(q) ||
                    it.category.lowercase().contains(q) ||
                    (it.note?.lowercase()?.contains(q) == true)
                }
            }
            return list
        }

    val categoryFilters: List<String>
        get() = listOf("All") + spendingCategories.map { it.name }

    /**
     * Analysis Tab filtered transactions based on [analysisFilterState].
     */
    val filteredAnalysisTransactions: List<Transaction>
        get() {
            val filters = analysisFilterState
            val isExpensesOnly = filters.selectedTypes == setOf(TransactionType.DEBIT)
            return transactions.filter { tx ->
                // Binary: Type filter
                if (filters.selectedTypes.isNotEmpty() && !filters.selectedTypes.contains(tx.type)) {
                    return@filter false
                }

                // Web unconditionally hides exclude_analytics == true from the analyzer and charts
                if (tx.isExcluded) return@filter false

                // If Expenses Only is selected, exclude Savings unless explicitly included in category filter
                val isSavingsExplicitlyIncluded = filters.categoryFilters[tx.category] == ItemFilterStatus.INCLUDED
                if (isExpensesOnly && tx.isSavings && !isSavingsExplicitlyIncluded) {
                    return@filter false
                }

                // Category Include / Exclude
                val incCategories = filters.categoryFilters.filter { it.value == ItemFilterStatus.INCLUDED }.keys
                val excCategories = filters.categoryFilters.filter { it.value == ItemFilterStatus.EXCLUDED }.keys
                if (excCategories.contains(tx.category)) return@filter false
                if (incCategories.isNotEmpty() && !incCategories.contains(tx.category)) return@filter false

                // Account Include / Exclude
                val incAccounts = filters.accountFilters.filter { it.value == ItemFilterStatus.INCLUDED }.keys
                val excAccounts = filters.accountFilters.filter { it.value == ItemFilterStatus.EXCLUDED }.keys
                if (excAccounts.contains(tx.bank)) return@filter false
                if (incAccounts.isNotEmpty() && !incAccounts.contains(tx.bank)) return@filter false


                // Date Range
                filters.customDateRange?.let { range ->
                    val start = range.first
                    val end = range.second
                    if (start != null && tx.timestampMillis < start) return@filter false
                    if (end != null && tx.timestampMillis > end) return@filter false
                }

                true
            }
        }

    /**
     * Dynamically computed spending categories for Analysis based on active filters,
     * mirroring the Web Spending Analyser logic.
     */
    val spendingAnalyzerData: List<SpendingCategory>
        get() {
            val txList = filteredAnalysisTransactions

            // Drill-down logic: If exactly one category is included, group by description.
            val incCategories = analysisFilterState.categoryFilters.filter { it.value == ItemFilterStatus.INCLUDED }.keys
            val isDrillDown = incCategories.size == 1
            
            val grouped = if (isDrillDown) {
                txList.groupBy { it.description?.takeIf { d -> d.isNotBlank() }?.trim() ?: "No Description" }
            } else {
                txList.groupBy { it.category }
            }

            val result = grouped.map { (key, txs) ->
                val sum = txs.sumOf { Math.abs(it.amount) }
                val color = if (isDrillDown) {
                    val hash = Math.abs(key.hashCode())
                    val hue = (hash % 360).toFloat()
                    androidx.compose.ui.graphics.Color.hsv(hue, 0.6f, 0.9f)
                } else {
                    ChartColors.forCategory(key)
                }
                SpendingCategory(key, sum, color)
            }.filter { it.amount > 0 }
            
            return result.sortedByDescending { it.amount }
        }

    val filteredTotalIncome: Double
        get() = if (analysisFilterState.hasActiveFilters) {
            filteredAnalysisTransactions.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }
        } else {
            totalIncome
        }

    val filteredTotalExpenses: Double
        get() = if (analysisFilterState.hasActiveFilters) {
            filteredAnalysisTransactions.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
        } else {
            totalExpenses
        }
}
