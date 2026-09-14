package io.github.vitalyostanin.geoshare.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import io.github.vitalyostanin.geoshare.collection.Fix
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * How often the satellites are asked directly.
 *
 * They answer about once a second, which is more than this has any use for:
 * the question is whether the satellites and the written position are in the
 * same place, and that does not change in a second. Every answer also costs a
 * row in the sky file.
 */
private const val SATELLITES_ASKED_EVERY_MILLIS = 5_000L

/**
 * Positions from the satellites and nothing else.
 *
 * The fused provider answers with whichever source it thinks best and will not
 * say which it was: a position it hands over may have come from the sky or
 * from the Wi-Fi in the building, and the two are worth entirely different
 * amounts when the question is whether the sky is lying. The platform's own
 * satellite provider has no such discretion — it answers from the receiver or
 * it does not answer.
 *
 * Collected only while the receiver is running for the fused request anyway.
 * Asking for it in a mode that had let the receiver sleep would buy the same
 * comparison at the price the whole collection policy exists to avoid.
 */
@SuppressLint("MissingPermission")
fun satellitePositions(context: Context): Flow<Fix> = callbackFlow {
    val manager = context.getSystemService(LocationManager::class.java)

    if (manager == null || !manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        close()

        return@callbackFlow
    }

    val listener =
        object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(fixOf(location))
            }

            // Abstract until Android 11; a provider that is switched off while
            // the phone is listening simply stops answering.
            override fun onProviderDisabled(provider: String) = Unit

            override fun onProviderEnabled(provider: String) = Unit

            @Deprecated("Kept because the platform still declares it below Android 11.")
            override fun onStatusChanged(provider: String, status: Int, extras: Bundle?) = Unit
        }

    manager.requestLocationUpdates(
        LocationManager.GPS_PROVIDER,
        SATELLITES_ASKED_EVERY_MILLIS,
        0f,
        listener,
        Looper.getMainLooper(),
    )

    awaitClose { manager.removeUpdates(listener) }
}
