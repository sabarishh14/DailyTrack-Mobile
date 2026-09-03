package com.example.dailytrack_mobile.domain.reminder

import java.time.DayOfWeek
import java.time.LocalTime

interface ReminderScheduler {
    fun scheduleReminder(time: LocalTime, days: Set<DayOfWeek>)
    fun cancelReminder()
}
