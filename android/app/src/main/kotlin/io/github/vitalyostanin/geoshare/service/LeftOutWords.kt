package io.github.vitalyostanin.geoshare.service

import io.github.vitalyostanin.geoshare.collection.CircleLimits
import io.github.vitalyostanin.geoshare.collection.Constellation
import io.github.vitalyostanin.geoshare.collection.Counted
import io.github.vitalyostanin.geoshare.collection.FalsePlace
import io.github.vitalyostanin.geoshare.collection.Fix
import io.github.vitalyostanin.geoshare.collection.JumpReason
import io.github.vitalyostanin.geoshare.collection.SkyDrift
import io.github.vitalyostanin.geoshare.collection.circleDrawn
import io.github.vitalyostanin.geoshare.collection.falsePlaceFor
import io.github.vitalyostanin.geoshare.collection.makeupOf
import io.github.vitalyostanin.geoshare.collection.routeRepeated
import io.github.vitalyostanin.geoshare.collection.secondOpinion
import io.github.vitalyostanin.geoshare.collection.word
import java.time.Duration
import java.time.Instant
import java.util.Locale

/**
 * What the filter was looking at the moment it caught a jump, gathered in one
 * place for the sentences below.
 *
 * [radio] is passed whole rather than as the answers read out of it: those
 * answers are asked for only by the sentence that ends up needing them, same
 * as before this was split out of [SharingService].
 */
internal data class LeftOutEvidence(
    val radio: RadioWatch,
    val restsOn: Map<Constellation, Counted>,
    val recentAnswers: List<Fix>,
    val circleLimits: CircleLimits,
    val falsePlaces: List<FalsePlace>,
    val networkSaid: Fix?,
    val awakeFor: Duration,
)

/** Says which of the rules caught the fix, in the words of the log. */
internal fun caughtBy(jump: JumpReason, fix: Fix, evidence: LeftOutEvidence): String = when (jump) {
    JumpReason.Width -> "by the width of the answer"

    JumpReason.Speed -> "by the speed it implies"

    JumpReason.Contradiction -> "by two places at one time"

    JumpReason.FalsePlace -> "by landing on ${theFalsePlace(fix, evidence)}"

    JumpReason.Unmoved -> "by the device never having been carried there"

    JumpReason.Drifted -> "by the answer walking away from a device nobody touched"

    JumpReason.Hurried ->
        "by the speed it claims for a device nobody touched: ${theSpeedClaimed(fix)}"

    JumpReason.Contradicted ->
        "by the other radios placing the device ${apartFromTheRadios(fix, evidence)}"

    JumpReason.Circling -> "by the shape of the road: ${theCircleDrawn(evidence)}"

    JumpReason.Repeating -> "by the road being gone round again: ${theRouteRepeated(evidence)}"

    JumpReason.OneBand -> "by what it rests on: ${theBandItRestsOn(evidence)}"

    JumpReason.OneTransmitter -> "by the sky it was built out of: ${theSkyItCameFrom(evidence)}"

    JumpReason.Settling -> "by the receiver still coming up: ${theReceiverSettling(fix, evidence)}"

    JumpReason.TimeApart -> "by the satellites' own time: ${theClockThatStepped(evidence)}"

    JumpReason.OffOrbit -> "by the published orbits: ${evidence.radio.orbitsSaying() ?: "no epoch"}"

    JumpReason.Saved -> "by the time it carries: a place saved at ${fix.takenAt}"
}

/**
 * What speed the provider claimed, for the line that says a fix was left
 * out because a device nobody touched was said to be travelling.
 */
private fun theSpeedClaimed(fix: Fix): String {
    val speed = fix.reported?.speedMps ?: return "no speed given"

    return String.format(Locale.ROOT, "%.1f m/s", speed)
}

/**
 * Which signal's time left the device's clock and by how much, for the line
 * that says a fix was left out.
 *
 * Both differences are written, against the receiver's clock and against
 * the count since the boot, because they say different things: the first
 * can be moved by the same signal that moved the time, and the second
 * cannot be reached at all. See SatelliteClock.kt.
 */
private fun theClockThatStepped(evidence: LeftOutEvidence): String {
    val stepped = evidence.radio.clockThatStepped() ?: return "no signal named"

    return String.format(
        Locale.ROOT,
        "%s %d, %d ns from the receiver's clock and %d ns from the device's own",
        stepped.signal.constellation.word,
        stepped.signal.id,
        stepped.apart.receiverNanos,
        stepped.apart.sinceBootNanos,
    )
}

/**
 * How long the device had been awake and how much of the sky its answer
 * rests on, for the line that says an answer of the first minutes was left
 * out.
 *
 * Both numbers, because either alone reads as the wrong complaint: the
 * seconds on their own say a device was restarted, which is not a fault,
 * and the count on its own says a receiver hears nothing, which is what a
 * device indoors does all day.
 */
private fun theReceiverSettling(fix: Fix, evidence: LeftOutEvidence): String = String.format(
    Locale.ROOT,
    "%d s since the boot, %d satellites in the fix",
    fix.sinceBoot?.seconds ?: 0L,
    evidence.restsOn.values.sumOf { it.usedInFix },
)

/**
 * Which systems the fix was built out of, for the line that says a fix was
 * left out.
 */
private fun theBandItRestsOn(evidence: LeftOutEvidence): String {
    val makeup = makeupOf(evidence.restsOn)

    return String.format(
        Locale.ROOT,
        "%d satellites of the GPS band and none of the %d GLONASS heard",
        makeup.bandInFix,
        makeup.glonassHeard,
    )
}

/**
 * Why the sky reads as one transmitter's, for the line that says a fix was
 * left out.
 */
private fun theSkyItCameFrom(evidence: LeftOutEvidence): String {
    val says = evidence.radio.skySays()
    val alongside =
        listOfNotNull(
            "the far band silent".takeIf { says.otherBandSilent },
            "the satellites moving as one block".takeIf { says.drift == SkyDrift.Together },
        )

    return String.format(
        Locale.ROOT,
        "the loudness %s and %s",
        says.shape.toString().lowercase(),
        alongside.joinToString(" and "),
    )
}

/**
 * The route the last answers are going round again, for the line that
 * says a fix was left out.
 */
private fun theRouteRepeated(evidence: LeftOutEvidence): String {
    val round = routeRepeated(evidence.recentAnswers) ?: return "a route traced again"

    return String.format(
        Locale.ROOT,
        "a route %.0f m across every %d s, within %.0f m of the round before",
        round.reachMetres,
        round.period.seconds,
        round.apartMetres,
    )
}

/**
 * The circle the last answers are drawing, for the line that says a fix
 * was left out.
 */
private fun theCircleDrawn(evidence: LeftOutEvidence): String {
    val circle =
        circleDrawn(evidence.recentAnswers, evidence.circleLimits)
            ?: return "a circle at a steady speed"

    return String.format(
        Locale.ROOT,
        "a circle of %.0f m at a steady %.0f m/s, %.0f degrees of it so far",
        circle.radiusMetres,
        circle.speedMps,
        circle.turnedDegrees,
    )
}

/** What the group calls the place this fix landed on. */
private fun theFalsePlace(fix: Fix, evidence: LeftOutEvidence): String =
    falsePlaceFor(fix, evidence.falsePlaces)?.named ?: "a place a false signal uses"

/**
 * How far the towers and the Wi-Fi put the device from where the satellites
 * just put it, for the line that says a fix was left out.
 */
private fun apartFromTheRadios(fix: Fix, evidence: LeftOutEvidence): String {
    val said =
        secondOpinion(fix, evidence.networkSaid, Instant.now(), evidence.awakeFor)
            ?: return "somewhere else"

    return String.format(
        Locale.ROOT,
        "%.0f m away, against the %.0f m they were allowed",
        said.apartMetres,
        said.allowedMetres,
    )
}
