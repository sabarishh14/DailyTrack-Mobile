package com.example.dailytrack_mobile.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.dailytrack_mobile.MainActivity
import com.example.dailytrack_mobile.R

class NotificationsHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders to log daily expenses, habits, and activities"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showReminderNotification() {
        createNotificationChannel()

        val notificationOptions = listOf(
            "Time to track your day!" to "Keep your streak going by recording today's activities and expenses.",
            "Daily Check-in" to "Don't forget to update your transactions and habits for today.",
            "Stay on top of your budget" to "Take a moment to record your spends and keep your cashflow accurate.",
            "Keep the momentum going!" to "Record your physical activities and daily goals for today.",
            "Evening Review" to "Review your portfolio, daily spending, and completed activities.",
            "Close out your day" to "Log your transactions and mark off today's activities in DailyTrack."
        )

        val (title, message) = notificationOptions.random()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "dailytrack_reminders"
        private const val NOTIFICATION_ID = 2001
    }
}
