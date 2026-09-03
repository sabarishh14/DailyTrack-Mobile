package com.example.dailytrack_mobile.data.repository

import com.example.dailytrack_mobile.data.local.demo.DemoDataManager
import com.example.dailytrack_mobile.data.remote.api.DailyTrackApi
import com.example.dailytrack_mobile.data.remote.dto.AddMovieDiaryRequestDto
import com.example.dailytrack_mobile.data.remote.dto.AddMovieRequestDto
import com.example.dailytrack_mobile.data.remote.dto.AddTvDiaryRequestDto
import com.example.dailytrack_mobile.data.remote.dto.AddTvShowRequestDto
import com.example.dailytrack_mobile.data.remote.dto.MediaLibraryResponseDto
import com.example.dailytrack_mobile.data.remote.dto.MediaSearchResultDto
import com.example.dailytrack_mobile.data.remote.dto.MediaShowDto
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SabdekhoRepository @Inject constructor(
    private val api: DailyTrackApi,
    private val demoDataManager: DemoDataManager
) {
    val dataUpdateFlow: SharedFlow<Unit> get() = demoDataManager.dataUpdateFlow

    private val cachedMediaLibrary = mutableMapOf<String, MediaLibraryResponseDto>()

    fun clearCache() {
        cachedMediaLibrary.clear()
    }

    suspend fun getMediaLibrary(
        limit: Int = 60,
        offset: Int = 0,
        type: String = "all",
        status: String = "WATCHING",
        forceRefresh: Boolean = false
    ): Result<MediaLibraryResponseDto> = runCatching {
        if (demoDataManager.isDemoModeEnabled()) {
            demoDataManager.getMediaLibrary(limit = limit, offset = offset, type = type, status = status)
        } else {
            val key = "$limit-$offset-$type-$status"
            if (!forceRefresh && cachedMediaLibrary.containsKey(key)) {
                cachedMediaLibrary[key]!!
            } else {
                api.getMediaLibrary(limit = limit, offset = offset, type = type, status = status).also {
                    cachedMediaLibrary[key] = it
                }
            }
        }
    }

    suspend fun searchMedia(query: String): Result<List<MediaSearchResultDto>> = runCatching {
        if (query.isBlank()) return@runCatching emptyList()

        if (demoDataManager.isDemoModeEnabled()) {
            demoDataManager.searchMedia(query)
        } else {
            val response = api.searchMedia(query.trim())
            val rawResults = response.data?.results ?: emptyList()
            rawResults.filter { it.mediaType == "movie" || it.mediaType == "tv" }
        }
    }

    suspend fun addMediaShow(
        tmdbId: Int?,
        title: String,
        type: String, // "Movie", "Series", "TV", "Anime"
        posterPath: String?,
        status: String, // "WATCHING", "TO WATCH", "WATCHED", "DROPPED"
        releaseYear: Int?,
        platform: String?,
        rating: Float?,
        review: String?,
        date: String
    ): Result<MediaShowDto> = runCatching {
        if (demoDataManager.isDemoModeEnabled()) {
            val created = demoDataManager.addMediaShow(
                title = title,
                type = type,
                status = status,
                posterPath = posterPath,
                tmdbId = tmdbId,
                platform = platform,
                rating = rating,
                review = review,
                date = date
            )
            demoDataManager.notifyDataUpdated()
            created
        } else {
            val isMovie = type.equals("movie", ignoreCase = true)
            val effectiveTmdbId = tmdbId ?: 0

            if (isMovie) {
                val addResponse = api.addMovie(
                    AddMovieRequestDto(
                        tmdb_id = effectiveTmdbId,
                        name = title,
                        poster_path = posterPath,
                        status = status,
                        year = releaseYear
                    )
                )
                val showId = addResponse.id ?: addResponse.show?.id ?: 0

                // If user provided rating or review and showId is available, log diary
                if (showId > 0 && ((rating != null && rating > 0f) || !review.isNullOrBlank())) {
                    try {
                        api.addMovieDiary(
                            AddMovieDiaryRequestDto(
                                movie_id = showId,
                                date = date,
                                rating = rating?.takeIf { it > 0f },
                                review = review?.takeIf { it.isNotBlank() },
                                tags = platform?.takeIf { it.isNotBlank() }
                            )
                        )
                    } catch (e: Exception) {
                        // Log failure is non-blocking for show creation
                        e.printStackTrace()
                    }
                }

                clearCache()
                demoDataManager.notifyDataUpdated()
                addResponse.show ?: MediaShowDto(
                    id = showId,
                    tmdbId = tmdbId,
                    name = title,
                    posterPath = posterPath,
                    type = "movie",
                    status = status
                )
            } else {
                val addResponse = api.addTvShow(
                    AddTvShowRequestDto(
                        tmdb_id = effectiveTmdbId,
                        name = title,
                        poster_path = posterPath,
                        status = status
                    )
                )
                val showId = addResponse.id ?: addResponse.show?.id ?: 0

                if (showId > 0 && ((rating != null && rating > 0f) || !review.isNullOrBlank())) {
                    try {
                        api.addTvDiary(
                            AddTvDiaryRequestDto(
                                tv_show_id = showId,
                                date = date,
                                rating = rating?.takeIf { it > 0f },
                                review = review?.takeIf { it.isNotBlank() },
                                tags = platform?.takeIf { it.isNotBlank() }
                            )
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                clearCache()
                demoDataManager.notifyDataUpdated()
                addResponse.show ?: MediaShowDto(
                    id = showId,
                    tmdbId = tmdbId,
                    name = title,
                    posterPath = posterPath,
                    type = "tv",
                    status = status
                )
            }
        }
    }
}
