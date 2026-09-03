package com.example.dailytrack_mobile.presentation.screens.invest

sealed class InvestAction {
    data class SelectTab(val tab: InvestTab) : InvestAction()
    data class SelectTimeRange(val range: ChartTimeRange) : InvestAction()
    object Refresh : InvestAction()
}
