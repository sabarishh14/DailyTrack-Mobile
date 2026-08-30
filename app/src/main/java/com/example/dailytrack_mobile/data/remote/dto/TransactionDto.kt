package com.example.dailytrack_mobile.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SplitMemberDto(
    @Json(name = "name") val name: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "paid") val paid: Boolean
)

@JsonClass(generateAdapter = true)
data class SplitDto(
    @Json(name = "id") val id: Long,
    @Json(name = "total_amount") val totalAmount: Double,
    @Json(name = "members") val members: List<SplitMemberDto>
)

@JsonClass(generateAdapter = true)
data class TransactionDto(
    @Json(name = "id") val id: Long,
    @Json(name = "account") val account: String,
    @Json(name = "date") val date: String,
    @Json(name = "month") val month: String,
    @Json(name = "type") val type: String,
    @Json(name = "heading") val heading: String,
    @Json(name = "description") val description: String?,
    @Json(name = "amount") val amount: Double,
    @Json(name = "exclude_analytics") val excludeAnalytics: Boolean,
    @Json(name = "split") val split: SplitDto?
)

@JsonClass(generateAdapter = true)
data class TransactionsResponseDto(
    @Json(name = "transactions") val transactions: List<TransactionDto>,
    @Json(name = "total") val total: Int,
    @Json(name = "limit") val limit: Int,
    @Json(name = "offset") val offset: Int,
    @Json(name = "hasMore") val hasMore: Boolean
)

@JsonClass(generateAdapter = true)
data class CategoriesResponseDto(
    @Json(name = "success") val success: Boolean,
    @Json(name = "categories") val categories: List<String>
)

@JsonClass(generateAdapter = true)
data class AddTransactionRequestDto(
    @Json(name = "account") val account: String,
    @Json(name = "date") val date: String,
    @Json(name = "type") val type: String,
    @Json(name = "heading") val heading: String,
    @Json(name = "description") val description: String? = "",
    @Json(name = "amount") val amount: Double,
    @Json(name = "exclude_analytics") val excludeAnalytics: Boolean = false
)

@JsonClass(generateAdapter = true)
data class ApiResponseDto(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String? = null
)

