package com.example.dailytrack_mobile.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PhysicalActivityDto(
    @Json(name = "id") val id: Long,
    @Json(name = "date") val date: String,
    @Json(name = "gym") val gym: Boolean,
    @Json(name = "badminton") val badminton: Boolean,
    @Json(name = "table_tennis") val tableTennis: Boolean,
    @Json(name = "cricket") val cricket: Boolean,
    @Json(name = "others") val others: Boolean,
    @Json(name = "description") val description: String?
)

@JsonClass(generateAdapter = true)
data class AddActivityRequestDto(
    @Json(name = "date") val date: String,
    @Json(name = "gym") val gym: Boolean,
    @Json(name = "badminton") val badminton: Boolean,
    @Json(name = "table_tennis") val tableTennis: Boolean,
    @Json(name = "cricket") val cricket: Boolean,
    @Json(name = "others") val others: Boolean,
    @Json(name = "description") val description: String? = ""
)
