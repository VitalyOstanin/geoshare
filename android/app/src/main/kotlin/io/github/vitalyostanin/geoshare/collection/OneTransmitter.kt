package io.github.vitalyostanin.geoshare.collection

// A sky that came out of one transmitter rather than out of the sky.
//
// Constellations.kt asks of a single epoch whether the fix rests on the band a
// false signal is transmitted in while the other band was there to rest on.
// That rule wants GLONASS heard, and the phone this was written against, under
// the transmitter, hears almost none: over the logs of 14.09 the receiver
// listed its GLONASS satellites and heard not one of them for six hours on
// end, while the 1575 MHz band carried a dozen at a steady thirty decibels.
//
// So the case is read the other way round here: the band that cannot be
// reached from the ground is not quiet because the sky is thin — the receiver
// holds the orbits of those satellites and expects them — it is quiet because
// something nearer is louder. On its own that still fires on an honest
// morning, where a phone indoors hears one band for a while; what settles it
// is the shape of the sky, which under a real sky is never flat. See Slant.kt.
//
// The time the signals take to arrive was measured against this and dropped.
// A transmitter that repeats one signal would hand every satellite the same
// flight time, and the spread within an epoch would collapse; the one here
// does not repeat, it writes times that agree with the position it is selling.
// Over the epochs of 14.09 marked by the rule below, the GPS spread came to
// 16.17 ms against 13.72 ms under an honest sky, and the measurements sat
// closer to the geometry of the orbit (0.12 ms off) than the honest ones did
// (0.42 ms). Nothing was left over: of 13480 measurements not one under the
// transmitter fell outside what an orbit allows.

/** How one system stood in one epoch, counted over the whole of it. */
data class Presence(
    /**
     * How many of its satellites the receiver holds an orbit for.
     *
     * Counted whether or not they were heard, which is the point: a receiver
     * that holds the orbit expects the satellite, and a satellite expected and
     * not heard is the fact the rule is read from.
     */
    val expected: Int,
    val heard: Int,
    val usedInFix: Int,
)

/** The epoch counted that way, system by system. */
fun presenceOf(sky: List<Satellite>): Map<Constellation, Presence> = sky
    .groupBy { it.constellation }
    .mapValues { (_, satellites) ->
        Presence(
            expected = satellites.count { it.hasEphemeris },
            heard = satellites.count { it.loudness > 0.0 },
            usedInFix = satellites.count { it.usedInFix },
        )
    }

/**
 * How much has to be true before one band's silence is held against it.
 *
 * Read off the logs of 14.09 rather than chosen. Under the transmitter the
 * receiver held orbits for eight to twelve GLONASS satellites and heard none
 * of them, while ten to nineteen of the 1575 MHz band were in the fix; on the
 * same phone the same morning, and on the phone across the city, GLONASS was
 * both heard and used every epoch.
 */
data class SilentBandLimits(
    /** How many GLONASS orbits the receiver has to hold for their silence to mean anything. */
    val glonassExpected: Int = 4,
    /** How much of the other band has to be heard at the same time. */
    val bandHeard: Int = 8,
    /** And how much of it the fix has to rest on. */
    val bandInFix: Int = 4,
)

/**
 * Whether the band a transmitter on the ground cannot reach has gone silent
 * while the band it transmits in carries the fix.
 *
 * One epoch. The caller reads it over several — see [SkySays] — because a
 * receiver loses a band for a few seconds often enough, and because the rule
 * this feeds leaves positions out.
 */
fun theOtherBandIsSilent(
    presence: Map<Constellation, Presence>,
    limits: SilentBandLimits = SilentBandLimits(),
): Boolean {
    val glonass = presence[Constellation.Glonass] ?: return false

    if (glonass.expected < limits.glonassExpected || glonass.heard > 0) {
        return false
    }

    val band = SHARE_THE_GPS_BAND.mapNotNull { presence[it] }

    return band.sumOf { it.heard } >= limits.bandHeard &&
        band.sumOf { it.usedInFix } >= limits.bandInFix
}

/**
 * What the last run of epochs says about the sky as a whole, in the two
 * measures that do not need an earlier position to compare against.
 *
 * Carried into the jump filter as one small value rather than as the windows
 * behind it: the filter is a pure function over an answer, and the windows
 * live with the subscriptions that fill them.
 */
data class SkySays(
    /** The shape of the loudness against the height of the satellites. */
    val shape: SkyShape = SkyShape.Unclear,
    /** Whether the satellites moved as one block over the run — see Drift.kt. */
    val drift: SkyDrift = SkyDrift.Unclear,
    /** Whether the other band has been silent for the whole of the run. */
    val otherBandSilent: Boolean = false,
    /**
     * Whether the time the satellites carry left the phone's own clock during
     * the run — see [satelliteTimeParted].
     *
     * Kept here beside the rest of what the sky says, and read on its own
     * rather than as a half of [fromOneTransmitter]: it rests on a clock that
     * cannot be reached from the ground instead of on how the sky looks, so
     * pairing it with the shape would only weaken it.
     */
    val satelliteTimeApart: Boolean = false,
)

/**
 * Whether the sky is one transmitter's work.
 *
 * A flat sky is required, and one of the two things that say the flatness is a
 * transmitter's rather than a thin sky's: the band it cannot reach gone
 * silent, or the satellites moving as one block.
 *
 * The measurements are why it is built that way. Neither of the second pair
 * fires on its own without cost — the silence of the other band alone fires on
 * a tenth to a quarter of the honest morning's windows, because a phone
 * indoors loses a band for minutes at a time — and the flat sky alone says
 * nothing about where the false signal comes from. Held against the flat sky,
 * the silence fired on two fifths of the windows of the worst hours under the
 * transmitter; the block moving together fires on half to nine tenths of them,
 * which is why it is worth having as the second way in. Neither pair fires on
 * any honest record: the hours of 14.09 when the same phone still took GLONASS
 * into the fix give none in 1366 windows, and the phone in another part of the
 * city none in 146.
 */
fun fromOneTransmitter(says: SkySays): Boolean = says.shape == SkyShape.Flat &&
    (says.otherBandSilent || says.drift == SkyDrift.Together)
