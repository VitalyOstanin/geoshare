package io.github.vitalyostanin.geoshare.service

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import io.github.vitalyostanin.geoshare.collection.CircleLimits
import io.github.vitalyostanin.geoshare.collection.circleLimitsStand
import io.github.vitalyostanin.geoshare.log.AppLog
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.Duration

private const val TAG = "geoshare.circling"

/**
 * What this group counts as going round in circles.
 *
 * The build carries limits wide enough to catch a transmitter of any shape,
 * and a group that has watched one over its own neighbourhood knows more than
 * that: a circle whose size and speed are known can be recognised in fewer
 * answers and with less room for doubt. Those numbers describe the
 * transmitter rather than the application, so they live in the group's
 * settings and never in a build anybody can download.
 *
 * A document that does not stand as limits is ignored whole rather than
 * applied in part: half a set of limits is a rule nobody has reasoned about,
 * and the ones in the build are the ones that were.
 */
fun circlingLimits(store: FirebaseFirestore = FirebaseFirestore.getInstance()): Flow<CircleLimits> =
    callbackFlow {
        val subscription =
            circlingSetting(store).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)

                    return@addSnapshotListener
                }

                trySend(limitsOf(snapshot))
            }

        awaitClose { subscription.remove() }
    }.comingBack("circling setting")

/** The document read as limits, falling back to the build's own. */
private fun limitsOf(snapshot: DocumentSnapshot?): CircleLimits {
    val standing = CircleLimits()

    if (snapshot == null || !snapshot.exists()) {
        return standing
    }

    val seconds = snapshot.getDouble(CirclingField.WITHIN_SECONDS)
    val widest = snapshot.getDouble(CirclingField.WIDEST_METRES)
    val tightest = snapshot.getDouble(CirclingField.TIGHTEST_METRES)
    val turned = snapshot.getDouble(CirclingField.TURNED_DEGREES)
    val asked =
        CircleLimits(
            answers = snapshot.getLong(CirclingField.ANSWERS)?.toInt() ?: standing.answers,
            within = seconds?.let(::asDuration) ?: standing.within,
            roundness = snapshot.getDouble(CirclingField.ROUNDNESS) ?: standing.roundness,
            widestMetres = widest ?: standing.widestMetres,
            tightestMetres = tightest ?: standing.tightestMetres,
            steadiness = snapshot.getDouble(CirclingField.STEADINESS) ?: standing.steadiness,
            turnedDegrees = turned ?: standing.turnedDegrees,
            slowestMps = snapshot.getDouble(CirclingField.SLOWEST_MPS) ?: standing.slowestMps,
        )

    if (!circleLimitsStand(asked)) {
        AppLog.w(TAG, "the circling setting does not stand as limits and was left out")

        return standing
    }

    if (asked != standing) {
        AppLog.i(TAG, "the group has narrowed what counts as going round in circles")
    }

    return asked
}

/** Seconds as the duration the rule is weighed in. */
private fun asDuration(seconds: Double): Duration =
    Duration.ofMillis((seconds * MILLIS_IN_A_SECOND).toLong())

private const val MILLIS_IN_A_SECOND = 1_000.0
