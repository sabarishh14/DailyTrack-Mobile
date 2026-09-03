package com.example.dailytrack_mobile.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.dailytrack_mobile.data.local.reminder.ReminderManager
import com.example.dailytrack_mobile.domain.reminder.ReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderManager: ReminderManager

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val notificationsHelper = NotificationsHelper(context)

        when (action) {
            ACTION_SHOW_REMINDER -> {
                notificationsHelper.showReminderNotification()
                scope.launch {
                    val enabled = reminderManager.isReminderEnabledFlow.first()
                    if (enabled) {
                        val timeStr = reminderManager.reminderTimeFlow.first()
                        val days = reminderManager.reminderDaysFlow.first()
                        val time = LocalTime.parse(timeStr)
                        reminderScheduler.scheduleReminder(time, days)
                    }
                }
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                scope.launch {
                    val enabled = reminderManager.isReminderEnabledFlow.first()
                    if (enabled) {
                        val timeStr = reminderManager.reminderTimeFlow.first()
                        val days = reminderManager.reminderDaysFlow.first()
                        val time = LocalTime.parse(timeStr)
                        reminderScheduler.scheduleReminder(time, days)
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_SHOW_REMINDER = "com.example.dailytrack_mobile.ACTION_SHOW_REMINDER"
    }
}
