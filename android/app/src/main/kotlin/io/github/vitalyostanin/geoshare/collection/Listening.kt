package io.github.vitalyostanin.geoshare.collection

import java.time.Duration

// Subscriptions that come back after they are refused.
//
// A subscription to the database is a standing question, and a refused one
// stops being asked: the client drops the listener and answers nothing more,
// however long the phone runs. That is what happened when the rules of the
// project did not yet know the collection of false places — all three phones
// were refused, the rules were deployed an hour later, and not one phone
// noticed until its application was started again by hand.
//
// So a refusal is treated as a pause rather than as an end. What has to be
// chosen is how long the pause is: too short and a phone whose permission is
// genuinely gone asks all day, too long and a permission handed back sits
// unused. The answer here is both — quick at first, and slower every time,
// up to a ceiling it keeps.

/** How long a refused subscription waits before it is taken out the first time. */
val LISTEN_AGAIN_AFTER: Duration = Duration.ofSeconds(5)

/**
 * The longest it ever waits between attempts.
 *
 * Five minutes. A permission handed back is normally handed back by somebody
 * who is watching for the effect, and five minutes is about as long as such a
 * person waits before deciding nothing happened.
 */
val LISTEN_AGAIN_AT_MOST: Duration = Duration.ofMinutes(5)

/**
 * How many doublings are worth counting.
 *
 * Past the ceiling the arithmetic says the same thing every time, and the
 * doubling itself would overflow long before the phone had been running that
 * long.
 */
private const val DOUBLINGS_COUNTED = 16

/**
 * How long to wait before taking a refused subscription out again, the
 * attempts counted from zero.
 *
 * Doubled each time rather than fixed: the first refusal is usually a rule
 * being deployed or an account signing in, and both are over in seconds; a
 * refusal still standing after an hour is a permission that is genuinely gone,
 * and asking about it every five seconds would cost the battery and the quota
 * for nothing.
 */
fun listenAgainAfter(
    attempt: Long,
    first: Duration = LISTEN_AGAIN_AFTER,
    atMost: Duration = LISTEN_AGAIN_AT_MOST,
): Duration {
    if (attempt >= DOUBLINGS_COUNTED) {
        return atMost
    }

    val waited = first.multipliedBy(1L shl attempt.toInt().coerceAtLeast(0))

    return if (waited > atMost) atMost else waited
}
