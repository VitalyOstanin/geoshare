package io.github.vitalyostanin.geoshare.map

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.Instant

// A request that one device forget where it thinks it is, out of the requests
// lying in the database.
//
// The jump filter holds on to the last place it believed, and a wrong one makes
// the device refuse every honest answer after it. The device's own settings
// carry a button for that; this is the same button pressed from somebody else's
// screen. The device carries out a request newer than the last one it did and
// no older than RESET_FRESH, then says so; the asking screen watches the same
// document to tell a device that did from one not heard from since.

/** How long a request waits for its device: one switched back on later the same day still forgets. */
val RESET_FRESH: Duration = Duration.ofDays(1)

/** How long a request the device carried out is still told of on its row. */
val RESET_TOLD: Duration = Duration.ofMinutes(5)

/** One request as the device or the asking screen holds it, out of what the document says. */
data class Reset(
    val phoneId: String,
    val askedBy: String,
    val askedAt: Instant,
    val doneAt: Instant?,
)

/**
 * A document turns into a request, or into nothing: without [askedBy] or
 * [askedAt] nothing was asked, and a [phoneId] without a dot names no device.
 */
fun resetOf(phoneId: String, askedBy: String?, askedAt: Instant?, doneAt: Instant?): Reset? {
    if (askedBy == null || askedAt == null || !phoneId.contains('.')) {
        return null
    }

    return Reset(phoneId = phoneId, askedBy = askedBy, askedAt = askedAt, doneAt = doneAt)
}

/**
 * Whether the device should forget its place for [reset] at [now]: newer than
 * [lastDone] and still within [RESET_FRESH] of [Reset.askedAt].
 */
fun shouldForget(reset: Reset, lastDone: Instant?, now: Instant): Boolean =
    (lastDone == null || reset.askedAt.isAfter(lastDone)) &&
        now.isBefore(reset.askedAt.plus(RESET_FRESH))

/** What a request is doing right now, for the asking screen to show. */
sealed interface ResetState {
    /** Asked, and the device has not said it did, within [RESET_FRESH]. */
    data object Waiting : ResetState

    /** The device forgot its place at [at]. */
    data class Done(val at: Instant) : ResetState

    /** The device did not answer within [RESET_FRESH]. */
    data object NoAnswer : ResetState
}

fun resetState(reset: Reset, now: Instant): ResetState = when {
    reset.doneAt != null -> ResetState.Done(reset.doneAt)
    now.isBefore(reset.askedAt.plus(RESET_FRESH)) -> ResetState.Waiting
    else -> ResetState.NoAnswer
}

/**
 * Whether [reset] is still worth a line on its row at [now]: while the device
 * has not answered, and for [RESET_TOLD] after it did. A request nobody
 * answered is told of for [RESET_TOLD] past its [RESET_FRESH] too.
 */
fun resetTold(reset: Reset, now: Instant): Boolean {
    val endedAt = reset.doneAt ?: reset.askedAt.plus(RESET_FRESH)

    return now.isBefore(endedAt.plus(RESET_TOLD))
}

/**
 * The requests standing on [phoneIds], each followed through [resetOf], by
 * device, the way [sirensOf] follows calls: a request made from any screen
 * shows on the row. A device never asked is left out.
 */
fun resetsOf(
    phoneIds: Set<String>,
    resetOf: (phoneId: String) -> Flow<Reset?>,
): Flow<Map<String, Reset>> = if (phoneIds.isEmpty()) {
    flowOf(emptyMap())
} else {
    combine(phoneIds.map { phoneId -> resetOf(phoneId).map { phoneId to it } }) { pairs ->
        pairs.mapNotNull { (phoneId, reset) -> reset?.let { phoneId to it } }.toMap()
    }
}
