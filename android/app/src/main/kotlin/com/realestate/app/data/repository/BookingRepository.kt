package com.realestate.app.data.repository

import com.realestate.app.BuildConfig
import com.realestate.app.data.api.ApiService
import com.realestate.app.data.mock.MockData
import com.realestate.app.data.models.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingRepository @Inject constructor(private val api: ApiService) {

    suspend fun getBookings(): Result<List<Booking>> =
        if (BuildConfig.USE_MOCK_DATA) {
            runCatching { MockData.getBookings() }
        } else {
            runCatching { api.getBookings() }
        }

    suspend fun createBooking(
        propertyId: String,
        date: String,
        time: String,
        message: String?,
    ): Result<Booking> =
        if (BuildConfig.USE_MOCK_DATA) {
            runCatching { MockData.createBooking(propertyId, date, time, message) }
        } else {
            runCatching { api.createBooking(BookingCreateRequest(propertyId, date, time, message)) }
        }

    suspend fun cancelBooking(id: String): Result<Unit> =
        if (BuildConfig.USE_MOCK_DATA) {
            runCatching { MockData.cancelBooking(id) }
        } else {
            runCatching { api.cancelBooking(id) }
        }

    suspend fun getOwnerBookings(): Result<List<Booking>> =
        if (BuildConfig.USE_MOCK_DATA) {
            runCatching { MockData.getBookings() }
        } else {
            runCatching { api.getOwnerBookings() }
        }

    suspend fun updateBookingStatus(id: String, status: String): Result<Booking> =
        if (BuildConfig.USE_MOCK_DATA) {
            runCatching {
                MockData.updateBookingStatus(id, status)
                MockData.getBookings().first { it.id == id }
            }
        } else {
            runCatching { api.updateBookingStatus(id, BookingStatusUpdate(status)) }
        }
}
