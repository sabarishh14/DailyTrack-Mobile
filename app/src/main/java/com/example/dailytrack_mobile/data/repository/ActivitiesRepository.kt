package com.example.dailytrack_mobile.data.repository

import com.example.dailytrack_mobile.data.local.demo.DemoDataManager
import com.example.dailytrack_mobile.data.remote.api.DailyTrackApi
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

    suspend fun getPhysicalActivities(): Result<List<PhysicalActivityDto>> = runCatching {
        if (demoDataManager.isDemoModeEnabled()) {
            demoDataManager.getPhysicalActivities()
        } else {
            api.getPhysicalActivities()
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
    ): Result<PhysicalActivityDto> = runCatching {
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
            // Live fallback
            PhysicalActivityDto(
                id = System.currentTimeMillis(),
                date = date,
                gym = gym,
                badminton = badminton,
                tableTennis = tableTennis,
                cricket = cricket,
                others = others,
                description = description
            )
        }
    }
}
