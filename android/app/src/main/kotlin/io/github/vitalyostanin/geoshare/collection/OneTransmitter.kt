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
    /** Whether the other band has been silent for the whole of the run. */
    val otherBandSilent: Boolean = false,
)

/**
 * Whether the sky is one transmitter's work.
 *
 * Both halves are required, and the measurements are why. Over the recorded
 * logs the silence of the other band alone fires on a tenth to a quarter of
 * the honest morning's windows — a phone indoors loses a band for minutes at a
 * time — and the flat sky alone never fires on an honest record but says
 * nothing about where the false signal comes from. Together they fired on two
 * fifths of the windows of the worst hours under the transmitter and on none
 * of the honest ones.
 */
fun fromOneTransmitter(says: SkySays): Boolean = says.shape == SkyShape.Flat && says.otherBandSilent
