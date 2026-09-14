package io.github.vitalyostanin.geoshare.collection

/**
 * Everything else the provider said about a fix.
 *
 * None of it is decided on: the rules ask where, how wide and when, and this
 * is the rest of the answer, kept because a question asked later cannot be
 * asked of a number nobody wrote down. Google's own logger writes all of it,
 * which is the measure this is held to.
 *
 * The altitude and the speed are the two a false signal has the hardest time
 * with: a transmitter drawing a road has to draw a speed along it, and the
 * height it hands out is the one number it has no reason to get right.
 */
data class Reported(
    val altitudeMetres: Double? = null,
    val speedMps: Double? = null,
    val bearingDegrees: Double? = null,
    val speedAccuracyMps: Double? = null,
    val bearingAccuracyDegrees: Double? = null,
    val verticalAccuracyMetres: Double? = null,
    /** Whether another application put this position there; see the source. */
    val pretended: Boolean = false,
    /**
     * Whatever the provider attached beyond the fields, as it wrote it.
     *
     * On these phones the fused provider puts the satellite count and the
     * loudness it worked from in here, which is the one place the platform
     * admits how a fused answer was arrived at.
     */
    val alongside: String? = null,
)
