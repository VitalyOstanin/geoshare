package io.github.vitalyostanin.geoshare.collection

import java.time.Duration

/**
 * The longest the phone may hear nothing before asking in a cheaper way.
 *
 * A mode names the accuracy it wants, and the provider honours it literally: in
 * the fast mode the request goes to the satellites alone, and a phone indoors
 * can then hear nothing at all — no fix, no write, and a card that quietly goes
 * grey on everybody else's map while the service is running perfectly well.
 *
 * So silence is given a deadline. Past it the phone asks once with a lower
 * accuracy, which lets in the network answer it can get from the Wi-Fi around
 * it: a hundred metres of doubt, said out loud, beats an exact answer nobody
 * ever hears.
 *
 * Twice the round of asking, because one missed round is ordinary; never less
 * than a minute, so the fast mode does not start a rescue between two fixes;
 * and never more than half of [STALE_AFTER], so that the rescue has time to
 * land before the card would go grey anyway. A mode that asks for nothing has
 * no deadline: it is not waiting to hear anything.
 */
fun rescueAfter(mode: CollectionMode): Duration? {
    val round = mode.asksAgainWithin() ?: return null
    val twice = round.multipliedBy(2)

    return twice.coerceIn(MINIMUM_SILENCE, STALE_AFTER.dividedBy(2))
}

private val MINIMUM_SILENCE: Duration = Duration.ofMinutes(1)

/**
 * What a rescue asks with.
 *
 * Coarser than the request that went quiet, which is the whole point:
 * [Accuracy.High] goes to the satellites alone, and a phone that hears nothing
 * from them is usually indoors, where the network is the one thing with an
 * answer. A mode that already asks coarsely keeps its own accuracy — below it
 * there is nothing left to try.
 */
fun rescueAccuracy(mode: CollectionMode): Accuracy =
    if (mode.accuracy == Accuracy.High) Accuracy.Balanced else mode.accuracy
