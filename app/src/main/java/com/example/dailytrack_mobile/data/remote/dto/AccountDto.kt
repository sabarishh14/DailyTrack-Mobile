package com.example.dailytrack_mobile.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AccountDto(
    @Json(name = "account") val account: String,
    @Json(name = "balance") val balance: Double,
    @Json(name = "real_balance") val realBalance: Double?,
    @Json(name = "balance_tracked") val balanceTracked: Boolean
)
