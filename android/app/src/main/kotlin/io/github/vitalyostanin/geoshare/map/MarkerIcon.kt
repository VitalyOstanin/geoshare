package io.github.vitalyostanin.geoshare.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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

/** The label above the dot: its text, the box around it and the gap down to the dot. */
private const val LABEL_TEXT_DP = 13f
private const val LABEL_PADDING_DP = 5f
private const val LABEL_GAP_DP = 4f
private const val LABEL_CORNER_DP = 6f
private val LABEL_BACKGROUND = Color.argb(0xEB, 0xFF, 0xFF, 0xFF)
private val LABEL_TEXT_COLOUR = Color.rgb(0x20, 0x20, 0x20)

/**
 * How far the middle of the dot sits above the bottom of the drawing.
 *
 * The layer hangs the drawing by its bottom edge, and the member is at the dot
 * rather than at that edge; this is the difference. It follows from the size
 * of the dot alone and not from the text, so the layer states it once instead
 * of carrying it on every point.
 *
 * The number is in the units a style offset is written in, and those are the
 * same length as a density-independent pixel: the drawing is made at the
 * screen's density and handed over carrying it.
 */
const val MARKER_ANCHOR_DP = (MARKER_DIAMETER_DP + MARKER_BORDER_DP * 2) / 2f

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
 * A named dot rather than a pin.
 *
 * Drawn here instead of shipped as a resource because what varies is the
 * colour and the name, and one drawing routine is less to keep in step than a
 * file per member plus a grey copy of each. The name is always up, the way a
 * place name is on a map: a dot on its own says somebody is there, not who.
 *
 * The text is part of the drawing rather than a label of its own because the
 * background is raster tiles: a style without a glyph server has no letters to
 * set a text layer in.
 */
fun markerIcon(context: Context, name: String, colour: Int, stale: Boolean): Bitmap {
    val diameter = dpToPx(context, MARKER_DIAMETER_DP)
    val border = dpToPx(context, MARKER_BORDER_DP)
    val dotSize = diameter + border * 2
    val gap = dpToPx(context, LABEL_GAP_DP)
    val padding = dpToPx(context, LABEL_PADDING_DP)

    val textPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = LABEL_TEXT_COLOUR
            textSize = dpToPx(context, LABEL_TEXT_DP).toFloat()
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

    val metrics = textPaint.fontMetrics
    val labelHeight = (metrics.descent - metrics.ascent).toInt() + padding * 2
    val labelWidth = textPaint.measureText(name).toInt() + padding * 2
    val width = maxOf(labelWidth, dotSize)
    val height = labelHeight + gap + dotSize

    // Handed the screen's metrics rather than left at the default: the library
    // reads the density off the bitmap to work out how many of its own pixels
    // the drawing covers, and a bitmap that claims the baseline density comes
    // out as many times too large as the screen is dense.
    val bitmap =
        Bitmap.createBitmap(
            context.resources.displayMetrics,
            width,
            height,
            Bitmap.Config.ARGB_8888,
        )

    val canvas = Canvas(bitmap)
    val middle = width / 2f

    val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = LABEL_BACKGROUND }
    val corner = dpToPx(context, LABEL_CORNER_DP).toFloat()

    canvas.drawRoundRect(
        middle - labelWidth / 2f,
        0f,
        middle + labelWidth / 2f,
        labelHeight.toFloat(),
        corner,
        corner,
        boxPaint,
    )

    canvas.drawText(name, middle, padding - metrics.ascent, textPaint)

    val dotCentre = labelHeight + gap + dotSize / 2f

    val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            // The white ring is what keeps the dot visible over a dark road or
            // a green park; without it a marker can sink into the tiles.
            color = Color.WHITE
        }

    canvas.drawCircle(middle, dotCentre, dotSize / 2f, paint)

    paint.color = if (stale) STALE_COLOUR else colour

    canvas.drawCircle(middle, dotCentre, diameter / 2f, paint)

    return bitmap
}

private fun dpToPx(context: Context, dp: Float): Int = TypedValue
    .applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics)
    .toInt()
