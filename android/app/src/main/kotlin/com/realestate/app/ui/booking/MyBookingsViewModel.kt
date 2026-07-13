package com.realestate.app.ui.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realestate.app.data.mock.MockData
import com.realestate.app.data.models.Booking
import com.realestate.app.data.models.Property
import com.realestate.app.data.repository.BookingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Booking enriched with the matching [Property] so the list screen can show full details. */
data class BookingWithProperty(
    val booking:  Booking,
    val property: Property?,
)

@HiltViewModel
class MyBookingsViewModel @Inject constructor(
    private val repo: BookingRepository,
) : ViewModel() {

    private val _bookings  = MutableStateFlow<List<BookingWithProperty>>(emptyList())
    val bookings: StateFlow<List<BookingWithProperty>> = _bookings

    private val _ownerBookings  = MutableStateFlow<List<BookingWithProperty>>(emptyList())
    val ownerBookings: StateFlow<List<BookingWithProperty>> = _ownerBookings

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init { load() }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            
            // 1. Load user's bookings (Buyer view)
            repo.getBookings()
                .onSuccess { list ->
                    _bookings.value = list.map { booking ->
                        BookingWithProperty(
                            booking  = booking,
                            property = MockData.getById(booking.propertyId),
                        )
                    }
                }
                .onFailure {
                    _bookings.value = emptyList()
                }

            // 2. Load bookings received for user's properties (Owner/Agent view)
            repo.getOwnerBookings()
                .onSuccess { list ->
                    _ownerBookings.value = list.map { booking ->
                        BookingWithProperty(
                            booking  = booking,
                            property = MockData.getById(booking.propertyId),
                        )
                    }
                }
                .onFailure {
                    _ownerBookings.value = emptyList()
                }

            _isLoading.value = false
        }
    }

    fun cancelBooking(id: String) {
        viewModelScope.launch {
            repo.cancelBooking(id)
            load()
        }
    }

    fun updateBookingStatus(id: String, status: String) {
        viewModelScope.launch {
            repo.updateBookingStatus(id, status)
            load()
        }
    }
}
