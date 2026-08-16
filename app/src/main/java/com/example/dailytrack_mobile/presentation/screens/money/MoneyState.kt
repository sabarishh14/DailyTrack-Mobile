package com.example.dailytrack_mobile.presentation.screens.money

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Data models
// ─────────────────────────────────────────────────────────────────────────────

enum class TransactionType { CREDIT, DEBIT }

data class Transaction(
    val title: String,
    val date: String,
    val bank: String,
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val emoji: String
)

data class SpendingCategory(
    val name: String,
    val amount: Double,
    val color: Color
)

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
}
