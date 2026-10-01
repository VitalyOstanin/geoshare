package io.github.vitalyostanin.geoshare.collection

import java.time.Duration
import java.time.Instant

// Two radios asked at one moment, and what it means that they disagree.
//
// Every other rule here compares a position with an earlier position, and a
// transmitter drawing a road defeats all of them at once by drawing it slowly:
// a circle a kilometre across, walked at the speed of a car, passes the speed
// rule, passes the width, passes the sensors of a phone in a moving car, and
// never leaves the false place it was drawn in.
//
// What it cannot do is reach the towers. They are laid out by an operator and
// answered from a database; the Wi-Fi around the phone is a list of names the
// spoofer did not put there. So the two radios can be asked the same question
// at the same moment, and the answer that matters is not either position but
// the distance between them: one of the two is wrong, and it is not the one
// the spoofer cannot reach.
//
// Twice on the same day this was the only rule that would have spoken. At
// 16:29 the satellites drew a circle of 1.2 km, eight kilometres from where
// the phone was; at 17:53 they placed it fifty-five kilometres away, while the
// network named the flat it was standing in.

/**
 * How many times the distance between the two answers has to exceed the doubt
 * in them before they are said to disagree.
 *
 * Three. The two radios do not measure the same thing to the same precision —
 * a tower answer is a district and a satellite answer is a doorway — and the
 * widths they report are honest about that. What is being looked for is not a
 * difference but an impossibility: two answers whose circles of doubt do not
 * come close to touching.
 */
const val SECOND_OPINION_MARGIN: Double = 3.0

/**
 * The distance below which a disagreement is not called one, whatever the
 * widths say.
 *
 * A kilometre. A tower answer that says fifty metres is not accurate to fifty
 * metres — the width of a network fix describes the database entry rather than
 * the phone — and without a floor an optimistic width of that kind would
 * accuse the satellites of lying about a street corner.
 */
const val SECOND_OPINION_FLOOR_METRES: Double = 1_000.0

/**
 * How far apart in time the two answers may stand and still be compared at
 * all.
 *
 * Five minutes, the same as an answer from the towers is allowed to be old
 * elsewhere. The gap does not have to be small because it is paid for rather
 * than ignored: whatever a phone could have travelled in it is added to what
 * the two answers are allowed to differ by, so a wide gap does not accuse the
 * satellites, it merely stops proving anything.
 */
val SECOND_OPINION_TOGETHER: Duration = NETWORK_ANSWER_STALE

/**
 * How often the other radios are asked while the satellites are answering
 * normally.
 *
 * Five minutes. Asking costs a scan of the Wi-Fi around the phone, and this is
 * the one rule that has to run while nothing at all looks wrong — a false road
 * drawn slowly breaks no other rule, so waiting for another rule to fire would
 * mean never asking. Paired with [SECOND_OPINION_TOGETHER] so that an answer
 * is normally still comparable when the next satellite fix arrives.
 */
val SECOND_OPINION_ASKED_EVERY: Duration = Duration.ofMinutes(5)

/**
 * How long the other radios may go unasked in a mode.
 *
 * Never more often than the mode itself asks. A scan of the Wi-Fi every five
 * minutes beside a position every twenty spends four times what the night
 * mode was chosen to spend, and most of what this application costs a phone
 * lying on a table is exactly those scans.
 */
fun secondOpinionEvery(mode: CollectionMode): Duration =
    maxOf(SECOND_OPINION_ASKED_EVERY, mode.interval)

/**
 * Whether the second opinion is fetched as a position arrives rather than on
 * a clock.
 *
 * In a mode that asks less often than an answer stays comparable, an answer
 * fetched on a clock is nearly always too old by the time the position it is
 * meant for arrives, and the rule would fall silent while still paying for
 * the scans.
 */
fun secondOpinionWithEachFix(mode: CollectionMode): Boolean =
    mode.interval > SECOND_OPINION_TOGETHER

/**
 * Whether a position arriving now should wait for the other radios to be
 * asked: not when the answer at hand is still comparable, and not more often
 * than [secondOpinionEvery].
 */
fun secondOpinionDue(
    mode: CollectionMode,
    held: Fix?,
    askedAt: Instant?,
    now: Instant,
    nowSinceBoot: Duration? = null,
): Boolean {
    if (held != null && since(held, now, nowSinceBoot).abs() <= SECOND_OPINION_TOGETHER) {
        return false
    }

    return askedAt == null || Duration.between(askedAt, now) >= secondOpinionEvery(mode)
}

/**
 * Whether something the phone did not measure contradicts the satellites about
 * where it is.
 *
 * What counts as that something is decided by the note rather than by the name
 * of the provider: an answer from the towers and an answer an assistance
 * service worked out from the Wi-Fi around the phone are both computed
 * somewhere the transmitter cannot reach, and the second of them arrives under
 * the name of the satellites. The one thing refused here is an answer that was
 * measured off the sky after all.
 *
 * Null rather than false wherever the question cannot be asked: nothing to
 * compare against, an answer too old to be about this moment, an answer so
 * wide it places the phone in a region rather than a place, or an answer that
 * turns out to be the satellites' own under another name. The distinction
 * matters because the caller reports what was asked as well as what was
 * answered, and a rule that has nothing to compare is not a rule that found
 * agreement.
 */
fun secondOpinion(
    satellites: Fix,
    network: Fix?,
    now: Instant,
    nowSinceBoot: Duration? = null,
): SecondOpinion? {
    if (network == null) {
        return null
    }

    // The satellites again, wearing the name of the network. The store of last
    // known places is shared, and on these phones it answers for the network
    // with whatever was last put there — which, under a false signal, is the
    // false position itself. Agreement with it would mean nothing and
    // disagreement even less.
    if (restsOnSatellites(network.reported?.alongside)) {
        return null
    }

    if ((network.accuracyMetres ?: 0.0) > NETWORK_WIDTH_LIMIT_METRES) {
        return null
    }

    val between = apart(network, satellites).abs()

    if (between > SECOND_OPINION_TOGETHER) {
        return null
    }

    if (since(network, now, nowSinceBoot).abs() > SECOND_OPINION_TOGETHER) {
        return null
    }

    val apart = distanceMetres(satellites, network)
    val doubt = (satellites.accuracyMetres ?: 0.0) + (network.accuracyMetres ?: 0.0)

    // What the phone could honestly have done between the two questions, at
    // the fastest the rules here believe anybody travels. Paid rather than
    // assumed away: it is what lets the two answers be minutes apart without
    // a phone that really did travel being called a liar for it.
    val travelled = PLAUSIBLE_SPEED_MPS * between.toMillis() / MILLIS_IN_A_SECOND
    val allowed = doubt * SECOND_OPINION_MARGIN + travelled
    val tooFar = apart > SECOND_OPINION_FLOOR_METRES && apart > allowed

    return SecondOpinion(
        apartMetres = apart,
        allowedMetres = allowed,
        disagrees = tooFar,
    )
}

/**
 * Whether something the phone did not measure puts it where the satellites say
 * it is.
 *
 * Not the opposite of [secondOpinion] disagreeing. That one looks for an
 * impossibility and grants three times the doubt before it names one, because
 * accusing the satellites of lying is a serious thing to do on the strength of
 * a database entry. This looks for the other answer landing inside the doubt
 * of the two together — a tower or a Wi-Fi service naming the same place —
 * and it is asked where a rule is about to be overruled rather than enforced.
 *
 * False wherever there is nothing to ask: no answer from the other radios, one
 * too old or too wide to mean anything, or the satellites' own answer under
 * the name of the network. Nothing to compare is not agreement.
 */
fun backedUpBy(
    satellites: Fix,
    network: Fix?,
    now: Instant,
    nowSinceBoot: Duration? = null,
): Boolean {
    if (network == null || secondOpinion(satellites, network, now, nowSinceBoot) == null) {
        return false
    }

    val doubt = (satellites.accuracyMetres ?: 0.0) + (network.accuracyMetres ?: 0.0)

    return distanceMetres(satellites, network) <= doubt
}

/**
 * What the other radios said about a satellite answer, kept in full so the log
 * can say by how much rather than merely that.
 */
data class SecondOpinion(
    /** How far the two answers are from each other. */
    val apartMetres: Double,
    /**
     * How far apart they were allowed to be: the doubt in the two of them
     * together, with a margin, plus whatever the phone could have travelled
     * between the questions.
     */
    val allowedMetres: Double,
    val disagrees: Boolean,
)

private const val MILLIS_IN_A_SECOND = 1_000.0
