package com.example.dailytrack_mobile.presentation.screens.activities

import java.time.Month
import java.time.Year

// ─────────────────────────────────────────────────────────────────────────────
// Data models
// ─────────────────────────────────────────────────────────────────────────────

enum class ActivityType(val label: String) {
    GYM("Gym"),
    BADMINTON("Badminton"),
    RUNNING("Running"),
    TABLE_TENNIS("TT"),
    OTHERS("Others")
}

data class ActivityEntry(
    val dayOfMonth: Int,
    val dayOfWeek: String,   // e.g. "FRI"
    val activities: List<ActivityType>  // empty list means rest day
)

// ─────────────────────────────────────────────────────────────────────────────
// Screen state
// ─────────────────────────────────────────────────────────────────────────────

data class ActivitiesState(
    val isLoading: Boolean = false,
    val selectedMonth: Month = Month.JULY,
    val selectedYear: Int = 2026,
    val activityLog: List<ActivityEntry> = sampleActivities()
)

// ─────────────────────────────────────────────────────────────────────────────
// Sample / hardcoded data for July 2026
// ─────────────────────────────────────────────────────────────────────────────

private fun sampleActivities(): List<ActivityEntry> = listOf(
    ActivityEntry(26, "SUN", listOf(ActivityType.RUNNING)),
    ActivityEntry(25, "SAT", listOf(ActivityType.GYM, ActivityType.RUNNING)),
    ActivityEntry(24, "FRI", listOf(ActivityType.BADMINTON)),
    ActivityEntry(23, "THU", listOf()),
    ActivityEntry(22, "WED", listOf(ActivityType.GYM)),
    ActivityEntry(21, "TUE", listOf(ActivityType.TABLE_TENNIS, ActivityType.RUNNING)),
    ActivityEntry(20, "MON", listOf()),
    ActivityEntry(19, "SUN", listOf(ActivityType.GYM, ActivityType.BADMINTON)),
    ActivityEntry(18, "SAT", listOf(ActivityType.GYM)),
    ActivityEntry(17, "FRI", listOf(ActivityType.TABLE_TENNIS)),
    ActivityEntry(16, "THU", listOf()),
    ActivityEntry(15, "WED", listOf(ActivityType.GYM, ActivityType.RUNNING)),
    ActivityEntry(14, "TUE", listOf(ActivityType.BADMINTON)),
    ActivityEntry(13, "MON", listOf(ActivityType.GYM)),
)
