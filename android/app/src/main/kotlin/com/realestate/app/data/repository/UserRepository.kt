package com.realestate.app.data.repository

import com.realestate.app.data.api.ApiService
import com.realestate.app.data.api.SavedSearch
import com.realestate.app.data.models.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(private val api: ApiService) {
    suspend fun getAgencies(query: String? = null): Result<List<Agency>> = runCatching { api.getAgencies(query) }
    suspend fun getAgency(id: String): Result<Agency> = runCatching { api.getAgency(id) }
    suspend fun getSavedSearches(): Result<List<SavedSearch>> = runCatching { api.getSavedSearches() }
}
