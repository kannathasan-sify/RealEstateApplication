package com.realestate.app.di

import com.realestate.app.data.api.ApiClient
import com.realestate.app.data.api.ApiService
import com.realestate.app.data.api.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideApiService(authInterceptor: AuthInterceptor): ApiService =
        ApiClient.create(authInterceptor)
}
