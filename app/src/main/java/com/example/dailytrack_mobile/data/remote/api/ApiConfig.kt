package com.example.dailytrack_mobile.data.remote.api

import com.example.dailytrack_mobile.BuildConfig

/**
 * Centralized API configuration.
 * All requests are authenticated dynamically using Google Firebase Auth JWT tokens.
 */
object ApiConfig {
    val BASE_URL = BuildConfig.BASE_URL
}
