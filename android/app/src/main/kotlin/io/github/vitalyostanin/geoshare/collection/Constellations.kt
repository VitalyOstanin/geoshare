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
