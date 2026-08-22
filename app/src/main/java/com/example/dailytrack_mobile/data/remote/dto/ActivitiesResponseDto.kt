package com.example.dailytrack_mobile.data.remote.dto

import com.squareup.moshi.Json

data class PhysicalActivityDto(
    val id: Long,
    val date: String,
    val gym: Boolean,
    val badminton: Boolean,
    @Json(name = "table_tennis") val tableTennis: Boolean,
    val cricket: Boolean,
    val others: Boolean,
    val description: String?
)
