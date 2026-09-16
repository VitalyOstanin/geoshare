package io.github.vitalyostanin.geoshare.service

import android.content.Context
import android.location.LocationManager
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.log.AppLog

/**
 * The name the receiver knows for throwing its aiding data away.
 *
 * Not a constant of the platform: the commands a provider answers to are the
 * receiver's own, and this one is the name every Android receiver has carried
 * since the interface was written. A receiver that does not know it answers
 * false, which is the whole of what this can be told.
 */
private const val DELETE_AIDING_DATA = "delete_aiding_data"

private const val TAG = "geoshare.aiding"

/**
 * How the last request to erase the aiding data ended.
 *
 * Three states rather than a boolean, because "not asked yet" and "asked and
 * refused" are what the row on the screen has to tell apart.
 */
enum class AidingErasure {
    /** Nothing has been asked of the receiver on this screen. */
    Untouched,

    /** The receiver took the command. */
    Erased,

    /** The receiver answered no, or there is no receiver to ask. */
    Refused,
}

/**
 * Throw away the orbits and the past positions the receiver searches by.
 *
 * The receiver keeps the orbits it has decoded or been handed over the network
 * and the place it last believed it was, and starts every search from them. It
 * has no way of telling data that has gone wrong from data that is merely old:
 * a constellation whose aiding data is corrupt is heard satellite by satellite
 * and still left out of every answer, for days, until something erases it.
 * This is that something, and it is the one lever the phone has over it —
 * every constellation loses its aiding data together, since the command takes
 * no argument saying which.
 *
 * The price is a cold start: the receiver searches the whole sky again, the
 * first position takes minutes instead of seconds, and the data is gathered
 * back over the quarter of an hour that follows.
 */
fun forgetAidingData(context: Context): AidingErasure {
    val manager = context.getSystemService(LocationManager::class.java)

    if (manager == null) {
        AppLog.w(TAG, "the aiding data was left alone: this phone has no location manager")

        return AidingErasure.Refused
    }

    val taken =
        runCatching {
            manager.sendExtraCommand(LocationManager.GPS_PROVIDER, DELETE_AIDING_DATA, null)
        }
            .onFailure { failure -> AppLog.w(TAG, "the receiver threw the command out", failure) }
            .getOrDefault(false)

    if (taken) {
        AppLog.i(TAG, "the receiver has thrown its aiding data away")
    } else {
        AppLog.w(TAG, "the receiver would not throw its aiding data away")
    }

    return if (taken) AidingErasure.Erased else AidingErasure.Refused
}

/**
 * What the row says after the receiver has answered, and nothing before that.
 */
fun aidingErasureNote(state: AidingErasure): Int? = when (state) {
    AidingErasure.Untouched -> null
    AidingErasure.Erased -> R.string.aiding_erased
    AidingErasure.Refused -> R.string.aiding_refused
}
