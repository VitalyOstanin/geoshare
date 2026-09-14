package io.github.vitalyostanin.geoshare.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether this phone's own map is in front of somebody at this moment.
 *
 * The claim written for the other members takes a round trip through the
 * database and comes back filtered out — a phone ignores its own marker, or it
 * would answer itself forever. So the one viewer that matters most, the person
 * holding this phone, was the only one the collection never heard about, and a
 * map opened here left the phone in whatever slow mode it was already in.
 *
 * Kept here rather than passed through the service's binder because the screen
 * and the collection share a process: the flow is the shortest honest path
 * between them, and it needs no network to work.
 */
object MapOnScreen {
    private val shown = MutableStateFlow(false)

    val state: StateFlow<Boolean> = shown.asStateFlow()

    fun opened() {
        shown.value = true
    }

    fun closed() {
        shown.value = false
    }
}
