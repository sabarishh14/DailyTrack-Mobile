package com.example.dailytrack_mobile.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MediaLibraryResponseDto(
    @Json(name = "success") val success: Boolean,
    @Json(name = "shows") val shows: List<MediaShowDto>?,
    @Json(name = "total_count") val totalCount: Int? = null,
    @Json(name = "hasMore") val hasMore: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class MediaShowDto(
    @Json(name = "id") val id: Int,
    @Json(name = "tmdb_id") val tmdbId: Int? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "added_on") val addedOn: String? = null
)

@JsonClass(generateAdapter = true)
data class MediaSearchResponseDto(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: MediaSearchDataDto? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class MediaSearchDataDto(
    @Json(name = "page") val page: Int? = null,
    @Json(name = "results") val results: List<MediaSearchResultDto>? = null,
    @Json(name = "total_pages") val totalPages: Int? = null,
    @Json(name = "total_results") val totalResults: Int? = null
)

@JsonClass(generateAdapter = true)
data class MediaSearchResultDto(
    @Json(name = "id") val id: Int,
    @Json(name = "title") val title: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "media_type") val mediaType: String? = null,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "release_date") val releaseDate: String? = null,
    @Json(name = "first_air_date") val firstAirDate: String? = null,
    @Json(name = "vote_average") val voteAverage: Double? = null,
    @Json(name = "overview") val overview: String? = null
) {
    val displayTitle: String
        get() = title ?: name ?: "Unknown"

    val year: String
        get() {
            val d = releaseDate ?: firstAirDate ?: ""
            return if (d.length >= 4) d.substring(0, 4) else ""
        }

    val isMovie: Boolean
        get() = mediaType.equals("movie", ignoreCase = true)
}

@JsonClass(generateAdapter = true)
data class AddMovieRequestDto(
    @Json(name = "tmdb_id") val tmdb_id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "poster_path") val poster_path: String? = null,
    @Json(name = "status") val status: String = "TO WATCH",
    @Json(name = "year") val year: Int? = null
)

@JsonClass(generateAdapter = true)
data class AddTvShowRequestDto(
    @Json(name = "tmdb_id") val tmdb_id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "poster_path") val poster_path: String? = null,
    @Json(name = "status") val status: String = "TO WATCH"
)

@JsonClass(generateAdapter = true)
data class AddMediaResponseDto(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String? = null,
    @Json(name = "id") val id: Int? = null,
    @Json(name = "show") val show: MediaShowDto? = null
)

@JsonClass(generateAdapter = true)
data class AddMovieDiaryRequestDto(
    @Json(name = "movie_id") val movie_id: Int,
    @Json(name = "date") val date: String,
    @Json(name = "rating") val rating: Float? = null,
    @Json(name = "review") val review: String? = null,
    @Json(name = "liked") val liked: Boolean = false,
    @Json(name = "rewatch") val rewatch: Boolean = false,
    @Json(name = "tags") val tags: String? = null
)

@JsonClass(generateAdapter = true)
data class AddTvDiaryRequestDto(
    @Json(name = "tv_show_id") val tv_show_id: Int,
    @Json(name = "date") val date: String,
    @Json(name = "rating") val rating: Float? = null,
    @Json(name = "review") val review: String? = null,
    @Json(name = "liked") val liked: Boolean = false,
    @Json(name = "rewatch") val rewatch: Boolean = false,
    @Json(name = "tags") val tags: String? = null,
    @Json(name = "season_number") val season_number: Int? = null,
    @Json(name = "episode_number") val episode_number: Int? = null
)
