package io.github.vitalyostanin.geoshare.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import io.github.vitalyostanin.geoshare.collection.Fix
import io.github.vitalyostanin.geoshare.log.AppLog
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * One position from the towers and the Wi-Fi around the device.
 *
 * Asked of the platform rather than of Play Services, which is the whole
 * point: Play Services picks the source itself and, where the satellites are
 * jammed, goes on offering their answer or none at all. The platform lets the
 * source be named, and the one named here never looks at a satellite.
 *
 * The answer is whatever arrives first; a caller that cannot wait forever puts
 * a deadline around the call.
 */
@SuppressLint("MissingPermission")
suspend fun networkPosition(context: Context): Fix? = suspendCancellableCoroutine { waiting ->
    val manager = context.getSystemService(LocationManager::class.java)

    if (manager == null || !manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
        waiting.resume(null)

        return@suspendCancellableCoroutine
    }

    val listener =
        object : LocationListener {
            override fun onLocationChanged(location: Location) {
                manager.removeUpdates(this)

                if (waiting.isActive) {
                    waiting.resume(fixOf(location))
                }
            }

            // Abstract until Android 11, and a provider that goes away while
            // the device is waiting is an answer of its own kind: none.
            override fun onProviderDisabled(provider: String) {
                manager.removeUpdates(this)

                if (waiting.isActive) {
                    waiting.resume(null)
                }
            }

            override fun onProviderEnabled(provider: String) = Unit

            @Deprecated("Kept because the platform still declares it below Android 11.")
            override fun onStatusChanged(provider: String, status: Int, extras: Bundle?) = Unit
        }

    waiting.invokeOnCancellation { manager.removeUpdates(listener) }

    manager.requestLocationUpdates(
        LocationManager.NETWORK_PROVIDER,
        0L,
        0f,
        listener,
        Looper.getMainLooper(),
    )
}

/**
 * What the platform knows without asking anybody, or nothing when what it
 * knows did not come from the towers after all.
 *
 * The store is asked for the network provider and answers with whatever it
 * last put there, which on at least one of these devices is the satellite
 * answer itself: a position nineteen metres wide, no seconds old, sitting on
 * the very place the spoofer had been handing out. Such an answer is not a
 * second opinion, it is the first one again — and the caller writes what comes
 * back past the jump rules, so it has to be refused here rather than argued
 * with later.
 */
@SuppressLint("MissingPermission")
fun lastNetworkPosition(context: Context): Fix? {
    val manager = context.getSystemService(LocationManager::class.java) ?: return null
    val known = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) ?: return null

    if (known.provider != LocationManager.NETWORK_PROVIDER) {
        AppLog.i(TAG, "the store answered for the network with a fix from ${known.provider}")

        return null
    }

    return fixOf(known)
}

private const val TAG = "geoshare.network"
