package io.github.vitalyostanin.geoshare.collection

import java.time.Duration
import java.time.Instant

// When to stop asking the satellites and ask the other radios instead.
//
// A phone inside a spoofed area is not silent — it is answered promptly, and
// every answer is a lie. The filter catches the lies, and the phone then says
// nothing at all: no anchor is ever set, so nothing is written, and the card
// goes grey on everybody else's map while the service runs perfectly. The
// towers and the Wi-Fi around the phone are not fed by the spoofer, and an
// answer from them a kilometre wide is worth more than an exact answer from
// seventy kilometres away.

/**
 * How many answers in a row have to be left out before the other radios are
 * asked.
 *
 * Three, because one or two dropped answers are ordinary — a fix taken as the
 * phone comes indoors, a width that widens for a moment — while three in a row
 * is the satellites no longer being a source of positions.
 */
const val JUMPS_BEFORE_THE_OTHER_RADIOS: Int = 3

/**
 * The least time between two such questions.
 *
 * Asking costs a Wi-Fi scan, and the spoofing lasts hours: without a floor the
 * phone would scan on every fix for as long as it goes on.
 */
val OTHER_RADIOS_ASKED_EVERY: Duration = Duration.ofMinutes(2)

/** How old an answer from the towers may be and still stand for now. */
val NETWORK_ANSWER_STALE: Duration = Duration.ofMinutes(5)

/**
 * The width past which even a true answer places the phone nowhere useful.
 *
 * Far wider than [USELESS_ACCURACY_METRES], and deliberately: that threshold
 * asks whether an answer is a position, this one asks whether an answer is
 * better than nothing at all. Five kilometres is a district — enough to tell a
 * member at home from a member across the city, which is the question the map
 * is actually asked.
 */
const val NETWORK_WIDTH_LIMIT_METRES: Double = 5_000.0

/**
 * Whether the phone should put its satellites aside and ask the towers.
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
 * reach the towers, but a phone that has been fed a false position for an hour
 * may have handed it to the platform's own store of last known places, and
 * that store is what answers when the towers are slow.
 */
fun networkAnswerStands(
    fix: Fix?,
    now: Instant,
    falsePlaces: List<FalsePlace> = emptyList(),
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

    return Duration.between(fix.takenAt, now) <= NETWORK_ANSWER_STALE
}

/**
 * Whether an answer from the other radios says more than the position already
 * written.
 *
 * It is asked because the answers those radios give are not one thing. The
 * Wi-Fi around a phone places it in the flat to within fifty metres; a tower
 * places it in the district to within two kilometres; and whichever of them
 * answers first is the one that arrives. Writing every answer as it comes
 * means a phone standing still walking half a kilometre every few minutes,
 * with the width growing each time — which is what was watched happening, a
 * position sixty metres wide replaced by one of one thousand six hundred and
 * sixty.
 *
 * So an answer is written over a fresh one only where it adds something: where
 * it is no wider, or where it names a place outside the doubt of the one
 * already written, which is a phone that has moved rather than a radio with a
 * poorer answer about the same place. Anything already stale is replaced
 * whatever it says — an old exact answer is not better than a new vague one,
 * it is merely older.
 */
fun saysMoreThan(asked: Fix, written: Fix?, now: Instant): Boolean {
    if (written == null || Duration.between(written.takenAt, now) > NETWORK_ANSWER_STALE) {
        return true
    }

    val wasWide = written.accuracyMetres ?: 0.0
    val isWide = asked.accuracyMetres ?: 0.0

    if (isWide <= wasWide) {
        return true
    }

    return distanceMetres(written, asked) > wasWide + isWide
}
