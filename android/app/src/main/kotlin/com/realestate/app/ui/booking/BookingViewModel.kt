package com.realestate.app.ui.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realestate.app.data.models.Booking
import com.realestate.app.data.repository.BookingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookingViewModel @Inject constructor(private val repo: BookingRepository) : ViewModel() {
    val selectedDate = MutableStateFlow("")
    val selectedTime = MutableStateFlow("10:00")
    val message = MutableStateFlow("")

    private val _state = MutableStateFlow<BookingState>(BookingState.Idle)
    val state: StateFlow<BookingState> = _state

    fun book(propertyId: String) {
        viewModelScope.launch {
            if (selectedDate.value.isBlank()) {
                _state.value = BookingState.Error("Please select a date")
                return@launch
            }
            _state.value = BookingState.Loading
            repo.createBooking(
                propertyId,
                selectedDate.value,
                selectedTime.value,
                message.value.ifBlank { null }
            ).fold(
                onSuccess = { _state.value = BookingState.Success(it) },
                onFailure = { _state.value = BookingState.Error(it.message ?: "Booking failed") }
            )
        }
    }
}

sealed class BookingState {
    object Idle : BookingState()
    object Loading : BookingState()
    data class Success(val booking: Booking) : BookingState()
    data class Error(val message: String) : BookingState()
}
