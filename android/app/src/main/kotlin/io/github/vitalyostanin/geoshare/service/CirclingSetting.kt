package io.github.vitalyostanin.geoshare.service

import com.google.firebase.firestore.DocumentSnapshot
import io.github.vitalyostanin.geoshare.collection.CircleLimits
import io.github.vitalyostanin.geoshare.collection.circleLimitsStand
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.project.GroupConnection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
fun circlingLimits(connection: GroupConnection): Flow<CircleLimits> =
    circlingSetting(connection).liveSnapshots().map { limitsOf(it) }.comingBack("circling setting")

/** The document read as limits, falling back to the build's own. */
private fun limitsOf(snapshot: DocumentSnapshot?): CircleLimits {
    if (snapshot == null || !snapshot.exists()) {
        return CircleLimits()
    }

    return limitsOf(snapshot.data.orEmpty())
}

/**
 * The fields read as limits. A field that is missing, or typed by hand in the
 * console as something other than a number, keeps the build's value, and the
 * whole set is then weighed as it stands.
 */
internal fun limitsOf(fields: Map<String, Any?>): CircleLimits {
    val defaults = CircleLimits()
    val number = { field: String -> numberIn(fields, field) }
    val asked =
        CircleLimits(
            answers = number(CirclingField.ANSWERS)?.toInt() ?: defaults.answers,
            within = number(CirclingField.WITHIN_SECONDS)?.let(::asDuration) ?: defaults.within,
            roundness = number(CirclingField.ROUNDNESS) ?: defaults.roundness,
            widestMetres = number(CirclingField.WIDEST_METRES) ?: defaults.widestMetres,
            tightestMetres = number(CirclingField.TIGHTEST_METRES) ?: defaults.tightestMetres,
            steadiness = number(CirclingField.STEADINESS) ?: defaults.steadiness,
            turnedDegrees = number(CirclingField.TURNED_DEGREES) ?: defaults.turnedDegrees,
            slowestMps = number(CirclingField.SLOWEST_MPS) ?: defaults.slowestMps,
        )

    if (!circleLimitsStand(asked)) {
        AppLog.w(TAG, "the circling setting does not stand as limits and was left out")

        return defaults
    }

    if (asked != defaults) {
        AppLog.i(TAG, "the group has narrowed what counts as going round in circles")
    }

    return asked
}

/** Seconds as the duration the rule is weighed in. */
private fun asDuration(seconds: Double): Duration =
    Duration.ofMillis((seconds * MILLIS_IN_A_SECOND).toLong())

private const val MILLIS_IN_A_SECOND = 1_000.0
