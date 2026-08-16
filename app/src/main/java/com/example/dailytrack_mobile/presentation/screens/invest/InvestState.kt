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

data class InvestState(
    val isLoading: Boolean = false,
    val selectedTab: InvestTab = InvestTab.OVERVIEW,

    val holdings: List<InvestmentHolding> = listOf(
        // ── Stocks ──────────────────────────────────────────────────
        InvestmentHolding("Reliance Industries",  45_000.0,  52_300.0,  InvestCategory.STOCKS),
        InvestmentHolding("TCS",                  35_000.0,  38_200.0,  InvestCategory.STOCKS),
        InvestmentHolding("HDFC Bank",            40_000.0,  44_800.0,  InvestCategory.STOCKS),
        InvestmentHolding("Infosys",              30_000.0,  33_500.0,  InvestCategory.STOCKS),
        InvestmentHolding("Nifty 50 Index ETF",   55_000.0,  62_100.0,  InvestCategory.STOCKS),
        InvestmentHolding("ITC",                  25_000.0,  26_850.0,  InvestCategory.STOCKS),
        InvestmentHolding("Asian Paints",         20_000.0,  18_400.0,  InvestCategory.STOCKS),
        InvestmentHolding("Wipro",                10_000.0,   9_600.0,  InvestCategory.STOCKS),

        // ── Mutual Funds ────────────────────────────────────────────
        InvestmentHolding("SBI Bluechip Fund",      50_000.0,  57_400.0, InvestCategory.MUTUAL_FUNDS),
        InvestmentHolding("Axis Mid Cap Fund",      35_000.0,  40_800.0, InvestCategory.MUTUAL_FUNDS),
        InvestmentHolding("Parag Parikh Flexi Cap", 45_000.0,  51_200.0, InvestCategory.MUTUAL_FUNDS),
        InvestmentHolding("ICICI Prudential Value", 20_000.0,  22_100.0, InvestCategory.MUTUAL_FUNDS),
        InvestmentHolding("Motilal Oswal Nasdaq",   30_000.0,  27_600.0, InvestCategory.MUTUAL_FUNDS),
        InvestmentHolding("HDFC Small Cap Fund",    20_000.0,  23_900.0, InvestCategory.MUTUAL_FUNDS),

        // ── Retirement ──────────────────────────────────────────────
        InvestmentHolding("Employee PF",            2_40_000.0, 3_12_000.0, InvestCategory.RETIREMENT),
        InvestmentHolding("Employer PF",            2_40_000.0, 3_12_000.0, InvestCategory.RETIREMENT),
        InvestmentHolding("NPS - Tier I",             80_000.0,   97_600.0, InvestCategory.RETIREMENT),
        InvestmentHolding("PPF",                    1_50_000.0, 1_82_400.0, InvestCategory.RETIREMENT),

        // ── FD ──────────────────────────────────────────────────────
        InvestmentHolding("SBI FD - 1Y",             1_00_000.0, 1_07_200.0, InvestCategory.FD),
        InvestmentHolding("HDFC FD - 2Y",              75_000.0,   82_500.0, InvestCategory.FD),
        InvestmentHolding("Post Office TD",            50_000.0,   54_100.0, InvestCategory.FD),

        // ── Gold ────────────────────────────────────────────────────
        InvestmentHolding("Sovereign Gold Bond",      60_000.0,   78_000.0, InvestCategory.GOLD),
        InvestmentHolding("Gold ETF",                 40_000.0,   51_200.0, InvestCategory.GOLD),
        InvestmentHolding("Digital Gold",             15_000.0,   18_900.0, InvestCategory.GOLD),

        // ── Real Estate ─────────────────────────────────────────────
        InvestmentHolding("2BHK Apartment (Velachery)", 45_00_000.0, 52_00_000.0, InvestCategory.REAL_ESTATE),
        InvestmentHolding("Plot (ECR)",                12_00_000.0, 15_50_000.0, InvestCategory.REAL_ESTATE),
    )
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

    // ── Chart data points (fake historical for sparkline) ──────────────
    val chartPoints: List<Float>
        get() = listOf(
            0.72f, 0.68f, 0.75f, 0.71f, 0.78f, 0.82f, 0.79f,
            0.85f, 0.83f, 0.88f, 0.86f, 0.91f, 0.89f, 0.93f,
            0.90f, 0.95f, 0.92f, 0.97f, 0.94f, 1.0f
        )
}
