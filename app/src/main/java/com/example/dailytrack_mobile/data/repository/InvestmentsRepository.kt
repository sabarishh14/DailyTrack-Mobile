package com.example.dailytrack_mobile.data.repository

import com.example.dailytrack_mobile.data.remote.api.DailyTrackApi
import com.example.dailytrack_mobile.data.remote.dto.EquityHoldingDto
import com.example.dailytrack_mobile.data.remote.dto.ManualAssetDto
import com.example.dailytrack_mobile.data.remote.dto.MutualFundHoldingDto
import com.example.dailytrack_mobile.data.remote.dto.PortfolioSnapshotDto
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

data class FullPortfolioData(
    val snapshots: List<PortfolioSnapshotDto>,
    val equityHoldings: List<EquityHoldingDto>,
    val mutualFundHoldings: List<MutualFundHoldingDto>,
    val manualAssets: List<ManualAssetDto>
)

@Singleton
class InvestmentsRepository @Inject constructor(
    private val api: DailyTrackApi
) {
    suspend fun getFullPortfolio(): Result<FullPortfolioData> = coroutineScope {
        try {
            // 1. Fetch snapshots to get historical data and the latest date
            val snapshots = api.getInvestments()
            
            // Extract the latest date (assuming the list is ordered descending as in the backend)
            val latestDate = snapshots.firstOrNull()?.date?.substringBefore("T")
            
            // 2. Fetch parallel requests for holdings
            val equityDeferred = async { api.getEquityHoldings() }
            val mfDeferred = async { 
                if (latestDate != null) {
                    api.getMutualFundHoldings(latestDate)
                } else {
                    emptyList()
                }
            }
            val manualAssetsDeferred = async { api.getManualAssets() }
            
            val equityHoldings = equityDeferred.await()
            val mutualFundHoldings = mfDeferred.await()
            val manualAssets = manualAssetsDeferred.await()
            
            Result.success(
                FullPortfolioData(
                    snapshots = snapshots,
                    equityHoldings = equityHoldings,
                    mutualFundHoldings = mutualFundHoldings,
                    manualAssets = manualAssets
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
