package com.realestate.app.data.api

import com.realestate.app.data.local.DataStoreManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val dataStoreManager: DataStoreManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { dataStoreManager.authToken.firstOrNull() }
        val request = if (!token.isNullOrBlank()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }

        val response = chain.proceed(request)

        // If the server rejects the token (expired or SECRET_KEY changed),
        // clear it from DataStore and broadcast SessionExpired so the app
        // can navigate back to the Login screen automatically.
        if (response.code == 401) {
            runBlocking {
                dataStoreManager.saveAuthToken("")   // clear stale token
                AuthEventBus.emit(AuthEvent.SessionExpired)
            }
        }

        return response
    }
}
