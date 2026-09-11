package io.github.vitalyostanin.geoshare.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.TypedValue
import kotlin.math.abs

/** The colours members are told apart by, picked so that none of them reads as grey. */
private val MEMBER_COLOURS =
    listOf(
        Color.rgb(0x1E, 0x88, 0xE5),
        Color.rgb(0x43, 0xA0, 0x47),
        Color.rgb(0xE5, 0x39, 0x35),
        Color.rgb(0x8E, 0x24, 0xAA),
        Color.rgb(0xF9, 0xA8, 0x25),
    )

/** What an old position fades to, matching the grey the list writes its age in. */
private val STALE_COLOUR = Color.rgb(0x9E, 0x9E, 0x9E)

private const val MARKER_DIAMETER_DP = 18f
private const val MARKER_BORDER_DP = 3f

/**
 * The colour of one member, the same one every time.
 *
 * Taken from the id rather than from the position in the list: the list is
 * ordered by the database and can change, and a member whose colour changes
 * under them is worse than an arbitrary colour that stays.
 */
fun memberColour(memberId: String): Int = MEMBER_COLOURS[colourIndex(memberId)]

/**
 * The index the colour is taken at, which is where the two clients have to
 * agree; the web page repeats this arithmetic.
 *
 * The remainder is taken before the sign is dropped, not after: the hash of a
 * string can be the smallest negative integer there is, and that one has no
 * positive counterpart to take.
 */
fun colourIndex(memberId: String): Int = abs(memberId.hashCode() % MEMBER_COLOURS.size)

/**
 * A dot rather than a pin.
 *
 * Drawn here instead of shipped as a resource because the only thing that
 * varies is the colour, and one drawing routine is less to keep in step than
 * a file per member plus a grey copy of each.
 */
fun markerIcon(context: Context, colour: Int, stale: Boolean): Drawable {
    val diameter = dpToPx(context, MARKER_DIAMETER_DP)
    val border = dpToPx(context, MARKER_BORDER_DP)
    val size = diameter + border * 2
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val centre = size / 2f

    val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            // The white ring is what keeps the dot visible over a dark road or
            // a green park; without it a marker can sink into the tiles.
            color = Color.WHITE
        }

    canvas.drawCircle(centre, centre, centre, paint)

    paint.color = if (stale) STALE_COLOUR else colour

    canvas.drawCircle(centre, centre, diameter / 2f, paint)

    return BitmapDrawable(context.resources, bitmap)
}

private fun dpToPx(context: Context, dp: Float): Int = TypedValue
    .applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics)
    .toInt()
