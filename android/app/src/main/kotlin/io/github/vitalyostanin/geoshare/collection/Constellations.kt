package io.github.vitalyostanin.geoshare.collection

// Which system a satellite belongs to, and what it means that one system is
// answering differently from another.
//
// GPS, Galileo and BeiDou all put their open civil signal on 1575.42 MHz.
// GLONASS puts its own near 1602 MHz, a band of its own some twenty-five
// megahertz away. That separation is the whole reason this file exists: a
// transmitter drawing a false road is a transmitter on one band, so everything
// sharing the GPS band moves together while GLONASS carries on as it was.
//
// A phone is not told what frequency it is listening on, but it is told which
// system each satellite belongs to, and for the civil signals those are the
// same fact said differently.

/** The system a satellite belongs to, as far as this has any use for it. */
enum class Constellation {
    Gps,
    Glonass,
    Galileo,
    Beidou,

    /** Japan's regional system, which broadcasts where GPS does. */
    Qzss,

    /** The augmentation satellites, which also sit in the GPS band. */
    Sbas,

    /** India's regional system. */
    Irnss,

    /** Whatever a receiver reports that is none of these. */
    Other,
}

/**
 * The ones whose civil signal sits where GPS puts its own.
 *
 * QZSS and SBAS transmit in that band too and are deliberately left out. What
 * this set is for is an average that a false signal moves, and both of those
 * are heard on these phones a few satellites at a time or not at all: letting
 * them in would make the band's average jump about as they come and go,
 * without adding anything a false signal would show up in. They are told apart
 * in the log, which is where the question of what was heard is asked.
 */
val SHARE_THE_GPS_BAND: Set<Constellation> =
    setOf(Constellation.Gps, Constellation.Galileo, Constellation.Beidou)

/**
 * Several systems read as the one band they are transmitting in.
 *
 * Only the ones with both halves of their history are averaged: a system heard
 * for the first time a moment ago has a recent average and nothing to compare
 * it against, and letting it in would put the two halves of the answer on
 * different sets of satellites.
 */
fun acrossTheBand(parts: Collection<Epochs>): Epochs {
    val whole = parts.filter { it.moved != null }

    if (whole.isEmpty()) {
        return Epochs(recent = null, earlier = null)
    }

    return Epochs(
        recent = whole.mapNotNull { it.recent }.average(),
        earlier = whole.mapNotNull { it.earlier }.average(),
    )
}

/**
 * What it means that the two bands are not saying the same thing.
 *
 * The sky is one sky: a phone taken out of a pocket hears every system get
 * louder, and a phone put on a car floor hears every system fade. When one
 * band moves and the other does not, the cause is not the sky but something
 * transmitting on that band.
 *
 * Which way it moved says which of the two it is, the same way the gain and
 * the noise do together: a band gone loud is being answered by something
 * close, and a band gone quiet is being buried.
 *
 * Nothing is said unless both bands have a history to compare, because the
 * whole statement is a comparison — a phone that hears no GLONASS at all is a
 * phone with one band, and one band cannot disagree with anything.
 */
fun whatTheBandsSay(sharedBand: Epochs, glonass: Epochs): Interference? {
    val shared = sharedBand.moved ?: return null
    val apart = glonass.moved ?: return null

    if (shared >= NOISE_MOVE_DB && apart < NOISE_MOVE_DB) {
        return Interference.Spoofing
    }

    if (shared <= -NOISE_MOVE_DB && apart > -NOISE_MOVE_DB) {
        return Interference.Jamming
    }

    return null
}

/**
 * How much has to be true before a fix is called one band's work.
 *
 * Both numbers are read off one phone's own logs rather than chosen: on the
 * Huawei, over two hundred and twenty-two fixes, the true ones rested on four
 * and a half GLONASS satellites on average and almost never on GPS, and the
 * false ones on nine GPS satellites and almost never on GLONASS. What the
 * limits have to do is separate those two without calling a thin sky a lie.
 */
data class OneBandLimits(
    /**
     * How many GLONASS satellites have to be heard before their absence from
     * the fix says anything.
     *
     * Three. Fewer than that is a receiver that could not have built on them
     * anyway, and a rule that fired there would be reading the sky rather than
     * the fix. It is also what keeps the rule silent on the HONOR phones,
     * which hear no GLONASS at all.
     */
    val glonassHeard: Int = 3,
    /**
     * How much of the shared band the fix has to rest on before the fix is
     * held to be its work.
     *
     * Four, which is the fewest satellites a position can be built out of at
     * all. Below it the receiver was resting on something else as well, and
     * the false fixes this is written for rested on nine.
     */
    val bandInFix: Int = 4,
)

/** What a fix rests on, in the numbers the one-band rule is read from. */
data class FixMakeup(
    /** How many of the shared band's satellites the fix rests on. */
    val bandInFix: Int,
    val glonassHeard: Int,
    val glonassInFix: Int,
)

/** The same counts as [counted] gives them, read as the two sides of the sky. */
fun makeupOf(counts: Map<Constellation, Counted>): FixMakeup {
    val glonass = counts[Constellation.Glonass] ?: Counted(heard = 0, usedInFix = 0)

    return FixMakeup(
        bandInFix = SHARE_THE_GPS_BAND.sumOf { counts[it]?.usedInFix ?: 0 },
        glonassHeard = glonass.heard,
        glonassInFix = glonass.usedInFix,
    )
}

/**
 * Whether the fix was built out of the band a false signal is transmitted in,
 * while the band it cannot reach was there to be built out of and was not.
 *
 * The one rule here that needs neither an earlier position nor a second radio:
 * it is read off a single epoch, so it speaks on the first false answer rather
 * than on the second. Everything else — the speed, the width, the shape of the
 * road — needs a trail to compare against, and the first false answer is
 * precisely the one that has none.
 *
 * What it rests on is that the two bands are twenty-five megahertz apart: a
 * transmitter drawing a false road transmits in one of them, and the receiver
 * has to prefer whichever satellites agree with each other. On the Huawei the
 * separation was total — while the fix rested on GLONASS the phone was at
 * home, and the moment it rested on GPS alone the phone was fifty-six
 * kilometres away — and the rule would have caught a hundred and eighteen of
 * the hundred and eighty-nine false fixes without touching a true one.
 *
 * The caveat is in what `usedInFix` is: the receiver's last status rather than
 * the fix's own working, which Android does not hand over. The two are a
 * fraction of a second apart and the caller asks only of a status that is
 * still current — see the freshness the watch enforces — but it remains a
 * statement about the epoch beside the fix rather than about the fix.
 */
fun restsOnOneBand(
    counts: Map<Constellation, Counted>,
    limits: OneBandLimits = OneBandLimits(),
): Boolean {
    val makeup = makeupOf(counts)

    if (makeup.glonassHeard < limits.glonassHeard) {
        return false
    }

    if (makeup.glonassInFix > 0) {
        return false
    }

    return makeup.bandInFix >= limits.bandInFix
}
