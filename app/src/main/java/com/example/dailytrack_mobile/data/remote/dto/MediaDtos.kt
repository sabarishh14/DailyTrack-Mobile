package com.example.dailytrack_mobile.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MediaLibraryResponseDto(
    @Json(name = "success") val success: Boolean,
    @Json(name = "shows") val shows: List<MediaShowDto>?,
    @Json(name = "total_count") val totalCount: Int?
)

@JsonClass(generateAdapter = true)
data class MediaShowDto(
    @Json(name = "id") val id: Int,
    @Json(name = "tmdb_id") val tmdbId: Int?,
    @Json(name = "name") val name: String?,
    @Json(name = "poster_path") val posterPath: String?,
    @Json(name = "type") val type: String?,
    @Json(name = "status") val status: String?
)
