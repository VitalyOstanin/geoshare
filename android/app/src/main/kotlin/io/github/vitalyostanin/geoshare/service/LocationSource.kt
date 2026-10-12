package io.github.vitalyostanin.geoshare.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import io.github.vitalyostanin.geoshare.collection.Asked
import io.github.vitalyostanin.geoshare.collection.FUSED_PROVIDER
import io.github.vitalyostanin.geoshare.collection.Fix
import io.github.vitalyostanin.geoshare.collection.GPS_PROVIDER
import io.github.vitalyostanin.geoshare.collection.NETWORK_PROVIDER
import io.github.vitalyostanin.geoshare.log.AppLog
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

private const val TAG = "geoshare.location"

/**
 * How long one of the platform's providers is given for a single answer.
 *
 * The fused provider of Play Services keeps its own deadline; the platform's
 * own ones are only asked to answer once, and a receiver indoors may never
 * do so.
 */
private const val ONE_ANSWER_MILLIS = 30_000L

/**
 * Positions as they arrive, for as long as the flow is collected.
 *
 * The permission is checked before the service starts and again by the
 * platform when the service enters the foreground, which is why the call here
 * is marked rather than guarded a third time.
 */
fun positionUpdates(context: Context, spec: RequestSpec, asked: Asked): Flow<Fix> = when (asked) {
    Asked.PlayServices -> fusedUpdates(context, spec)
    is Asked.Platform -> platformUpdates(context, spec, asked.providers)
}

@SuppressLint("MissingPermission")
private fun fusedUpdates(context: Context, spec: RequestSpec): Flow<Fix> = callbackFlow {
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

                    trySend(fixOf(location))
                }
            }
        }

    client.requestLocationUpdates(request, callback, Looper.getMainLooper())

    awaitClose { client.removeLocationUpdates(callback) }
}

/**
 * The same request put to the platform's own providers, each of them on its
 * own: the receiver and the network answer separately and both answers go
 * through the same rules.
 *
 * The quality numbers of the platform are the ones Play Services uses, so the
 * priority carries over unchanged. Batching is honoured from Android 12 on;
 * below it the platform takes the interval alone.
 */
@SuppressLint("MissingPermission")
private fun platformUpdates(
    context: Context,
    spec: RequestSpec,
    providers: List<String>,
): Flow<Fix> = callbackFlow {
    val manager = context.getSystemService(LocationManager::class.java)

    if (manager == null) {
        close()

        return@callbackFlow
    }

    val request =
        LocationRequestCompat
            .Builder(spec.intervalMillis)
            .setQuality(spec.priority)
            .setMaxUpdateDelayMillis(spec.maxDelayMillis)
            .build()

    val listener =
        LocationListenerCompat { location ->
            noteIfPretended(location)

            trySend(fixOf(location))
        }

    val executor = ContextCompat.getMainExecutor(context)

    for (provider in providers) {
        LocationManagerCompat.requestLocationUpdates(manager, provider, request, executor, listener)
    }

    AppLog.i(TAG, "asking the platform rather than Play Services: ${providers.joinToString()}")

    awaitClose { LocationManagerCompat.removeUpdates(manager, listener) }
}

/**
 * One position, asked for now.
 *
 * The scheduled request batches fixes, which is what keeps the radio asleep but
 * also means the first one can be three quarters of an hour late. A member who
 * has just turned sharing on should appear on the map before that.
 */
suspend fun currentPosition(context: Context, priority: Int, asked: Asked): Fix? = when (asked) {
    Asked.PlayServices -> fusedPosition(context, priority)
    is Asked.Platform -> platformPosition(context, asked.providers)
}

@SuppressLint("MissingPermission")
private suspend fun fusedPosition(context: Context, priority: Int): Fix? {
    val client = LocationServices.getFusedLocationProviderClient(context)
    val cancellation = CancellationTokenSource()

    val location =
        try {
            client.getCurrentLocation(priority, cancellation.token).await()
        } finally {
            cancellation.cancel()
        }

    location?.let(::noteIfPretended)

    return location?.let(::fixOf)
}

/** The first answer from the providers in their order of preference. */
private suspend fun platformPosition(context: Context, providers: List<String>): Fix? {
    val manager = context.getSystemService(LocationManager::class.java) ?: return null

    for (provider in providers) {
        val location =
            withTimeoutOrNull(ONE_ANSWER_MILLIS) { oneAnswer(context, manager, provider) }

        if (location != null) {
            noteIfPretended(location)

            return fixOf(location)
        }
    }

    return null
}

@SuppressLint("MissingPermission")
private suspend fun oneAnswer(
    context: Context,
    manager: LocationManager,
    provider: String,
): Location? = suspendCancellableCoroutine { waiting ->
    val cancellation = CancellationSignal()

    waiting.invokeOnCancellation { cancellation.cancel() }

    LocationManagerCompat.getCurrentLocation(
        manager,
        provider,
        cancellation,
        ContextCompat.getMainExecutor(context),
    ) { location ->
        if (waiting.isActive) {
            waiting.resume(location)
        }
    }
}

/**
 * The providers the platform has on this device, whether switched on or not:
 * a provider switched off now may be switched on later, and asking it costs
 * nothing until then.
 */
fun presentProviders(context: Context): Set<String> {
    val manager = context.getSystemService(LocationManager::class.java) ?: return emptySet()

    // The fused name is spelled out because the platform constant arrives only
    // with Android 12, while a blend under that name exists on older devices.
    return setOf(FUSED_PROVIDER, GPS_PROVIDER, NETWORK_PROVIDER)
        .filter { LocationManagerCompat.hasProvider(manager, it) }
        .toSet()
}

/**
 * Says so when a position was put there by another application.
 *
 * A different thing from the false signal the filters are about: this is the
 * device being told where it is by software on it, which anybody can turn on in
 * the developer settings and which no radio can do. It costs one field to
 * check and it is the only kind of pretence the platform will admit to.
 *
 * Written down rather than acted on. A member who has a mock provider running
 * knows they have one, and a marker that quietly stopped moving would be worse
 * than a marker that says where the device claims to be.
 */
private fun noteIfPretended(location: Location) {
    if (pretended(location)) {
        AppLog.w(TAG, "this position was put there by another application, not by the radio")
    }
}
