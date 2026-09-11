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

/** The label above the dot: its text, the box around it and the gap down to the dot. */
private const val LABEL_TEXT_DP = 13f
private const val LABEL_PADDING_DP = 5f
private const val LABEL_GAP_DP = 4f
private const val LABEL_CORNER_DP = 6f
private val LABEL_BACKGROUND = Color.argb(0xEB, 0xFF, 0xFF, 0xFF)
private val LABEL_TEXT_COLOUR = Color.rgb(0x20, 0x20, 0x20)

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
 */
fun markerIcon(context: Context, name: String, colour: Int, stale: Boolean): MarkerIcon {
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

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
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

    // The dot sits on the position, and the label above it; an icon anchored
    // by its own middle would put the member half a label north of where they
    // are.
    return MarkerIcon(
        drawable = BitmapDrawable(context.resources, bitmap),
        anchorV = dotCentre / height,
    )
}

/**
 * The drawing and where its point is.
 *
 * The vertical anchor is part of the icon rather than a constant next to it:
 * it follows from the height of the label, which follows from the text size.
 */
data class MarkerIcon(val drawable: Drawable, val anchorV: Float)

private fun dpToPx(context: Context, dp: Float): Int = TypedValue
    .applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics)
    .toInt()
