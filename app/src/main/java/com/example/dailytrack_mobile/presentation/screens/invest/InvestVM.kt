package com.example.dailytrack_mobile.presentation.screens.invest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailytrack_mobile.data.local.datastore.DemoModeManager
import com.example.dailytrack_mobile.data.repository.InvestmentsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InvestVM @Inject constructor(
    private val repository: InvestmentsRepository,
    private val demoModeManager: DemoModeManager
) : ViewModel() {

    private val _state = MutableStateFlow(InvestState(isLoading = true))
    val state: StateFlow<InvestState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            demoModeManager.isDemoModeEnabledFlow.collect {
                loadInvestments()
            }
        }
        viewModelScope.launch {
            repository.dataUpdateFlow.collect {
                loadInvestments()
            }
        }
    }

    fun onAction(action: InvestAction) {
        when (action) {
            is InvestAction.SelectTab -> _state.update {
                it.copy(
                    selectedTab = action.tab,
                    chartPoints = getFilteredPoints(it.historicalSnapshots, it.selectedTimeRange, action.tab)
                )
            }
            is InvestAction.SelectTimeRange -> _state.update {
                it.copy(
                    selectedTimeRange = action.range,
                    chartPoints = getFilteredPoints(it.historicalSnapshots, action.range, it.selectedTab)
                )
            }
        }
    }

    private fun loadInvestments() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = repository.getFullPortfolio()
            if (result.isSuccess) {
                val data = result.getOrNull()
                if (data != null) {
                    val allHoldings = mutableListOf<InvestmentHolding>()

                    // Map Equity
                    data.equityHoldings.forEach { eq ->
                        allHoldings.add(
                            InvestmentHolding(
                                name = eq.symbol,
                                invested = eq.investedValue,
                                current = eq.currentValue,
                                category = InvestCategory.STOCKS
                            )
                        )
                    }

                    // Map Mutual Funds
                    data.mutualFundHoldings.forEach { mf ->
                        allHoldings.add(
                            InvestmentHolding(
                                name = mf.symbol,
                                invested = mf.investedValue,
                                current = mf.currentValue,
                                category = InvestCategory.MUTUAL_FUNDS
                            )
                        )
                    }

                    // Map Manual Assets
                    data.manualAssets.forEach { asset ->
                        val cat = when (asset.category.uppercase()) {
                            "FD", "RD", "CASH" -> InvestCategory.FD
                            "EPF", "PPF", "NPS" -> InvestCategory.RETIREMENT
                            "SGB", "GOLD" -> InvestCategory.GOLD
                            "REALESTATE" -> InvestCategory.REAL_ESTATE
                            else -> InvestCategory.STOCKS // Fallback, though we shouldn't hit this normally
                        }
                        
                        allHoldings.add(
                            InvestmentHolding(
                                name = asset.name,
                                invested = asset.investedValue,
                                current = asset.currentValue,
                                category = cat
                            )
                        )
                    }

                    _state.update { 
                        val nextState = it.copy(
                            holdings = allHoldings,
                            historicalSnapshots = data.snapshots,
                            isLoading = false
                        )
                        nextState.copy(chartPoints = getFilteredPoints(nextState.historicalSnapshots, nextState.selectedTimeRange, nextState.selectedTab))
                    }
                }
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun getFilteredPoints(
        snapshots: List<com.example.dailytrack_mobile.data.remote.dto.PortfolioSnapshotDto>, 
        range: ChartTimeRange,
        tab: InvestTab
    ): List<ChartPoint> {
        if (snapshots.isEmpty()) return emptyList()
        
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val now = java.time.LocalDate.now()
        
        val cutoffDate = when (range) {
            ChartTimeRange.ONE_MONTH -> now.minusMonths(1)
            ChartTimeRange.THREE_MONTHS -> now.minusMonths(3)
            ChartTimeRange.SIX_MONTHS -> now.minusMonths(6)
            ChartTimeRange.ONE_YEAR -> now.minusYears(1)
            ChartTimeRange.YTD -> java.time.LocalDate.of(now.year, 1, 1)
            ChartTimeRange.ALL -> null
        }

        return snapshots
            .reversed()
            .filter {
                if (cutoffDate == null) true
                else {
                    try {
                        val date = java.time.LocalDate.parse(it.date.substringBefore("T"), formatter)
                        !date.isBefore(cutoffDate)
                    } catch (e: Exception) {
                        true
                    }
                }
            }
            .mapNotNull { snapshot ->
                val curr = when (tab) {
                    InvestTab.OVERVIEW -> snapshot.grandTotalCurr
                    InvestTab.STOCKS -> snapshot.currStocks
                    InvestTab.MUTUAL_FUNDS -> snapshot.currMf
                    InvestTab.RETIREMENT -> snapshot.currProv
                    InvestTab.FD -> snapshot.currFixed
                    InvestTab.GOLD -> snapshot.currGold
                    InvestTab.REAL_ESTATE -> null
                }?.toFloat()
                
                val inv = when (tab) {
                    InvestTab.OVERVIEW -> snapshot.grandTotalInv
                    InvestTab.STOCKS -> snapshot.invStocks
                    InvestTab.MUTUAL_FUNDS -> snapshot.invMf
                    InvestTab.RETIREMENT -> snapshot.invProv
                    InvestTab.FD -> snapshot.invFixed
                    InvestTab.GOLD -> snapshot.invGold
                    InvestTab.REAL_ESTATE -> null
                }?.toFloat()

                if (curr != null && curr > 0f) {
                    ChartPoint(
                        date = snapshot.date.substringBefore("T"),
                        invested = inv ?: 0f,
                        current = curr
                    )
                } else null
            }
    }
}
