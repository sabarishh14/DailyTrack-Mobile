package com.example.dailytrack_mobile

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DailyTrackApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
