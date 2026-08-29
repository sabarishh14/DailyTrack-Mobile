package com.example.dailytrack_mobile.data.repository

import com.example.dailytrack_mobile.data.local.demo.DemoDataManager
import com.example.dailytrack_mobile.data.remote.api.DailyTrackApi
import com.example.dailytrack_mobile.data.remote.dto.MediaLibraryResponseDto
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

    suspend fun getMediaLibrary(
        limit: Int = 60,
        offset: Int = 0,
        type: String = "all",
        status: String = "WATCHING"
    ): Result<MediaLibraryResponseDto> = runCatching {
        if (demoDataManager.isDemoModeEnabled()) {
            demoDataManager.getMediaLibrary(limit = limit, offset = offset, type = type, status = status)
        } else {
            api.getMediaLibrary(limit = limit, offset = offset, type = type, status = status)
        }
    }

    suspend fun addMediaShow(
        title: String,
        type: String,
        status: String,
        platform: String?,
        rating: Int,
        review: String?
    ): Result<MediaShowDto> = runCatching {
        if (demoDataManager.isDemoModeEnabled()) {
            demoDataManager.addMediaShow(
                title = title,
                type = type,
                status = status,
                platform = platform,
                rating = rating,
                review = review
            )
        } else {
            // Live API fallback (mocking response if remote post endpoint not defined)
            MediaShowDto(
                id = (System.currentTimeMillis() % 100000).toInt(),
                tmdbId = null,
                name = title,
                posterPath = null,
                type = type.lowercase(),
                status = status
            )
        }
    }
}
