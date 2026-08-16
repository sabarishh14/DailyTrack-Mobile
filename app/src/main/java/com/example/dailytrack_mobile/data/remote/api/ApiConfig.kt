package com.example.dailytrack_mobile.data.remote.api

import com.example.dailytrack_mobile.BuildConfig

/**
 * Centralized API configuration.
 * Base URL and API key are now securely read from local.properties.
 */
object ApiConfig {
    val BASE_URL = BuildConfig.BASE_URL
    val API_KEY = BuildConfig.API_KEY
}
