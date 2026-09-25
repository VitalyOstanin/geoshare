package io.github.vitalyostanin.geoshare.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import io.github.vitalyostanin.geoshare.collection.Fencing
import io.github.vitalyostanin.geoshare.collection.Fix
import io.github.vitalyostanin.geoshare.collection.GEOFENCE_RADIUS_METRES
import io.github.vitalyostanin.geoshare.collection.GEOFENCE_RESPONSIVENESS
import io.github.vitalyostanin.geoshare.collection.GEOFENCE_SETTLING
import io.github.vitalyostanin.geoshare.collection.dwellingAfter
import io.github.vitalyostanin.geoshare.log.AppLog
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

private const val TAG = "geoshare.geofence"

/** Sent to this application alone; the receiver below is the only thing listening. */
private const val CROSSINGS = "io.github.vitalyostanin.geoshare.GEOFENCE_CROSSING"

/**
 * There is one fence and it is always the same one, so a new one put up under
 * this name replaces the old rather than joining it.
 */
private const val FENCE = "last-written-position"

/**
 * Whether the phone is still settled where it last said it was.
 *
 * A fence around the last written position, watched by the platform rather
 * than by us: the system knows where it is anyway, for its own reasons and for
 * every other application at once, and hears the crossing without this
 * application asking for a single fix. That is the whole point of the quiet
 * mode — a phone at home stops collecting altogether and still notices when it
 * leaves.
 *
 * The fence follows the positions this service writes, which is why the
 * centres arrive as a flow. Moving it starts the settling over: a phone that
 * has just written somewhere new is somewhere new, whatever the old fence had
 * decided about it.
 */
@SuppressLint("MissingPermission")
fun standingStill(context: Context, centres: Flow<Fix?>): Flow<Boolean> = callbackFlow {
    var dwelling = false

    send(false)

    val receiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val event = GeofencingEvent.fromIntent(intent) ?: return

                if (event.hasError()) {
                    val reason = GeofenceStatusCodes.getStatusCodeString(event.errorCode)
                    AppLog.w(TAG, "the fence could not be watched: $reason")

                    return
                }

                val crossing = crossingOf(event.geofenceTransition) ?: return

                dwelling = dwellingAfter(listOf(crossing), dwelling)
                AppLog.i(TAG, "the phone is ${if (dwelling) "settled" else "away from the fence"}")
                trySend(dwelling)
            }
        }

    // Only this application sends it, and nothing else may reach in.
    ContextCompat.registerReceiver(
        context,
        receiver,
        IntentFilter(CROSSINGS),
        ContextCompat.RECEIVER_NOT_EXPORTED,
    )

    // Mutable, because the platform fills the intent with the crossing it is
    // delivering; an immutable one would arrive empty.
    val delivery =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(CROSSINGS).setPackage(context.packageName),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    val client = LocationServices.getGeofencingClient(context)

    launch {
        centres.filterNotNull().collect { centre ->
            // The old fence's verdict does not carry over to the new one, and
            // a phone that has just moved its fence has by definition gone
            // somewhere: it starts outside until the platform says otherwise.
            dwelling = false
            trySend(false)

            client
                .addGeofences(fenceAround(centre), delivery)
                // Said out loud because nothing else marks it: a fence that
                // was never put up and one the phone has simply not crossed
                // yet look exactly alike in the log otherwise.
                .addOnSuccessListener {
                    AppLog.i(TAG, "a fence now stands around the last position")
                }
                .addOnFailureListener { error ->
                    AppLog.w(TAG, "could not put a fence around the last position", error)
                }
        }
    }

    awaitClose {
        client.removeGeofences(delivery)
        context.unregisterReceiver(receiver)
        delivery.cancel()
    }
}
    // The same answer twice changes no mode, and every one of them costs a
    // cancelled subscription and a new one.
    .distinctUntilChanged()

/** The platform's crossing in the terms this project decides modes in. */
private fun crossingOf(transition: Int): Fencing? = when (transition) {
    Geofence.GEOFENCE_TRANSITION_DWELL -> Fencing.Dwelt
    Geofence.GEOFENCE_TRANSITION_ENTER -> Fencing.Entered
    Geofence.GEOFENCE_TRANSITION_EXIT -> Fencing.Left
    else -> null
}

private fun fenceAround(centre: Fix): GeofencingRequest {
    val fence =
        Geofence
            .Builder()
            .setRequestId(FENCE)
            .setCircularRegion(
                centre.latitude,
                centre.longitude,
                GEOFENCE_RADIUS_METRES.toFloat(),
            )
            // The fence lives as long as the service watches it, and the
            // service takes it down itself; an expiry would leave a phone
            // silently collecting as if it had never been at home.
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setLoiteringDelay(GEOFENCE_SETTLING.toMillis().toInt())
            .setNotificationResponsiveness(GEOFENCE_RESPONSIVENESS.toMillis().toInt())
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_DWELL or Geofence.GEOFENCE_TRANSITION_EXIT,
            )
            .build()

    return GeofencingRequest
        .Builder()
        // The phone is inside the fence at the moment it goes up, since it was
        // drawn around the position just written. Without this nothing would
        // arrive until the phone had left and come back, and the quiet mode
        // would be reachable only by going out.
        .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL)
        .addGeofence(fence)
        .build()
}
