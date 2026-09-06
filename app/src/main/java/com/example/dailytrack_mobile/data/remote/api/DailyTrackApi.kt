package com.example.dailytrack_mobile.data.remote.api

import com.example.dailytrack_mobile.data.remote.dto.AccountDto
import com.example.dailytrack_mobile.data.remote.dto.AddActivityRequestDto
import com.example.dailytrack_mobile.data.remote.dto.AddManualAssetRequestDto
import com.example.dailytrack_mobile.data.remote.dto.AddMediaResponseDto
import com.example.dailytrack_mobile.data.remote.dto.AddMovieDiaryRequestDto
import com.example.dailytrack_mobile.data.remote.dto.AddMovieRequestDto
import com.example.dailytrack_mobile.data.remote.dto.AddTransactionRequestDto
import com.example.dailytrack_mobile.data.remote.dto.BulkEditTransactionItemDto
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
import okhttp3.ResponseBody
import retrofit2.Response
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

    @PUT("/api/transactions/bulk-edit")
    suspend fun bulkEditTransactions(
        @Body updates: List<BulkEditTransactionItemDto>
    ): ApiResponseDto

    @POST("/api/transactions/bulk-delete")
    suspend fun bulkDeleteTransactions(
        @Body ids: List<Long>
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

    @GET("/api/media/diary")
    suspend fun getMediaDiary(
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("type") type: String = "all",
        @Query("show_id") showId: Int? = null
    ): com.example.dailytrack_mobile.data.remote.dto.MediaDiaryResponseDto

    @GET("/api/movies/stats")
    suspend fun getMovieStats(
        @Query("year") year: String? = null
    ): com.example.dailytrack_mobile.data.remote.dto.MediaStatsResponseDto

    @GET("/api/movies/details/{tmdb_id}")
    suspend fun getMovieDetails(
        @Path("tmdb_id") tmdbId: Int
    ): com.example.dailytrack_mobile.data.remote.dto.MediaDetailsResponseDto

    @GET("/api/tv/details/{tmdb_id}")
    suspend fun getTvDetails(
        @Path("tmdb_id") tmdbId: Int
    ): com.example.dailytrack_mobile.data.remote.dto.MediaDetailsResponseDto

    @PUT("/api/movies/{id}")
    suspend fun updateMovieStatus(
        @Path("id") id: Int,
        @Body request: com.example.dailytrack_mobile.data.remote.dto.UpdateMediaStatusRequestDto
    ): ApiResponseDto

    @PUT("/api/tv/shows/{id}")
    suspend fun updateTvShowStatus(
        @Path("id") id: Int,
        @Body request: com.example.dailytrack_mobile.data.remote.dto.UpdateMediaStatusRequestDto
    ): ApiResponseDto

    @DELETE("/api/movies/{id}")
    suspend fun deleteMovie(
        @Path("id") id: Int
    ): ApiResponseDto

    @DELETE("/api/tv/shows/{id}")
    suspend fun deleteTvShow(
        @Path("id") id: Int
    ): ApiResponseDto

    @PUT("/api/movies/diary")
    suspend fun updateMovieDiary(
        @Body request: com.example.dailytrack_mobile.data.remote.dto.UpdateDiaryLogRequestDto
    ): ApiResponseDto

    @PUT("/api/tv/diary")
    suspend fun updateTvDiary(
        @Body request: com.example.dailytrack_mobile.data.remote.dto.UpdateDiaryLogRequestDto
    ): ApiResponseDto

    @retrofit2.http.HTTP(method = "DELETE", path = "/api/movies/diary", hasBody = true)
    suspend fun deleteMovieDiary(
        @Body request: com.example.dailytrack_mobile.data.remote.dto.DeleteDiaryLogRequestDto
    ): ApiResponseDto

    @retrofit2.http.HTTP(method = "DELETE", path = "/api/tv/diary", hasBody = true)
    suspend fun deleteTvDiary(
        @Body request: com.example.dailytrack_mobile.data.remote.dto.DeleteDiaryLogRequestDto
    ): ApiResponseDto

    @POST("/api/movies/{id}/rematch")
    suspend fun rematchMovie(
        @Path("id") id: Int,
        @Body request: com.example.dailytrack_mobile.data.remote.dto.RematchMediaRequestDto
    ): ApiResponseDto

    @POST("/api/tv/shows/{id}/rematch")
    suspend fun rematchTvShow(
        @Path("id") id: Int,
        @Body request: com.example.dailytrack_mobile.data.remote.dto.RematchMediaRequestDto
    ): ApiResponseDto

    @GET("/")
    suspend fun checkHealth(): Response<ResponseBody>

    @GET("/test-db")
    suspend fun testDb(): Response<ResponseBody>

    @POST("/api/auth/firebase-login")
    suspend fun firebaseLogin(
        @Body request: com.example.dailytrack_mobile.data.remote.dto.FirebaseLoginRequestDto
    ): Response<com.example.dailytrack_mobile.data.remote.dto.FirebaseLoginResponseDto>
}

