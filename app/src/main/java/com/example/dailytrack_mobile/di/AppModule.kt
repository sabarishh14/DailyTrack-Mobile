package com.example.dailytrack_mobile.di

import com.example.dailytrack_mobile.data.remote.api.DailyTrackApi
import com.example.dailytrack_mobile.data.repository.MoneyRepository
import com.example.dailytrack_mobile.data.repository.ActivitiesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideMoneyRepository(api: DailyTrackApi): MoneyRepository =
        MoneyRepository(api)

    @Provides
    @Singleton
    fun provideActivitiesRepository(api: DailyTrackApi): ActivitiesRepository =
        ActivitiesRepository(api)
}
