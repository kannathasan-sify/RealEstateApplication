package com.realestate.app.di

import android.content.Context
import com.realestate.app.data.api.ApiClient
import com.realestate.app.data.api.ApiService
import com.realestate.app.data.api.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /** 10 MB on-disk HTTP cache for public GETs (see ApiClient.isPubliclyCacheable). */
    private const val HTTP_CACHE_SIZE_BYTES = 10L * 1024 * 1024

    @Provides
    @Singleton
    fun provideHttpCache(@ApplicationContext context: Context): Cache =
        Cache(File(context.cacheDir, "http_cache"), HTTP_CACHE_SIZE_BYTES)

    @Provides
    @Singleton
    fun provideApiService(authInterceptor: AuthInterceptor, cache: Cache): ApiService =
        ApiClient.create(authInterceptor, cache)
}
