package io.github.vitalyostanin.geoshare.map

import java.time.Duration
import java.time.Instant

// A call that rings one phone in the group, out of the sirens lying in the
// database.
//
// A phone answers a call that is newer than the last one it answered and no
// older than SIREN_FRESH, sounding for SIREN_SECONDS through the alarm
// stream; the caller's own screen watches the same document to tell an
// answered phone from one switched off, and gives up waiting after the same
// SIREN_FRESH.

/** How long a phone sounds once it starts, in seconds. */
const val SIREN_SECONDS: Long = 22

/** How long a call waits for its phone, and how long the caller's screen waits for an answer. */
val SIREN_FRESH: Duration = Duration.ofSeconds(60)

/**
 * One call as the phone or the caller's screen holds it, out of what the
 * document says.
 *
 * [memberId] is read out of [phoneId] the way a position's is, since a siren
 * is addressed the same way: `{uid}.{deviceId}`.
 */
data class Siren(
    val phoneId: String,
    val calledBy: String,
    val calledAt: Instant,
    val heardAt: Instant?,
    val stoppedBy: String?,
    val stoppedAt: Instant?,
) {
    val memberId: String get() = phoneId.substringBefore('.')
}

/**
 * A document turns into a siren, or into nothing: without [calledBy] or
 * [calledAt] there is no call to answer, and a [phoneId] without a dot names
 * no phone.
 */
fun sirenOf(
    phoneId: String,
    calledBy: String?,
    calledAt: Instant?,
    heardAt: Instant?,
    stoppedBy: String?,
    stoppedAt: Instant?,
): Siren? {
    if (calledBy == null || calledAt == null || !phoneId.contains('.')) {
        return null
    }

    return Siren(
        phoneId = phoneId,
        calledBy = calledBy,
        calledAt = calledAt,
        heardAt = heardAt,
        stoppedBy = stoppedBy,
        stoppedAt = stoppedAt,
    )
}

/**
 * Whether the phone should sound for [siren] at [now]: not stopped, newer
 * than [lastAnswered], and still within [SIREN_FRESH] of [calledAt] — a call
 * whose own clock ran a little ahead counts too, since [calledAt] is the
 * server's own stamp and the phone reads it no faster than the server wrote
 * it.
 */
fun shouldSound(siren: Siren, lastAnswered: Instant?, now: Instant): Boolean =
    siren.stoppedBy == null &&
        (lastAnswered == null || siren.calledAt.isAfter(lastAnswered)) &&
        now.isBefore(siren.calledAt.plus(SIREN_FRESH))

/** What a call is doing right now, for the caller's own screen to show. */
sealed interface SirenState {
    /** Called, and not yet heard, but still within [SIREN_FRESH]: the phone may still answer. */
    data object Waiting : SirenState

    /** Sounding until [until], which is when the phone heard it plus [SIREN_SECONDS]. */
    data class Sounding(val until: Instant) : SirenState

    /** Nobody heard it within [SIREN_FRESH]: most often a phone switched off. */
    data object NoAnswer : SirenState

    /** Stopped early, by [by] — the phone's own owner or another caller. */
    data class Stopped(val by: String) : SirenState

    /** Heard, and its [SIREN_SECONDS] have already run out. */
    data object Over : SirenState
}

/**
 * [siren]'s state at [now], in the order the document's own fields settle
 * it: a stop wins over everything else, an answer over silence, and silence
 * past [SIREN_FRESH] means nobody is coming.
 */
fun sirenState(siren: Siren, now: Instant): SirenState = when {
    siren.stoppedBy != null -> SirenState.Stopped(siren.stoppedBy)

    siren.heardAt != null -> {
        val until = siren.heardAt.plus(Duration.ofSeconds(SIREN_SECONDS))

        if (now.isBefore(until)) SirenState.Sounding(until) else SirenState.Over
    }

    now.isBefore(siren.calledAt.plus(SIREN_FRESH)) -> SirenState.Waiting

    else -> SirenState.NoAnswer
}
