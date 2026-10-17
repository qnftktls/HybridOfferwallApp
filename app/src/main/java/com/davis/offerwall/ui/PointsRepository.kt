package com.davis.offerwall.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory home for the user's reward balance.
 *
 * This is deliberately minimal for the first slice. Replace it with your real
 * source of truth (Room / DataStore / your backend) without touching the UI:
 * MainActivity only observes [points] as a StateFlow.
 *
 * Thread-safe: offer-completion callbacks arrive on the WebView thread,
 * UI observes on main. StateFlow handles the hand-off.
 */
object PointsRepository {

    private val _points = MutableStateFlow(0)
    val points: StateFlow<Int> = _points.asStateFlow()

    /** Add (or subtract, with a negative delta) points and return the new balance. */
    fun addPoints(delta: Int): Int {
        var newValue = 0
        _points.update { current ->
            newValue = (current + delta).coerceAtLeast(0)
            newValue
        }
        return newValue
    }

    /** Reset to zero — useful for logout or debugging. */
    fun reset() {
        _points.value = 0
    }
}
