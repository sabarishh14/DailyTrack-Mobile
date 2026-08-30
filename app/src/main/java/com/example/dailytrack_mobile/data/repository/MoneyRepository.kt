package com.example.dailytrack_mobile.data.repository

import com.example.dailytrack_mobile.data.local.demo.DemoDataManager
import com.example.dailytrack_mobile.data.remote.api.DailyTrackApi
import com.example.dailytrack_mobile.data.remote.dto.AccountDto
import com.example.dailytrack_mobile.data.remote.dto.AddTransactionRequestDto
import com.example.dailytrack_mobile.data.remote.dto.TransactionDto
import com.example.dailytrack_mobile.data.remote.dto.TransactionsResponseDto
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MoneyRepository @Inject constructor(
    private val api: DailyTrackApi,
    private val demoDataManager: DemoDataManager
) {
    val dataUpdateFlow: SharedFlow<Unit> get() = demoDataManager.dataUpdateFlow

    suspend fun getAccounts(): Result<List<AccountDto>> = runCatching {
        if (demoDataManager.isDemoModeEnabled()) {
            demoDataManager.getAccounts()
        } else {
            api.getAccounts()
        }
    }

    suspend fun getTransactions(
        limit: Int = 100,
        offset: Int = 0,
        month: String? = null
    ): Result<TransactionsResponseDto> = runCatching {
        if (demoDataManager.isDemoModeEnabled()) {
            demoDataManager.getTransactions(limit = limit, offset = offset, month = month)
        } else {
            api.getTransactions(limit = limit, offset = offset, month = month)
        }
    }

    suspend fun getCategories(): Result<List<String>> = runCatching {
        if (demoDataManager.isDemoModeEnabled()) {
            demoDataManager.getCategories()
        } else {
            api.getCategories().categories
        }
    }

    suspend fun addTransaction(
        type: String,
        category: String,
        amount: Double,
        note: String?,
        accountName: String,
        date: String,
        excludeAnalytics: Boolean
    ): Result<Unit> = runCatching {
        if (demoDataManager.isDemoModeEnabled()) {
            demoDataManager.addTransaction(
                type = type,
                category = category,
                amount = amount,
                note = note,
                accountName = accountName,
                date = date,
                excludeAnalytics = excludeAnalytics
            )
        } else {
            val request = AddTransactionRequestDto(
                account = accountName,
                date = date,
                type = type,
                heading = category,
                description = note ?: "",
                amount = amount,
                excludeAnalytics = excludeAnalytics
            )
            val response = api.addTransaction(request)
            if (!response.success) {
                throw Exception(response.message ?: "Failed to add transaction")
            }
            demoDataManager.notifyDataUpdated()
        }
    }
}
