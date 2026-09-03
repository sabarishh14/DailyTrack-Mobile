package com.example.dailytrack_mobile

import com.example.dailytrack_mobile.data.reminder.ReminderSchedulerImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ReminderSchedulerTest {

    @Test
    fun calculateNextTriggerTime_sameDayFuture_triggersToday() {
        // Monday 10:00 AM
        val now = LocalDateTime.of(2026, 9, 7, 10, 0) // 2026-09-07 is a Monday
        val targetTime = LocalTime.of(21, 0) // 9:00 PM
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)

        val triggerMillis = ReminderSchedulerImpl.calculateNextTriggerTime(targetTime, days, now)
        val triggerDateTime = Instant.ofEpochMilli(triggerMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()

        assertEquals(2026, triggerDateTime.year)
        assertEquals(9, triggerDateTime.monthValue)
        assertEquals(7, triggerDateTime.dayOfMonth)
        assertEquals(21, triggerDateTime.hour)
        assertEquals(0, triggerDateTime.minute)
    }

    @Test
    fun calculateNextTriggerTime_sameDayPast_triggersNextScheduledDay() {
        // Monday 22:00 (10:00 PM) - target 21:00 has already passed today
        val now = LocalDateTime.of(2026, 9, 7, 22, 0) // Monday
        val targetTime = LocalTime.of(21, 0)
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)

        val triggerMillis = ReminderSchedulerImpl.calculateNextTriggerTime(targetTime, days, now)
        val triggerDateTime = Instant.ofEpochMilli(triggerMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()

        // Should trigger next Wednesday, Sep 9
        assertEquals(DayOfWeek.WEDNESDAY, triggerDateTime.dayOfWeek)
        assertEquals(9, triggerDateTime.dayOfMonth)
        assertEquals(21, triggerDateTime.hour)
        assertEquals(0, triggerDateTime.minute)
    }

    @Test
    fun calculateNextTriggerTime_todayNotScheduled_triggersUpcomingScheduledDay() {
        // Tuesday 10:00 AM - Tuesday is not in schedule
        val now = LocalDateTime.of(2026, 9, 8, 10, 0) // Tuesday
        val targetTime = LocalTime.of(21, 0)
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)

        val triggerMillis = ReminderSchedulerImpl.calculateNextTriggerTime(targetTime, days, now)
        val triggerDateTime = Instant.ofEpochMilli(triggerMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()

        // Next scheduled day is Wednesday Sep 9
        assertEquals(DayOfWeek.WEDNESDAY, triggerDateTime.dayOfWeek)
        assertEquals(9, triggerDateTime.dayOfMonth)
        assertEquals(21, triggerDateTime.hour)
        assertEquals(0, triggerDateTime.minute)
    }

    @Test
    fun calculateNextTriggerTime_weekendsOnly_schedulesSaturday() {
        // Friday 23:00 - Next is Saturday
        val now = LocalDateTime.of(2026, 9, 11, 23, 0) // Friday
        val targetTime = LocalTime.of(9, 30)
        val days = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

        val triggerMillis = ReminderSchedulerImpl.calculateNextTriggerTime(targetTime, days, now)
        val triggerDateTime = Instant.ofEpochMilli(triggerMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()

        assertEquals(DayOfWeek.SATURDAY, triggerDateTime.dayOfWeek)
        assertEquals(12, triggerDateTime.dayOfMonth)
        assertEquals(9, triggerDateTime.hour)
        assertEquals(30, triggerDateTime.minute)
    }
}
