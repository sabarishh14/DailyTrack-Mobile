package com.example.dailytrack_mobile.data.remote.api

import com.example.dailytrack_mobile.data.remote.dto.AccountDto
import com.example.dailytrack_mobile.data.remote.dto.AddTransactionRequestDto
import com.example.dailytrack_mobile.data.remote.dto.ApiResponseDto
import com.example.dailytrack_mobile.data.remote.dto.CategoriesResponseDto
import com.example.dailytrack_mobile.data.remote.dto.TransactionsResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import com.example.dailytrack_mobile.data.remote.dto.PhysicalActivityDto
import com.example.dailytrack_mobile.data.remote.dto.PortfolioSnapshotDto
import com.example.dailytrack_mobile.data.remote.dto.EquityHoldingDto
import com.example.dailytrack_mobile.data.remote.dto.MutualFundHoldingDto
import com.example.dailytrack_mobile.data.remote.dto.ManualAssetDto

interface DailyTrackApi {

    @GET("/api/accounts")
    suspend fun getAccounts(): List<AccountDto>

    @GET("/api/transactions")
    suspend fun getTransactions(
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("month") month: String? = null
    ): TransactionsResponseDto

    @POST("/api/transactions")
    suspend fun addTransaction(
        @Body transaction: AddTransactionRequestDto
    ): ApiResponseDto

    @GET("/api/transactions/categories")
    suspend fun getCategories(): CategoriesResponseDto

    @GET("/api/physical")
    suspend fun getPhysicalActivities(): List<PhysicalActivityDto>

    @GET("/api/investments")
    suspend fun getInvestments(): List<PortfolioSnapshotDto>

    @GET("/api/equity")
    suspend fun getEquityHoldings(): List<EquityHoldingDto>

    @GET("/api/investments/{date}/holdings")
    suspend fun getMutualFundHoldings(@retrofit2.http.Path("date") date: String): List<MutualFundHoldingDto>

    @GET("/api/manual_assets")
    suspend fun getManualAssets(): List<ManualAssetDto>

    @GET("/api/media/library")
    suspend fun getMediaLibrary(
        @Query("limit") limit: Int = 60,
        @Query("offset") offset: Int = 0,
        @Query("type") type: String = "all",
        @Query("status") status: String = "WATCHING"
    ): com.example.dailytrack_mobile.data.remote.dto.MediaLibraryResponseDto
}
