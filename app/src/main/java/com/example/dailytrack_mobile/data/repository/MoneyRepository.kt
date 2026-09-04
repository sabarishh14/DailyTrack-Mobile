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

    private var cachedAccounts: List<AccountDto>? = null
    private val cachedTransactions = mutableMapOf<String, TransactionsResponseDto>()
    private var cachedCategories: List<String>? = null

    fun clearCache() {
        cachedAccounts = null
        cachedTransactions.clear()
        cachedCategories = null
    }

    suspend fun getAccounts(forceRefresh: Boolean = false): Result<List<AccountDto>> = runCatching {
        if (demoDataManager.isDemoModeEnabled()) {
            demoDataManager.getAccounts()
        } else {
            if (!forceRefresh && cachedAccounts != null) {
                cachedAccounts!!
            } else {
                api.getAccounts().also { cachedAccounts = it }
            }
        }
    }

    suspend fun getTransactions(
        limit: Int = 100,
        offset: Int = 0,
        month: String? = null,
        forceRefresh: Boolean = false
    ): Result<TransactionsResponseDto> = runCatching {
        if (demoDataManager.isDemoModeEnabled()) {
            demoDataManager.getTransactions(limit = limit, offset = offset, month = month)
        } else {
            val key = "$limit-$offset-$month"
            if (!forceRefresh && cachedTransactions.containsKey(key)) {
                cachedTransactions[key]!!
            } else {
                api.getTransactions(limit = limit, offset = offset, month = month).also {
                    cachedTransactions[key] = it
                }
            }
        }
    }

    suspend fun getCategories(forceRefresh: Boolean = false): Result<List<String>> = runCatching {
        if (demoDataManager.isDemoModeEnabled()) {
            demoDataManager.getCategories()
        } else {
            if (!forceRefresh && cachedCategories != null) {
                cachedCategories!!
            } else {
                api.getCategories().categories.also { cachedCategories = it }
            }
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
            clearCache()
            demoDataManager.notifyDataUpdated()
        }
    }

    suspend fun updateTransaction(
        id: Long,
        type: String,
        category: String,
        amount: Double,
        note: String?,
        accountName: String,
        date: String,
        excludeAnalytics: Boolean
    ): Result<Unit> = runCatching {
        if (demoDataManager.isDemoModeEnabled()) {
            demoDataManager.updateTransaction(
                id = id,
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
            val response = api.updateTransaction(id, request)
            if (!response.success) {
                throw Exception(response.message ?: "Failed to update transaction")
            }
            clearCache()
            demoDataManager.notifyDataUpdated()
        }
    }

    suspend fun deleteTransaction(id: Long): Result<Unit> = runCatching {
        if (demoDataManager.isDemoModeEnabled()) {
            demoDataManager.deleteTransaction(id)
        } else {
            val response = api.deleteTransaction(id)
            if (!response.success) {
                throw Exception(response.message ?: "Failed to delete transaction")
            }
            clearCache()
            demoDataManager.notifyDataUpdated()
        }
    }

    suspend fun checkHealth(): Result<Boolean> = runCatching {
        if (demoDataManager.isDemoModeEnabled()) {
            return@runCatching true
        }
        try {
            val response = api.checkHealth()
            if (response.isSuccessful) {
                response.body()?.close()
                return@runCatching true
            }
        } catch (_: Exception) {
            // If / check threw, proceed to try /test-db
        }

        try {
            val dbResponse = api.testDb()
            if (dbResponse.isSuccessful) {
                dbResponse.body()?.close()
                return@runCatching true
            }
        } catch (_: Exception) {
            // Proceed to fallback
        }

        // Final fallback: if screen hydration works, getAccounts() will confirm the server is reachable
        api.getAccounts()
        true
    }
}
