package io.github.vitalyostanin.geoshare.collection

import java.time.Duration
import java.time.Instant

// When to stop asking the satellites and ask the other radios instead.
//
// A device inside a spoofed area is not silent — it is answered promptly, and
// every answer is a lie. The filter catches the lies, and the device then says
// nothing at all: no anchor is ever set, so nothing is written, and the card
// goes grey on everybody else's map while the service runs perfectly. The
// towers and the Wi-Fi around the device are not fed by the spoofer, and an
// answer from them a kilometre wide is worth more than an exact answer from
// seventy kilometres away.

/**
 * How many answers in a row have to be left out before the other radios are
 * asked.
 *
 * Three, because one or two dropped answers are ordinary — a fix taken as the
 * device comes indoors, a width that widens for a moment — while three in a row
 * is the satellites no longer being a source of positions.
 */
const val JUMPS_BEFORE_THE_OTHER_RADIOS: Int = 3

/**
 * The least time between two such questions.
 *
 * Asking costs a Wi-Fi scan, and the spoofing lasts hours: without a floor the
 * device would scan on every fix for as long as it goes on.
 */
val OTHER_RADIOS_ASKED_EVERY: Duration = Duration.ofMinutes(2)

/** How old an answer from the towers may be and still stand for now. */
val NETWORK_ANSWER_STALE: Duration = Duration.ofMinutes(5)

/**
 * The width past which even a true answer places the device nowhere useful.
 *
 * Far wider than [USELESS_ACCURACY_METRES], and deliberately: that threshold
 * asks whether an answer is a position, this one asks whether an answer is
 * better than nothing at all. Five kilometres is a district — enough to tell a
 * member at home from a member across the city, which is the question the map
 * is actually asked.
 */
const val NETWORK_WIDTH_LIMIT_METRES: Double = 5_000.0

/**
 * Whether the device should put its satellites aside and ask the towers.
 *
 * Counted in answers left out rather than in silence: the rescue that watches
 * for silence is a different failure, one where the provider says nothing at
 * all.
 */
fun otherRadiosAreDue(jumpsInARow: Int, askedAt: Instant?, now: Instant): Boolean {
    if (jumpsInARow < JUMPS_BEFORE_THE_OTHER_RADIOS) {
        return false
    }

    if (askedAt == null) {
        return true
    }

    return Duration.between(askedAt, now) >= OTHER_RADIOS_ASKED_EVERY
}

/**
 * Whether what the other radios answered is worth writing in place of the
 * satellites.
 *
 * The same list of false places is asked about here too. A spoofer does not
 * reach the towers, but a device that has been fed a false position for an hour
 * may have handed it to the platform's own store of last known places, and
 * that store is what answers when the towers are slow.
 */
fun networkAnswerStands(
    fix: Fix?,
    now: Instant,
    falsePlaces: List<FalsePlace> = emptyList(),
    nowSinceBoot: Duration? = null,
): Boolean {
    if (fix == null) {
        return false
    }

    if (falsePlaceFor(fix, falsePlaces) != null) {
        return false
    }

    if ((fix.accuracyMetres ?: 0.0) > NETWORK_WIDTH_LIMIT_METRES) {
        return false
    }

    return since(fix, now, nowSinceBoot) <= NETWORK_ANSWER_STALE
}

/**
 * Whether an answer from the other radios says more than the position already
 * written.
 *
 * It is asked because the answers those radios give are not one thing. The
 * Wi-Fi around a device places it in the flat to within fifty metres; a tower
 * places it in the district to within two kilometres; and whichever of them
 * answers first is the one that arrives. Writing every answer as it comes
 * means a device standing still walking half a kilometre every few minutes,
 * with the width growing each time — which is what was watched happening, a
 * position sixty metres wide replaced by one of one thousand six hundred and
 * sixty.
 *
 * So an answer is written over a standing one only where it adds something:
 * where it is no wider, or where it names a place well outside the doubt of
 * the one already written, which is a device that has moved rather than a radio
 * with a poorer answer about the same place. Anything already stale is
 * replaced whatever it says — an old exact answer is not better than a new
 * vague one, it is merely older.
 *
 * Well outside rather than merely outside, by [SECOND_OPINION_MARGIN], and
 * that margin was paid for too: a device standing in its own flat at
 * twenty-eight metres was moved nearly two kilometres by a tower answering one
 * thousand seven hundred, whose circle missed the flat by a hundred and
 * fifty-six metres. A width of one thousand seven hundred is not a measurement
 * that can contradict one of twenty-eight by a hundred and fifty-six metres —
 * the width of a network answer describes a database entry rather than the
 * device, which is the same reason the second opinion grants three times the
 * doubt before it calls two answers a disagreement.
 *
 * Stale here is [STALE_AFTER] rather than [NETWORK_ANSWER_STALE], and the
 * difference was paid for: with five minutes, a device standing in its own flat
 * had its position of forty-two metres replaced, six minutes later, by a tower
 * answer of one thousand six hundred and seventy-seven, and the marker walked
 * four hundred and thirty metres. The five minutes answer a different
 * question — how old an answer from the towers may be to be used at all — and
 * a position that is still good enough to show is not one to overwrite with
 * something vaguer about the same place.
 */
fun saysMoreThan(
    asked: Fix,
    written: Fix?,
    now: Instant,
    nowSinceBoot: Duration? = null,
): Boolean {
    if (written == null || since(written, now, nowSinceBoot) > STALE_AFTER) {
        return true
    }

    val wasWide = written.accuracyMetres ?: 0.0
    val isWide = asked.accuracyMetres ?: 0.0

    if (isWide <= wasWide) {
        return true
    }

    return distanceMetres(written, asked) > (wasWide + isWide) * SECOND_OPINION_MARGIN
}
