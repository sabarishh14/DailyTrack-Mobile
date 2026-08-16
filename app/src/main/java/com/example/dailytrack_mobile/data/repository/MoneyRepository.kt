package com.example.dailytrack_mobile.data.repository

import com.example.dailytrack_mobile.data.remote.api.DailyTrackApi
import com.example.dailytrack_mobile.data.remote.dto.AccountDto
import com.example.dailytrack_mobile.data.remote.dto.TransactionsResponseDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MoneyRepository @Inject constructor(
    private val api: DailyTrackApi
) {
    suspend fun getAccounts(): Result<List<AccountDto>> = runCatching {
        api.getAccounts()
    }

    suspend fun getTransactions(
        limit: Int = 100,
        offset: Int = 0,
        month: String? = null
    ): Result<TransactionsResponseDto> = runCatching {
        api.getTransactions(limit = limit, offset = offset, month = month)
    }

    suspend fun getCategories(): Result<List<String>> = runCatching {
        api.getCategories().categories
    }
}
