package com.realestate.app.data.api

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * App-wide event bus for authentication events.
 *
 * [AuthInterceptor] emits [AuthEvent.SessionExpired] whenever ANY API call
 * receives a 401 Unauthorized response. [AppNavGraph] collects this event and
 * navigates the user back to the Login screen, clearing the back-stack.
 *
 * Using a SharedFlow with replay=0 means each event is consumed only once —
 * no stale "session expired" pop-up shown the next time the user opens the app.
 */
object AuthEventBus {
    private val _events = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    suspend fun emit(event: AuthEvent) = _events.emit(event)
}

sealed class AuthEvent {
    /** Emitted when a 401 response is received — token is expired or invalid. */
    object SessionExpired : AuthEvent()
}
