package com.example.dailytrack_mobile.data.repository

import com.example.dailytrack_mobile.data.local.demo.DemoDataManager
import com.example.dailytrack_mobile.data.remote.api.DailyTrackApi
import com.example.dailytrack_mobile.data.remote.dto.AddActivityRequestDto
import com.example.dailytrack_mobile.data.remote.dto.PhysicalActivityDto
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivitiesRepository @Inject constructor(
    private val api: DailyTrackApi,
    private val demoDataManager: DemoDataManager
) {
    val dataUpdateFlow: SharedFlow<Unit> get() = demoDataManager.dataUpdateFlow

    private var cachedActivities: List<PhysicalActivityDto>? = null

    fun clearCache() {
        cachedActivities = null
    }

    suspend fun getPhysicalActivities(forceRefresh: Boolean = false): Result<List<PhysicalActivityDto>> = runCatching {
        if (demoDataManager.isDemoModeEnabled()) {
            demoDataManager.getPhysicalActivities()
        } else {
            if (!forceRefresh && cachedActivities != null) {
                cachedActivities!!
            } else {
                api.getPhysicalActivities().also { cachedActivities = it }
            }
        }
    }

    suspend fun addPhysicalActivity(
        date: String,
        gym: Boolean,
        badminton: Boolean,
        tableTennis: Boolean,
        cricket: Boolean,
        others: Boolean,
        description: String?
    ): Result<Unit> = runCatching {
        if (demoDataManager.isDemoModeEnabled()) {
            demoDataManager.addPhysicalActivity(
                date = date,
                gym = gym,
                badminton = badminton,
                tableTennis = tableTennis,
                cricket = cricket,
                others = others,
                description = description
            )
        } else {
            val request = AddActivityRequestDto(
                date = date,
                gym = gym,
                badminton = badminton,
                tableTennis = tableTennis,
                cricket = cricket,
                others = others,
                description = description ?: ""
            )
            val response = api.addPhysicalActivity(request)
            if (!response.success) {
                throw Exception(response.message ?: "Failed to log activity")
            }
            clearCache()
            demoDataManager.notifyDataUpdated()
        }
    }
}
