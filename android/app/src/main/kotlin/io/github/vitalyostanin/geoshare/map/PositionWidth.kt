package io.github.vitalyostanin.geoshare.map

import kotlin.math.roundToLong

/** Where metres stop being the unit worth writing and kilometres start. */
private const val METRES_IN_A_KILOMETRE = 1_000.0

/** How a tenth of a kilometre is arrived at: hundreds of metres, then back. */
private const val METRES_IN_A_TENTH = 100.0
private const val TENTHS_IN_A_KILOMETRE = 10.0

/**
 * Below this a width is written in the calm colour, above it in the ochre one.
 *
 * A hundred metres is where an answer stops naming a building and starts naming
 * a block, which is the difference the colour is there to show. The page holds
 * the same number.
 */
const val WIDTH_WORTH_NOTICING_METRES = 100.0

/**
 * How wide an answer is, in the unit worth naming.
 *
 * A count and a unit rather than a finished string, the same way an age is
 * handled: the wording belongs to the resources, and the choice of unit can be
 * tested without them.
 */
sealed interface PositionWidth {
    data class Metres(val count: Long) : PositionWidth

    /** Kilometres to one decimal place; two would be reading the noise. */
    data class Kilometres(val count: Double) : PositionWidth
}

/**
 * The width to show for an answer the provider called this wide.
 *
 * Metres up to a kilometre and kilometres above it, because a width is read to
 * decide whether the marker means a room, a courtyard or a district, and a
 * four-digit count of metres is read one digit at a time.
 */
fun positionWidth(metres: Double): PositionWidth {
    val whole = metres.roundToLong()

    if (whole < METRES_IN_A_KILOMETRE) {
        // A width is a radius, and a radius of nought metres is a claim no
        // receiver makes. Anything that rounds down to it is the narrowest
        // answer there is rather than no answer at all.
        return PositionWidth.Metres(whole.coerceAtLeast(1))
    }

    return PositionWidth.Kilometres(
        (metres / METRES_IN_A_TENTH).roundToLong() / TENTHS_IN_A_KILOMETRE,
    )
}

/** Whether a width is wide enough to be worth saying so in the colour. */
fun widthIsWide(metres: Double): Boolean = metres >= WIDTH_WORTH_NOTICING_METRES
