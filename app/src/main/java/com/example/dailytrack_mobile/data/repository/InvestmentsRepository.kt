package com.example.dailytrack_mobile.data.repository

import com.example.dailytrack_mobile.data.local.demo.DemoDataManager
import com.example.dailytrack_mobile.data.remote.api.DailyTrackApi
import com.example.dailytrack_mobile.data.remote.dto.EquityHoldingDto
import com.example.dailytrack_mobile.data.remote.dto.ManualAssetDto
import com.example.dailytrack_mobile.data.remote.dto.MutualFundHoldingDto
import com.example.dailytrack_mobile.data.remote.dto.PortfolioSnapshotDto
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharedFlow
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
    private val api: DailyTrackApi,
    private val demoDataManager: DemoDataManager
) {
    val dataUpdateFlow: SharedFlow<Unit> get() = demoDataManager.dataUpdateFlow

    suspend fun getFullPortfolio(): Result<FullPortfolioData> = coroutineScope {
        try {
            if (demoDataManager.isDemoModeEnabled()) {
                return@coroutineScope Result.success(demoDataManager.getFullPortfolio())
            }

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

    suspend fun addInvestment(
        name: String,
        category: String,
        amount: Double,
        frequency: String,
        note: String?
    ): Result<Unit> = runCatching {
        if (demoDataManager.isDemoModeEnabled()) {
            demoDataManager.addInvestment(
                name = name,
                category = category,
                amount = amount,
                frequency = frequency,
                note = note
            )
        }
    }

    suspend fun addAsset(
        name: String,
        assetClass: String,
        purchasePrice: Double,
        currentValue: Double,
        note: String?
    ): Result<Unit> = runCatching {
        if (demoDataManager.isDemoModeEnabled()) {
            demoDataManager.addManualAsset(
                name = name,
                assetClass = assetClass,
                purchasePrice = purchasePrice,
                currentValue = currentValue,
                note = note
            )
        }
    }
}
