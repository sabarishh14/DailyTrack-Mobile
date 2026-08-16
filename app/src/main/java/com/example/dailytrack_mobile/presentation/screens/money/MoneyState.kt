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

enum class FilterVisibility {
    ACTIVE,
    EXCLUDED
}

data class Transaction(
    val title: String,
    val date: String,
    val bank: String,
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val emoji: String,
    val isExcluded: Boolean = false,
    val timestampMillis: Long = System.currentTimeMillis()
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
    val selectedVisibilities: Set<FilterVisibility> = emptySet(), // Empty means neutral (all included)

    // Complex Filters (Category & Account Include/Exclude)
    val categoryFilters: Map<String, ItemFilterStatus> = emptyMap(),
    val accountFilters: Map<String, ItemFilterStatus> = emptyMap(),

    // Date & Time
    val financialYear: String? = null, // e.g. "FY 2025-26"
    val customDateRange: Pair<Long?, Long?>? = null // Pair of Start & End epoch millis
) {
    val activeFilterCount: Int
        get() {
            var count = 0
            if (selectedTypes.isNotEmpty()) count += selectedTypes.size
            if (selectedVisibilities.isNotEmpty()) count += selectedVisibilities.size
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
    val IncomeGreen   = Color(0xFF2ECC71)   // emerald green
    val ExpenseRed    = Color(0xFFE74C3C)   // red
}

// ─────────────────────────────────────────────────────────────────────────────
// State
// ─────────────────────────────────────────────────────────────────────────────

data class MoneyState(
    val isLoading: Boolean = false,
    val selectedTab: Int = 0,               // 0 = Analysis, 1 = List
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val isFilterSheetVisible: Boolean = false,
    val analysisFilterState: AnalysisFilterState = AnalysisFilterState(),

    val spendingCategories: List<SpendingCategory> = listOf(
        SpendingCategory("Food",          4_800.0,  ChartColors.Food),
        SpendingCategory("Bills",         6_200.0,  ChartColors.Bills),
        SpendingCategory("Shopping",      12_400.0, ChartColors.Shopping),
        SpendingCategory("Transport",     3_800.0,  ChartColors.Transport),
        SpendingCategory("Health",        2_500.0,  ChartColors.Health),
        SpendingCategory("Entertainment", 1_800.0,  ChartColors.Entertainment),
    ),

    val totalIncome: Double = 1_13_000.0,
    val totalExpenses: Double = 31_500.0,

    val transactions: List<Transaction> = listOf(
        Transaction("Swiggy Order",      "Jul 25", "HDFC",  488.0,    TransactionType.DEBIT,  "Food",          "🍔"),
        Transaction("Netflix",           "Jul 24", "ICICI", 649.0,    TransactionType.DEBIT,  "Bills",         "📺"),
        Transaction("Salary Credit",     "Jul 22", "Kotak", 95_000.0, TransactionType.CREDIT, "Income",        "💰"),
        Transaction("Petrol Fill",       "Jul 22", "HDFC",  2_200.0,  TransactionType.DEBIT,  "Transport",     "⛽"),
        Transaction("Amazon",            "Jul 20", "ICICI", 3_499.0,  TransactionType.DEBIT,  "Shopping",      "📦"),
        Transaction("Gym Membership",    "Jul 18", "HDFC",  2_500.0,  TransactionType.DEBIT,  "Health",        "💪"),
        Transaction("Freelance Payment", "Jul 16", "HDFC",  18_000.0, TransactionType.CREDIT, "Income",        "💸"),
        Transaction("Electricity Bill",  "Jul 15", "SBI",   1_840.0,  TransactionType.DEBIT,  "Bills",         "⚡"),
        Transaction("Zomato",            "Jul 14", "HDFC",  320.0,    TransactionType.DEBIT,  "Food",          "🍕"),
        Transaction("Movie Tickets",     "Jul 12", "ICICI", 750.0,    TransactionType.DEBIT,  "Entertainment", "🎬"),
        Transaction("Uber Ride",         "Jul 10", "HDFC",  380.0,    TransactionType.DEBIT,  "Transport",     "🚗"),
        Transaction("PhonePe Cashback",  "Jul 8",  "HDFC",  100.0,    TransactionType.CREDIT, "Income",        "🎁"),
    )
) {
    val allAvailableCategories: List<String>
        get() = listOf("Food", "Bills", "Shopping", "Transport", "Health", "Entertainment", "Income")

    val allAvailableAccounts: List<String>
        get() = listOf("HDFC", "ICICI", "Kotak", "SBI")

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

                // Binary: Visibility filter
                if (filters.selectedVisibilities.isNotEmpty()) {
                    val isTxActive = !tx.isExcluded
                    val matchActive = filters.selectedVisibilities.contains(FilterVisibility.ACTIVE) && isTxActive
                    val matchExcluded = filters.selectedVisibilities.contains(FilterVisibility.EXCLUDED) && tx.isExcluded
                    if (!matchActive && !matchExcluded) return@filter false
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
            val colorMap = mapOf(
                "Food" to ChartColors.Food,
                "Bills" to ChartColors.Bills,
                "Shopping" to ChartColors.Shopping,
                "Transport" to ChartColors.Transport,
                "Health" to ChartColors.Health,
                "Entertainment" to ChartColors.Entertainment
            )

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
                        result.add(SpendingCategory(catName, sum, colorMap[catName] ?: ChartColors.Shopping))
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

