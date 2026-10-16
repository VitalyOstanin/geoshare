package io.github.vitalyostanin.geoshare.update

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * What the screens need to know about updates, as state they can observe.
 * The store holds the same facts but cannot be watched; the checks (from the
 * activity, the service and the settings button) publish here after writing
 * to it, so that the dot on the account button appears when a check finds a
 * release and goes when the member has updated.
 */
object UpdateBoard {
    /** The newer release the dot announces; null until [refresh] has read it or when there is none. */
    var found: LatestRelease? by mutableStateOf(null)

    /** Set when the notification was tapped; the map opens the settings and clears it. */
    var openRequested: Boolean by mutableStateOf(false)

    /**
     * Reads the store: a release found earlier is shown from the start, and
     * none is shown while the automatic check is off, since the member has
     * asked not to be told.
     */
    fun refresh(record: UpdateRecord, current: String) {
        found = record.found?.takeIf { record.autoCheck && isNewerVersion(it.version, current) }
    }
}
