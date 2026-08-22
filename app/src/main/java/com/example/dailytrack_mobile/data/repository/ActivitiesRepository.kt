package com.example.dailytrack_mobile.data.repository

import com.example.dailytrack_mobile.data.remote.api.DailyTrackApi
import com.example.dailytrack_mobile.data.remote.dto.PhysicalActivityDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivitiesRepository @Inject constructor(
    private val api: DailyTrackApi
) {
    suspend fun getPhysicalActivities(): Result<List<PhysicalActivityDto>> = runCatching {
        api.getPhysicalActivities()
    }
}
