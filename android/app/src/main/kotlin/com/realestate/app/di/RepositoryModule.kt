package com.realestate.app.di

import android.content.Context
import com.realestate.app.data.api.ApiService
import com.realestate.app.data.local.DataStoreManager
import com.realestate.app.data.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides @Singleton
    fun provideAuthRepository(api: ApiService, ds: DataStoreManager) = AuthRepository(api, ds)

    @Provides @Singleton
    fun providePropertyRepository(@ApplicationContext context: Context, api: ApiService) = PropertyRepository(context, api)

    @Provides @Singleton
    fun provideBookingRepository(api: ApiService) = BookingRepository(api)

    @Provides @Singleton
    fun provideUserRepository(api: ApiService) = UserRepository(api)
}
