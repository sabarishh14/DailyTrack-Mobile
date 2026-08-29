package com.example.dailytrack_mobile.di

import android.content.Context
import com.example.dailytrack_mobile.data.local.datastore.DemoModeManager
import com.example.dailytrack_mobile.data.local.demo.DemoDataManager
import com.example.dailytrack_mobile.data.remote.api.DailyTrackApi
import com.example.dailytrack_mobile.data.repository.ActivitiesRepository
import com.example.dailytrack_mobile.data.repository.InvestmentsRepository
import com.example.dailytrack_mobile.data.repository.MoneyRepository
import com.example.dailytrack_mobile.data.repository.SabdekhoRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDemoModeManager(@ApplicationContext context: Context): DemoModeManager =
        DemoModeManager(context)

    @Provides
    @Singleton
    fun provideDemoDataManager(
        @ApplicationContext context: Context,
        demoModeManager: DemoModeManager
    ): DemoDataManager = DemoDataManager(context, demoModeManager)

    @Provides
    @Singleton
    fun provideMoneyRepository(
        api: DailyTrackApi,
        demoDataManager: DemoDataManager
    ): MoneyRepository = MoneyRepository(api, demoDataManager)

    @Provides
    @Singleton
    fun provideActivitiesRepository(
        api: DailyTrackApi,
        demoDataManager: DemoDataManager
    ): ActivitiesRepository = ActivitiesRepository(api, demoDataManager)

    @Provides
    @Singleton
    fun provideInvestmentsRepository(
        api: DailyTrackApi,
        demoDataManager: DemoDataManager
    ): InvestmentsRepository = InvestmentsRepository(api, demoDataManager)

    @Provides
    @Singleton
    fun provideSabdekhoRepository(
        api: DailyTrackApi,
        demoDataManager: DemoDataManager
    ): SabdekhoRepository = SabdekhoRepository(api, demoDataManager)
}
