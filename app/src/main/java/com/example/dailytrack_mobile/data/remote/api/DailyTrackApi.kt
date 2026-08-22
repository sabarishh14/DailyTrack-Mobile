package com.example.dailytrack_mobile.data.remote.api

import com.example.dailytrack_mobile.data.remote.dto.AccountDto
import com.example.dailytrack_mobile.data.remote.dto.CategoriesResponseDto
import com.example.dailytrack_mobile.data.remote.dto.TransactionsResponseDto
import retrofit2.http.GET
import retrofit2.http.Query
import com.example.dailytrack_mobile.data.remote.dto.PhysicalActivityDto

interface DailyTrackApi {

    @GET("/api/accounts")
    suspend fun getAccounts(): List<AccountDto>

    @GET("/api/transactions")
    suspend fun getTransactions(
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("month") month: String? = null
    ): TransactionsResponseDto

    @GET("/api/transactions/categories")
    suspend fun getCategories(): CategoriesResponseDto

    @GET("/api/physical")
    suspend fun getPhysicalActivities(): List<PhysicalActivityDto>
}
