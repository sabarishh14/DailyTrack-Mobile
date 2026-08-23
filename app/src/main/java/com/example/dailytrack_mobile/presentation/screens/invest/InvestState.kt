package com.example.dailytrack_mobile.presentation.screens.invest

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Investment accent palette
// ─────────────────────────────────────────────────────────────────────────────

object InvestColors {
    val Stocks       = Color(0xFF2ECC71)  // green
    val MutualFunds  = Color(0xFFA78BFA)  // lavender
    val Retirement   = Color(0xFF3B82F6)  // blue
    val FD           = Color(0xFFF59E0B)  // amber
    val Gold         = Color(0xFFFBBF24)  // gold
    val RealEstate   = Color(0xFFEC4899)  // pink
    val GainGreen    = Color(0xFF2ECC71)
    val LossRed      = Color(0xFFEF4444)
}

// ─────────────────────────────────────────────────────────────────────────────
// Data models
// ─────────────────────────────────────────────────────────────────────────────

data class InvestmentHolding(
    val name: String,
    val invested: Double,
    val current: Double,
    val category: InvestCategory
) {
    val pnl: Double get() = current - invested
    val pnlPercent: Double get() = if (invested == 0.0) 0.0 else (pnl / invested) * 100.0
    val isGain: Boolean get() = pnl >= 0
}

enum class InvestCategory(val label: String, val color: Color) {
    STOCKS("Stocks", InvestColors.Stocks),
    MUTUAL_FUNDS("Mutual Funds", InvestColors.MutualFunds),
    RETIREMENT("Retirement", InvestColors.Retirement),
    FD("FD", InvestColors.FD),
    GOLD("Gold", InvestColors.Gold),
    REAL_ESTATE("Real Estate", InvestColors.RealEstate)
}

enum class InvestTab(val label: String) {
    OVERVIEW("Overview"),
    STOCKS("Stocks"),
    MUTUAL_FUNDS("Mutual Funds"),
    RETIREMENT("Retirement"),
    FD("FD"),
    GOLD("Gold"),
    REAL_ESTATE("Real Estate")
}

// ─────────────────────────────────────────────────────────────────────────────
// State
// ─────────────────────────────────────────────────────────────────────────────

enum class ChartTimeRange(val label: String) {
    ONE_MONTH("1M"),
    THREE_MONTHS("3M"),
    SIX_MONTHS("6M"),
    ONE_YEAR("1Y"),
    YTD("YTD"),
    ALL("ALL")
}

data class ChartPoint(
    val date: String,
    val invested: Float,
    val current: Float
) {
    val pnl: Float get() = current - invested
    val pnlPercent: Float get() = if (invested == 0f) 0f else (pnl / invested) * 100f
}

data class InvestState(
    val isLoading: Boolean = false,
    val selectedTab: InvestTab = InvestTab.OVERVIEW,
    val holdings: List<InvestmentHolding> = emptyList(),
    val historicalSnapshots: List<com.example.dailytrack_mobile.data.remote.dto.PortfolioSnapshotDto> = emptyList(),
    val selectedTimeRange: ChartTimeRange = ChartTimeRange.ALL,
    val chartPoints: List<ChartPoint> = emptyList()
) {
    // ── Derived portfolio totals ────────────────────────────────────────
    val totalInvested: Double get() = holdings.sumOf { it.invested }
    val totalCurrent: Double get() = holdings.sumOf { it.current }
    val totalPnl: Double get() = totalCurrent - totalInvested
    val totalPnlPercent: Double get() = if (totalInvested == 0.0) 0.0 else (totalPnl / totalInvested) * 100.0
    val isOverallGain: Boolean get() = totalPnl >= 0

    // ── Per-category aggregation ────────────────────────────────────────
    data class CategorySummary(
        val category: InvestCategory,
        val invested: Double,
        val current: Double
    ) {
        val pnl: Double get() = current - invested
        val pnlPercent: Double get() = if (invested == 0.0) 0.0 else (pnl / invested) * 100.0
        val isGain: Boolean get() = pnl >= 0
    }

    val categorySummaries: List<CategorySummary>
        get() = InvestCategory.entries.map { cat ->
            val catHoldings = holdings.filter { it.category == cat }
            CategorySummary(
                category = cat,
                invested = catHoldings.sumOf { it.invested },
                current = catHoldings.sumOf { it.current }
            )
        }.filter { it.invested > 0 }

    // ── Filtered holdings for a tab ─────────────────────────────────────
    val filteredHoldings: List<InvestmentHolding>
        get() = when (selectedTab) {
            InvestTab.OVERVIEW -> holdings
            InvestTab.STOCKS -> holdings.filter { it.category == InvestCategory.STOCKS }
            InvestTab.MUTUAL_FUNDS -> holdings.filter { it.category == InvestCategory.MUTUAL_FUNDS }
            InvestTab.RETIREMENT -> holdings.filter { it.category == InvestCategory.RETIREMENT }
            InvestTab.FD -> holdings.filter { it.category == InvestCategory.FD }
            InvestTab.GOLD -> holdings.filter { it.category == InvestCategory.GOLD }
            InvestTab.REAL_ESTATE -> holdings.filter { it.category == InvestCategory.REAL_ESTATE }
        }

    val filteredInvested: Double get() = filteredHoldings.sumOf { it.invested }
    val filteredCurrent: Double get() = filteredHoldings.sumOf { it.current }
    val filteredPnl: Double get() = filteredCurrent - filteredInvested
    val isFilteredGain: Boolean get() = filteredPnl >= 0

}
