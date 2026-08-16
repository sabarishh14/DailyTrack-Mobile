package com.example.dailytrack_mobile.presentation.screens.money

import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// Data models
// ─────────────────────────────────────────────────────────────────────────────

enum class TransactionType { CREDIT, DEBIT }

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
    LAST_30_DAYS,
    THIS_MONTH,
    EXPENSES_ONLY
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
    val date: String,
    val bank: String,
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val emoji: String,
    val isExcluded: Boolean = false,
    val timestampMillis: Long = System.currentTimeMillis(),
    val monthStr: String = "",
    val split: SplitInfo? = null
)

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
    val errorMessage: String? = null,
    val selectedTab: Int = 0,               // 0 = Analysis, 1 = List
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val isFilterSheetVisible: Boolean = false,
    val analysisFilterState: AnalysisFilterState = AnalysisFilterState(),

    // API-driven data
    val transactions: List<Transaction> = emptyList(),
    val accounts: List<AccountInfo> = emptyList(),
    val apiCategories: List<String> = emptyList(),

    // Pagination
    val currentOffset: Int = 0,
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val totalTransactionCount: Int = 0
) {
    // ── Computed properties ──────────────────────────────────────────

    val totalIncome: Double
        get() = transactions
            .filter { it.type == TransactionType.CREDIT }
            .sumOf { it.amount }

    val totalExpenses: Double
        get() = transactions
            .filter { it.type == TransactionType.DEBIT && !it.isExcluded }
            .sumOf { it.amount }

    val spendingCategories: List<SpendingCategory>
        get() {
            val debits = transactions.filter { it.type == TransactionType.DEBIT && !it.isExcluded }
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

    val allAvailableAccounts: List<String>
        get() = if (accounts.isNotEmpty()) accounts.map { it.account }
                else transactions.map { it.bank }.distinct().sorted()

    val filteredTransactions: List<Transaction>
        get() {
            var list = transactions
            if (selectedCategory != "All") {
                list = list.filter { it.category == selectedCategory }
            }
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.lowercase()
                list = list.filter {
                    it.title.lowercase().contains(q) ||
                    (it.description?.lowercase()?.contains(q) == true) ||
                    it.bank.lowercase().contains(q) ||
                    it.category.lowercase().contains(q)
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
            return transactions.filter { tx ->
                // Binary: Type filter
                if (filters.selectedTypes.isNotEmpty() && !filters.selectedTypes.contains(tx.type)) {
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
     * Dynamically computed spending categories for Analysis based on active filters.
     */
    val filteredSpendingCategories: List<SpendingCategory>
        get() {
            val txList = filteredAnalysisTransactions.filter { it.type == TransactionType.DEBIT }

            // Group filtered debits by category
            val grouped = txList.groupBy { it.category }
            val result = mutableListOf<SpendingCategory>()

            // Ensure categories that exist in baseline or filtered list are represented
            spendingCategories.forEach { baseCat ->
                val matchingTxs = grouped[baseCat.name]
                if (matchingTxs != null) {
                    val sum = matchingTxs.sumOf { it.amount }
                    if (sum > 0) {
                        result.add(SpendingCategory(baseCat.name, sum, baseCat.color))
                    }
                } else if (!analysisFilterState.hasActiveFilters) {
                    result.add(baseCat)
                }
            }

            // Also add any other debit categories if present
            grouped.forEach { (catName, txs) ->
                if (result.none { it.name == catName }) {
                    val sum = txs.sumOf { it.amount }
                    if (sum > 0) {
                        result.add(SpendingCategory(catName, sum, ChartColors.forCategory(catName)))
                    }
                }
            }

            return result
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
