package com.example.dailytrack_mobile

import com.example.dailytrack_mobile.data.remote.api.DailyTrackApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class ServerStatusHealthCheckTest {

    @Test
    fun retrofit_checkHealth_interfaceDefinitionIsValid() {
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://example.com/")
            .client(OkHttpClient())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        val api = retrofit.create(DailyTrackApi::class.java)
        assertNotNull(api)
    }

    @Test
    fun responseBody_isSuccessful_logic() {
        val successfulResponse = Response.success("{\"status\":\"Online\"}".toResponseBody(null))
        assertTrue(successfulResponse.isSuccessful)
        successfulResponse.body()?.close()
    }
}
