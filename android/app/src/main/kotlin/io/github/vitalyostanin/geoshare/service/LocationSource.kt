package io.github.vitalyostanin.geoshare.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Build
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import io.github.vitalyostanin.geoshare.collection.Fix
import io.github.vitalyostanin.geoshare.log.AppLog
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant

private const val TAG = "geoshare.location"

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
                    noteIfPretended(location)

                    trySend(
                        Fix(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            takenAt = Instant.ofEpochMilli(location.time),
                            accuracyMetres = location.accuracy.toDouble(),
                            from = location.provider,
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

    location?.let(::noteIfPretended)

    return location?.let {
        Fix(
            latitude = it.latitude,
            longitude = it.longitude,
            takenAt = Instant.ofEpochMilli(it.time),
            accuracyMetres = it.accuracy.toDouble(),
            from = it.provider,
        )
    }
}

/**
 * Says so when a position was put there by another application.
 *
 * A different thing from the false signal the filters are about: this is the
 * phone being told where it is by software on it, which anybody can turn on in
 * the developer settings and which no radio can do. It costs one field to
 * check and it is the only kind of pretence the platform will admit to.
 *
 * Written down rather than acted on. A member who has a mock provider running
 * knows they have one, and a marker that quietly stopped moving would be worse
 * than a marker that says where the phone claims to be.
 */
private fun noteIfPretended(location: Location) {
    val pretended =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            location.isMock
        } else {
            @Suppress("DEPRECATION")
            location.isFromMockProvider
        }

    if (pretended) {
        AppLog.w(TAG, "this position was put there by another application, not by the radio")
    }
}
