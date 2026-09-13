package io.github.vitalyostanin.geoshare.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import io.github.vitalyostanin.geoshare.collection.Fix
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.Instant
import kotlin.coroutines.resume

/**
 * One position from the towers and the Wi-Fi around the phone.
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
            // the phone is waiting is an answer of its own kind: none.
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

/** What the platform knows without asking anybody, or nothing when it knows nothing. */
@SuppressLint("MissingPermission")
fun lastNetworkPosition(context: Context): Fix? {
    val manager = context.getSystemService(LocationManager::class.java) ?: return null

    return manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let(::fixOf)
}

private fun fixOf(location: Location): Fix = Fix(
    latitude = location.latitude,
    longitude = location.longitude,
    takenAt = Instant.ofEpochMilli(location.time),
    accuracyMetres = location.accuracy.toDouble(),
)
