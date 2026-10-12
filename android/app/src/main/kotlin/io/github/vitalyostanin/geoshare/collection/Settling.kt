package io.github.vitalyostanin.geoshare.collection

import java.time.Duration

// The first minutes after the device was switched on.
//
// A receiver coming up has no almanac it trusts, no clock correction and no
// satellites it has locked on to, and it answers anyway: the platform hands
// over whatever it can put together, and what it puts together in that state
// is a position several hundred metres wide built out of nothing. A device left
// on a table answered twenty-three seconds after a boot with a fix a hundred
// and twenty-two metres wide and no GPS satellite in it at all, and went on to
// report moving sixty-six metres at 6.6 m/s and two hundred and twenty-three
// metres at 18.8 m/s while lying untouched there.
//
// None of the rules of Jumps.kt caught any of that, and none of them could:
// the speeds are those of somebody walking and driving, the widths are inside
// what a real fix in a courtyard looks like, and the readings that would have
// told — the shape of the sky, the gains — need a minute of epochs before they
// say anything at all. So the answers of those minutes are held to tighter
// limits instead, and the ones that pass are written down as what they are.

/**
 * How long after a boot the receiver is still treated as settling.
 *
 * Five minutes, which is where the run above stopped producing places the
 * device had never been. The window is not the whole of the reading — see
 * [stillSettling] — it is the end of it: a receiver that has found nothing in
 * five minutes is somewhere it will not find anything, indoors or under a
 * transmitter, and holding every answer it gives from then on to the tighter
 * limits would leave such a device silent for as long as it stays there.
 */
val SETTLING_AFTER_BOOT: Duration = Duration.ofMinutes(5)

/**
 * How many satellites in the answer say the receiver has settled.
 *
 * Four, which is what a position needs: three for the place and one for the
 * clock. Fewer than that is the platform putting an answer together out of
 * something else and naming the satellites as its source.
 */
const val SETTLED_SATELLITES: Int = 4

/**
 * The widest an answer may be while the receiver is still settling, in metres.
 *
 * A hundred metres is a courtyard: a fix that wide places a device at the right
 * building, which is the whole of what a member wants out of the minutes after
 * a device is switched on. The number is read off a real run rather than chosen
 * for its roundness — the first answer of that boot was a hundred and
 * twenty-two metres wide and named a place the device had never been.
 *
 * Away from this window the same rule stands at five hundred — see
 * [USELESS_ACCURACY_METRES] — because a device whose receiver is working has
 * earned the benefit of the doubt, and a device whose receiver is coming up has
 * not yet.
 */
const val SETTLING_WIDTH_METRES: Double = 100.0

/**
 * The fastest a device may appear to travel while the receiver is settling, in
 * metres a second.
 *
 * Five metres a second is eighteen kilometres an hour: a person walking or
 * running stays under it, a bicycle and a car do not. Read off the same run as
 * the width — the answers of that boot walked sixty-six metres at 6.6 m/s
 * while the device lay on a table, which is a pace nothing in the ordinary
 * rules has any reason to doubt.
 *
 * This is the deliberate cost of the window: a device restarted in a moving car
 * writes nothing until its receiver settles, and its marker stands where it
 * was for up to the five minutes above. That is the lesser harm — a marker
 * that is behind says a device is behind, and a marker moved by the answers of
 * a receiver coming up says a device is somewhere nobody has been.
 */
const val SETTLING_SPEED_MPS: Double = 5.0

/**
 * Whether the receiver is still coming up, so far as the device can tell.
 *
 * Two conditions rather than either alone, and each covers what the other
 * cannot. The count of satellites is the thing that matters — an answer built
 * out of four of them is an answer off the sky, whatever the clock says — but
 * a device indoors and a device under a transmitter both report none of them
 * for as long as they stay there, and a window that waits for four would never
 * close. The clock closes it. The other way round, a receiver that locks on in
 * forty seconds is not helped by being held under the tighter limits for the
 * remaining four minutes.
 *
 * [sinceBoot] is absent for a fix read back from the trail file, and such a
 * fix is not treated as settling: the count it would be compared against
 * belongs to a run of the process that has ended.
 */
fun stillSettling(sinceBoot: Duration?, restsOn: Map<Constellation, Counted>): Boolean {
    if (sinceBoot == null || sinceBoot >= SETTLING_AFTER_BOOT) {
        return false
    }

    return restsOn.values.sumOf { it.usedInFix } < SETTLED_SATELLITES
}

/**
 * Whether an answer of those minutes is one to leave out.
 *
 * The same two things the ordinary rules ask — how wide the answer is, and how
 * fast the device would have had to travel — asked with the limits of this
 * window. The width is asked without an anchor, which is the point of it: the
 * first answer after a boot has nothing before it to be compared with, and it
 * is the one most likely to be built out of nothing.
 *
 * The speed is asked without the allowance the ordinary rule makes for the
 * width of the two answers. Inside this window that allowance is what lets the
 * false movement through: two answers a hundred metres wide cover any step of
 * a creep between them, and a creep is exactly what a receiver coming up
 * produces.
 */
fun settlingJump(last: Fix?, next: Fix): Boolean {
    if ((next.accuracyMetres ?: 0.0) >= SETTLING_WIDTH_METRES) {
        return true
    }

    val speed = last?.let { impliedSpeedMps(it, next) } ?: return false

    return speed > SETTLING_SPEED_MPS
}
