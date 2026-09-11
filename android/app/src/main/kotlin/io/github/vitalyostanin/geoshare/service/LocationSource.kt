package io.github.vitalyostanin.geoshare.service

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import io.github.vitalyostanin.geoshare.collection.Fix
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant

/**
 * Positions as they arrive, for as long as the flow is collected.
 *
 * The permission is checked before the service starts and again by the
 * platform when the service enters the foreground, which is why the call here
 * is marked rather than guarded a third time.
 */
@SuppressLint("MissingPermission")
fun positionUpdates(context: Context, spec: RequestSpec): Flow<Fix> = callbackFlow {
    val client = LocationServices.getFusedLocationProviderClient(context)

    val request =
        LocationRequest
            .Builder(spec.priority, spec.intervalMillis)
            .setMaxUpdateDelayMillis(spec.maxDelayMillis)
            // A first fix that is merely good enough beats a better one
            // that takes another half minute of radio time.
            .setWaitForAccurateLocation(false)
            .build()

    val callback =
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { location ->
                    trySend(
                        Fix(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            takenAt = Instant.ofEpochMilli(location.time),
                        ),
                    )
                }
            }
        }

    client.requestLocationUpdates(request, callback, Looper.getMainLooper())

    awaitClose { client.removeLocationUpdates(callback) }
}

/**
 * One position, asked for now.
 *
 * The scheduled request batches fixes, which is what keeps the radio asleep but
 * also means the first one can be three quarters of an hour late. A member who
 * has just turned sharing on should appear on the map before that.
 */
@SuppressLint("MissingPermission")
suspend fun currentPosition(context: Context, priority: Int): Fix? {
    val client = LocationServices.getFusedLocationProviderClient(context)
    val cancellation = CancellationTokenSource()

    val location =
        try {
            client.getCurrentLocation(priority, cancellation.token).await()
        } finally {
            cancellation.cancel()
        }

    return location?.let {
        Fix(
            latitude = it.latitude,
            longitude = it.longitude,
            takenAt = Instant.ofEpochMilli(it.time),
        )
    }
}
