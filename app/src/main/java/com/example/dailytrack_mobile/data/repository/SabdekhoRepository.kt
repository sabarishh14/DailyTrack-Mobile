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
    private val cachedMediaDetails = mutableMapOf<String, com.example.dailytrack_mobile.data.remote.dto.MediaDetailsDataDto>()

    fun clearCache() {
        cachedMediaLibrary.clear()
        cachedMediaDetails.clear()
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
        date: String,
        liked: Boolean = false,
        rewatch: Boolean = false,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null
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
                date = date,
                liked = liked,
                rewatch = rewatch,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber
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

                val shouldLog = status.equals("WATCHED", ignoreCase = true) ||
                        (rating != null && rating > 0f) ||
                        !review.isNullOrBlank() ||
                        liked ||
                        rewatch
                if (showId > 0 && shouldLog) {
                    try {
                        api.addMovieDiary(
                            AddMovieDiaryRequestDto(
                                movie_id = showId,
                                date = date,
                                rating = rating?.takeIf { it > 0f },
                                review = review?.takeIf { it.isNotBlank() },
                                liked = liked,
                                rewatch = rewatch,
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

                val shouldLog = status.equals("WATCHED", ignoreCase = true) ||
                        (rating != null && rating > 0f) ||
                        !review.isNullOrBlank() ||
                        liked ||
                        rewatch ||
                        seasonNumber != null
                if (showId > 0 && shouldLog) {
                    try {
                        api.addTvDiary(
                            AddTvDiaryRequestDto(
                                tv_show_id = showId,
                                date = date,
                                rating = rating?.takeIf { it > 0f },
                                review = review?.takeIf { it.isNotBlank() },
                                liked = liked,
                                rewatch = rewatch,
                                tags = platform?.takeIf { it.isNotBlank() },
                                season_number = seasonNumber,
                                episode_number = episodeNumber
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

    suspend fun getMediaDiary(
        limit: Int = 100,
        offset: Int = 0,
        type: String = "all",
        showId: Int? = null,
        forceRefresh: Boolean = false
    ): Result<com.example.dailytrack_mobile.data.remote.dto.MediaDiaryResponseDto> = runCatching {
        if (demoDataManager.isDemoModeEnabled()) {
            demoDataManager.getMediaDiary(limit = limit, offset = offset, type = type, showId = showId)
        } else {
            api.getMediaDiary(limit = limit, offset = offset, type = type, showId = showId)
        }
    }

    suspend fun getMovieStats(
        year: String? = null,
        forceRefresh: Boolean = false
    ): Result<com.example.dailytrack_mobile.data.remote.dto.MediaStatsResponseDto> = runCatching {
        if (demoDataManager.isDemoModeEnabled()) {
            demoDataManager.getMovieStats(year = year)
        } else {
            api.getMovieStats(year = year)
        }
    }

    suspend fun getMediaDetails(
        tmdbId: Int,
        isMovie: Boolean
    ): Result<com.example.dailytrack_mobile.data.remote.dto.MediaDetailsDataDto> = runCatching {
        val cacheKey = "$tmdbId-$isMovie"
        if (cachedMediaDetails.containsKey(cacheKey)) {
            return@runCatching cachedMediaDetails[cacheKey]!!
        }
        if (demoDataManager.isDemoModeEnabled()) {
            demoDataManager.getMediaDetails(tmdbId = tmdbId, isMovie = isMovie).also {
                cachedMediaDetails[cacheKey] = it
            }
        } else {
            val resp = if (isMovie) api.getMovieDetails(tmdbId) else api.getTvDetails(tmdbId)
            val data = resp.data ?: throw Exception(resp.message ?: "Failed to load details")
            cachedMediaDetails[cacheKey] = data
            data
        }
    }

    suspend fun updateMediaStatus(
        showId: Int,
        isMovie: Boolean,
        status: String
    ): Result<Boolean> = runCatching {
        if (demoDataManager.isDemoModeEnabled()) {
            val res = demoDataManager.updateMediaStatus(showId, isMovie, status)
            demoDataManager.notifyDataUpdated()
            clearCache()
            res
        } else {
            val req = com.example.dailytrack_mobile.data.remote.dto.UpdateMediaStatusRequestDto(status = status)
            if (isMovie) api.updateMovieStatus(showId, req) else api.updateTvShowStatus(showId, req)
            clearCache()
            demoDataManager.notifyDataUpdated()
            true
        }
    }

    suspend fun deleteMediaShow(
        showId: Int,
        isMovie: Boolean
    ): Result<Boolean> = runCatching {
        if (demoDataManager.isDemoModeEnabled()) {
            val res = demoDataManager.deleteMediaShow(showId, isMovie)
            demoDataManager.notifyDataUpdated()
            clearCache()
            res
        } else {
            if (isMovie) api.deleteMovie(showId) else api.deleteTvShow(showId)
            clearCache()
            demoDataManager.notifyDataUpdated()
            true
        }
    }

    suspend fun logDiaryEntry(
        showId: Int,
        isMovie: Boolean,
        date: String,
        rating: Float?,
        review: String?,
        liked: Boolean,
        rewatch: Boolean,
        tags: String?,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null
    ): Result<Boolean> = runCatching {
        if (demoDataManager.isDemoModeEnabled()) {
            val res = demoDataManager.addDiaryLog(
                showId = showId,
                isMovie = isMovie,
                date = date,
                rating = rating,
                review = review,
                liked = liked,
                rewatch = rewatch,
                tags = tags,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber
            )
            demoDataManager.notifyDataUpdated()
            clearCache()
            res
        } else {
            if (isMovie) {
                api.addMovieDiary(
                    AddMovieDiaryRequestDto(
                        movie_id = showId,
                        date = date,
                        rating = rating?.takeIf { it > 0f },
                        review = review?.takeIf { it.isNotBlank() },
                        liked = liked,
                        rewatch = rewatch,
                        tags = tags?.takeIf { it.isNotBlank() }
                    )
                )
            } else {
                api.addTvDiary(
                    AddTvDiaryRequestDto(
                        tv_show_id = showId,
                        date = date,
                        rating = rating?.takeIf { it > 0f },
                        review = review?.takeIf { it.isNotBlank() },
                        liked = liked,
                        rewatch = rewatch,
                        tags = tags?.takeIf { it.isNotBlank() },
                        season_number = seasonNumber,
                        episode_number = episodeNumber
                    )
                )
            }
            demoDataManager.notifyDataUpdated()
            clearCache()
            true
        }
    }

    suspend fun updateDiaryLog(
        logId: Int,
        isMovie: Boolean,
        rating: Float?,
        review: String?,
        liked: Boolean?,
        rewatch: Boolean?,
        tags: String?,
        date: String? = null,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null
    ): Result<Boolean> = runCatching {
        if (demoDataManager.isDemoModeEnabled()) {
            val res = demoDataManager.updateDiaryLog(
                logId = logId,
                isMovie = isMovie,
                rating = rating,
                review = review,
                liked = liked,
                rewatch = rewatch,
                tags = tags,
                date = date,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber
            )
            demoDataManager.notifyDataUpdated()
            res
        } else {
            val req = com.example.dailytrack_mobile.data.remote.dto.UpdateDiaryLogRequestDto(
                log_ids = listOf(logId),
                rating = rating,
                review = review,
                liked = liked,
                rewatch = rewatch,
                tags = tags,
                date = date,
                season_number = seasonNumber,
                episode_number = episodeNumber
            )
            if (isMovie) api.updateMovieDiary(req) else api.updateTvDiary(req)
            demoDataManager.notifyDataUpdated()
            true
        }
    }

    suspend fun deleteDiaryLog(
        logId: Int,
        isMovie: Boolean
    ): Result<Boolean> = runCatching {
        if (demoDataManager.isDemoModeEnabled()) {
            val res = demoDataManager.deleteDiaryLog(logId = logId, isMovie = isMovie)
            demoDataManager.notifyDataUpdated()
            res
        } else {
            val req = com.example.dailytrack_mobile.data.remote.dto.DeleteDiaryLogRequestDto(log_ids = listOf(logId))
            if (isMovie) api.deleteMovieDiary(req) else api.deleteTvDiary(req)
            demoDataManager.notifyDataUpdated()
            true
        }
    }

    suspend fun rematchMedia(
        showId: Int,
        isMovie: Boolean,
        tmdbId: Int,
        name: String,
        posterPath: String?,
        year: String?
    ): Result<Boolean> = runCatching {
        if (demoDataManager.isDemoModeEnabled()) {
            val res = demoDataManager.rematchMedia(showId, isMovie, tmdbId, name, posterPath, year)
            demoDataManager.notifyDataUpdated()
            clearCache()
            res
        } else {
            val req = com.example.dailytrack_mobile.data.remote.dto.RematchMediaRequestDto(
                tmdb_id = tmdbId,
                name = name,
                poster_path = posterPath,
                year = year
            )
            if (isMovie) api.rematchMovie(showId, req) else api.rematchTvShow(showId, req)
            demoDataManager.notifyDataUpdated()
            clearCache()
            true
        }
    }
}

