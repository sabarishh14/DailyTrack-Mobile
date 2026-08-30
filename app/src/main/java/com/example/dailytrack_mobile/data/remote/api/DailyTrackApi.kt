package com.example.dailytrack_mobile.data.remote.api

import com.example.dailytrack_mobile.data.remote.dto.AccountDto
import com.example.dailytrack_mobile.data.remote.dto.AddActivityRequestDto
import com.example.dailytrack_mobile.data.remote.dto.AddManualAssetRequestDto
import com.example.dailytrack_mobile.data.remote.dto.AddMediaResponseDto
import com.example.dailytrack_mobile.data.remote.dto.AddMovieDiaryRequestDto
import com.example.dailytrack_mobile.data.remote.dto.AddMovieRequestDto
import com.example.dailytrack_mobile.data.remote.dto.AddTransactionRequestDto
import com.example.dailytrack_mobile.data.remote.dto.AddTvDiaryRequestDto
import com.example.dailytrack_mobile.data.remote.dto.AddTvShowRequestDto
import com.example.dailytrack_mobile.data.remote.dto.ApiResponseDto
import com.example.dailytrack_mobile.data.remote.dto.CategoriesResponseDto
import com.example.dailytrack_mobile.data.remote.dto.EquityHoldingDto
import com.example.dailytrack_mobile.data.remote.dto.ManualAssetDto
import com.example.dailytrack_mobile.data.remote.dto.MediaLibraryResponseDto
import com.example.dailytrack_mobile.data.remote.dto.MediaSearchResponseDto
import com.example.dailytrack_mobile.data.remote.dto.MutualFundHoldingDto
import com.example.dailytrack_mobile.data.remote.dto.PhysicalActivityDto
import com.example.dailytrack_mobile.data.remote.dto.PortfolioSnapshotDto
import com.example.dailytrack_mobile.data.remote.dto.TransactionsResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

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

    @PUT("/api/transactions/{id}")
    suspend fun updateTransaction(
        @Path("id") id: Long,
        @Body transaction: AddTransactionRequestDto
    ): ApiResponseDto

    @DELETE("/api/transactions/{id}")
    suspend fun deleteTransaction(
        @Path("id") id: Long
    ): ApiResponseDto

    @GET("/api/transactions/categories")
    suspend fun getCategories(): CategoriesResponseDto

    @GET("/api/physical")
    suspend fun getPhysicalActivities(): List<PhysicalActivityDto>

    @POST("/api/physical")
    suspend fun addPhysicalActivity(
        @Body activity: AddActivityRequestDto
    ): ApiResponseDto

    @GET("/api/investments")
    suspend fun getInvestments(): List<PortfolioSnapshotDto>

    @GET("/api/equity")
    suspend fun getEquityHoldings(): List<EquityHoldingDto>

    @GET("/api/investments/{date}/holdings")
    suspend fun getMutualFundHoldings(@Path("date") date: String): List<MutualFundHoldingDto>

    @GET("/api/manual_assets")
    suspend fun getManualAssets(): List<ManualAssetDto>

    @POST("/api/manual_assets")
    suspend fun addManualAsset(
        @Body asset: AddManualAssetRequestDto
    ): ApiResponseDto

    @GET("/api/media/library")
    suspend fun getMediaLibrary(
        @Query("limit") limit: Int = 60,
        @Query("offset") offset: Int = 0,
        @Query("type") type: String = "all",
        @Query("status") status: String = "WATCHING"
    ): MediaLibraryResponseDto

    @GET("/api/media/search")
    suspend fun searchMedia(
        @Query("q") query: String
    ): MediaSearchResponseDto

    @POST("/api/movies")
    suspend fun addMovie(
        @Body request: AddMovieRequestDto
    ): AddMediaResponseDto

    @POST("/api/tv/shows")
    suspend fun addTvShow(
        @Body request: AddTvShowRequestDto
    ): AddMediaResponseDto

    @POST("/api/movies/diary")
    suspend fun addMovieDiary(
        @Body request: AddMovieDiaryRequestDto
    ): ApiResponseDto

    @POST("/api/tv/diary")
    suspend fun addTvDiary(
        @Body request: AddTvDiaryRequestDto
    ): ApiResponseDto
}
