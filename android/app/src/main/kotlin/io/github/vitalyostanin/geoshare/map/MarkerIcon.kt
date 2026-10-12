package io.github.vitalyostanin.geoshare.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.util.TypedValue
import kotlin.math.abs
import kotlin.math.min

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
val STALE_COLOUR = Color.rgb(0x9E, 0x9E, 0x9E)

/**
 * The colour the group is looking for somebody in: the same red everywhere it
 * is said, on the pin, on the card and on the strip.
 */
val SOUGHT_COLOUR: Int = Color.rgb(0xC6, 0x28, 0x28)

/** How wide the ring around a sought member's head is, outside the white edge. */
private const val SOUGHT_RING_DP = 3f

/**
 * The colour a group of members wears.
 *
 * Deliberately none of the member colours: a wide pin in somebody's own colour
 * would read as that member standing there with the others tucked behind them.
 */
private val GROUP_COLOUR = Color.rgb(0x5C, 0x6B, 0xC0)

/** The head of the pin, the point below it, and the white edge around both. */
private const val PIN_HEAD_DP = 48f
private const val PIN_POINT_DP = 22f
private const val PIN_POINT_HALF_DP = 10f
private const val PIN_EDGE_DP = 2f

/**
 * How far the top of the point sits inside the head.
 *
 * The two shapes are joined into one outline, and the corners of the triangle
 * have to be inside the head for the join to leave no notch on either side.
 */
private const val PIN_POINT_INSET_DP = 8f

/** The face inside the head, and the white ring that separates it from the colour. */
private const val FACE_DP = 36f
private const val FACE_EDGE_DP = 2f

/** How far apart the faces of a group stand, which is what widens the head. */
private const val FACE_STEP_DP = 28f

/**
 * How many circles a pin ever has in it.
 *
 * Three at 28dp apart is a head of 104dp, about a third of the width of a
 * narrow device; a group of any size stops there and counts the rest instead.
 */
const val PIN_MAX_CIRCLES = 3

/** The label above the pin: its text, the box around it and the gap down to the head, in the page's numbers. */
private const val LABEL_TEXT_DP = 13f
private const val LABEL_PADDING_ACROSS_DP = 6f
private const val LABEL_PADDING_DOWN_DP = 2f
private const val LABEL_GAP_DP = 4f
private const val LABEL_CORNER_DP = 8f

/** The frame around the name of the member whose card is open, as the page draws it. */
private const val LABEL_FRAME_DP = 2f
private val LABEL_BACKGROUND = Color.argb(0xEB, 0xFF, 0xFF, 0xFF)
private val LABEL_TEXT_COLOUR = Color.rgb(0x20, 0x20, 0x20)

/** The letter a member without a picture is drawn as, in the size it is written at. */
private const val INITIAL_TEXT_DP = 17f

/** The count of the members a group has no room for, written smaller than a letter. */
private const val COUNTER_TEXT_DP = 14f

/**
 * One member as a pin draws them: their picture if it has arrived, and
 * otherwise the letter that stands in for it.
 */
data class PinFace(val picture: Bitmap?, val initial: String, val colour: Int, val stale: Boolean)

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
 * How large an account picture has to be to fill the face in a pin.
 *
 * Asked for at exactly this size rather than at whatever the account hosts:
 * the drawing is the only thing the picture is used for, and a portrait
 * several hundred points across would be decoded in full to be shrunk.
 */
fun faceSize(context: Context): Int = dpToPx(context, FACE_DP)

/**
 * The letter that stands in for a member without a picture.
 *
 * The name is what the group calls them, so its first letter is what they are
 * recognised by; an empty name leaves the pin with a face-shaped blank, which
 * is still a pin of their colour.
 */
fun memberInitial(name: String): String {
    val trimmed = name.trim()

    if (trimmed.isEmpty()) {
        return ""
    }

    // By what a reader sees as one character rather than by what Java stores
    // as one: a name beginning with an emoji has it written in two units here
    // and in one on the page, and half of a pair is not a letter at all.
    return trimmed.substring(0, trimmed.offsetByCodePoints(0, 1)).uppercase()
}

/**
 * How many of a group's faces are drawn, the rest being left to the counter.
 *
 * The web page repeats this, so that a group of five is the same marker
 * wherever it is looked at: three or fewer are all drawn, and a larger group
 * gives its last circle up to the count of who is not shown.
 */
fun facesShown(count: Int): Int = if (count > PIN_MAX_CIRCLES) PIN_MAX_CIRCLES - 1 else count

/**
 * A pin with a face in it.
 *
 * Drawn here instead of shipped as a resource because what varies is the
 * colour, the name and the picture, and one drawing routine is less to keep in
 * step than a file per member. The name is always up, the way a place name is
 * on a map: a pin on its own says somebody is there, not who.
 *
 * The text is part of the drawing rather than a label of its own because the
 * background is raster tiles: a style without a glyph server has no letters to
 * set a text layer in.
 *
 * The point at the bottom is the position. That is what the layer hangs the
 * drawing by, and it is why the face can be this large without covering the
 * place the member actually is.
 */
fun markerIcon(
    context: Context,
    name: String,
    colour: Int,
    stale: Boolean,
    /** The member's account picture, already loaded; a member without one gets their initial. */
    face: Bitmap?,
    /** The word to ring the pin with and write above the name, or null where nobody is sought. */
    soughtMark: String? = null,
    /** The colour to frame the name in while this member's card is open, or null for none. */
    frame: Int? = null,
): Bitmap {
    val body = if (stale) STALE_COLOUR else colour

    return pinBitmap(
        context = context,
        label = name,
        body = body,
        circles =
        listOf(
            Circle(
                picture = face,
                text = memberInitial(name),
                textColour = body,
                fade = stale,
            ),
        ),
        soughtMark = soughtMark,
        frame = frame,
    )
}

/**
 * One pin for the members who are standing in the same place.
 *
 * Wider than a single pin and in a colour of its own, with the faces side by
 * side: two people in one flat should be one thing on the map that says how
 * many they are, rather than two markers covering each other's labels.
 */
fun groupIcon(
    context: Context,
    /** What is written above the group, which is the count of the members in it. */
    label: String,
    /** The members whose faces are drawn, already cut down to what fits. */
    faces: List<PinFace>,
    /** The count of the members there was no room for, written as text; absent when all fit. */
    counter: String?,
    /** The word to ring the pin with, where anybody standing under it is being looked for. */
    soughtMark: String? = null,
): Bitmap {
    val body = if (faces.all { it.stale }) STALE_COLOUR else GROUP_COLOUR

    val circles =
        faces.map { face ->
            Circle(
                picture = face.picture,
                text = face.initial,
                textColour = face.colour,
                fade = face.stale,
            )
        }

    val rest =
        counter?.let {
            listOf(Circle(picture = null, text = it, textColour = body, fade = false))
        }.orEmpty()

    return pinBitmap(
        context = context,
        label = label,
        body = body,
        circles = circles + rest,
        soughtMark = soughtMark,
    )
}

/** What goes inside one circle of a pin: a picture, or text in place of one. */
private class Circle(val picture: Bitmap?, val text: String, val textColour: Int, val fade: Boolean)

/**
 * The drawing itself, whether it is one member or several.
 *
 * A group is the same pin with a longer head: the head is a capsule, so its
 * ends stay round however many circles are put in it, and a single member is
 * the case where the capsule is as wide as it is tall — a circle.
 */
private fun pinBitmap(
    context: Context,
    label: String,
    body: Int,
    circles: List<Circle>,
    /** The word to ring the head with and write above the name, or null to draw neither. */
    soughtMark: String? = null,
    /**
     * The colour to frame the name in, or null for none: the member whose card
     * is open stands on top of any neighbour in the same spot, and their name
     * lying over the neighbour's read as one line with it.
     */
    frame: Int? = null,
): Bitmap {
    val head = dpToPx(context, PIN_HEAD_DP)
    val step = dpToPx(context, FACE_STEP_DP)
    val faceSide = dpToPx(context, FACE_DP)
    val point = dpToPx(context, PIN_POINT_DP)
    val pointHalf = dpToPx(context, PIN_POINT_HALF_DP)
    val pointInset = dpToPx(context, PIN_POINT_INSET_DP)
    val edge = dpToPx(context, PIN_EDGE_DP)
    val gap = dpToPx(context, LABEL_GAP_DP)
    val across = dpToPx(context, LABEL_PADDING_ACROSS_DP)
    val down = dpToPx(context, LABEL_PADDING_DOWN_DP)
    val ring = if (soughtMark != null) dpToPx(context, SOUGHT_RING_DP) else 0
    val framed = if (frame != null) dpToPx(context, LABEL_FRAME_DP) else 0

    val headWidth = head + step * (circles.size - 1).coerceAtLeast(0)

    val textPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = LABEL_TEXT_COLOUR
            textSize = dpToPx(context, LABEL_TEXT_DP).toFloat()
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

    val metrics = textPaint.fontMetrics
    val labelHeight = (metrics.descent - metrics.ascent).toInt() + down * 2
    val labelWidth = textPaint.measureText(label).toInt() + across * 2

    // The capsule above the name, in white on the sought colour rather than
    // the name's dark on white: it is what the whole group is scanning the
    // map for, and it has to be found before the name is read.
    val soughtPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = dpToPx(context, LABEL_TEXT_DP).toFloat()
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
    val soughtMetrics = soughtPaint.fontMetrics
    val soughtHeight =
        if (soughtMark != null) {
            (soughtMetrics.descent - soughtMetrics.ascent).toInt() + down * 2
        } else {
            0
        }
    val soughtWidth =
        if (soughtMark != null) soughtPaint.measureText(soughtMark).toInt() + across * 2 else 0
    val soughtStack = if (soughtMark != null) soughtHeight + gap else 0

    val width = maxOf(labelWidth + framed * 2, soughtWidth, headWidth + (edge + ring) * 2)
    val headTop = soughtStack + framed + labelHeight + framed + gap + edge + ring
    val height = headTop + head + point

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
    val corner = dpToPx(context, LABEL_CORNER_DP).toFloat()

    if (soughtMark != null) {
        val soughtBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SOUGHT_COLOUR }

        canvas.drawRoundRect(
            middle - soughtWidth / 2f,
            0f,
            middle + soughtWidth / 2f,
            soughtHeight.toFloat(),
            corner,
            corner,
            soughtBoxPaint,
        )
        canvas.drawText(soughtMark, middle, down - soughtMetrics.ascent, soughtPaint)
    }

    val labelTop = (soughtStack + framed).toFloat()
    val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = LABEL_BACKGROUND }

    if (frame != null) {
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = frame }

        canvas.drawRoundRect(
            middle - labelWidth / 2f - framed,
            labelTop - framed,
            middle + labelWidth / 2f + framed,
            labelTop + labelHeight + framed,
            corner + framed,
            corner + framed,
            framePaint,
        )
    }

    canvas.drawRoundRect(
        middle - labelWidth / 2f,
        labelTop,
        middle + labelWidth / 2f,
        labelTop + labelHeight,
        corner,
        corner,
        boxPaint,
    )

    canvas.drawText(label, middle, labelTop + down - metrics.ascent, textPaint)

    val headCentre = headTop + head / 2f
    val shape =
        pinShape(
            centreX = middle,
            centreY = headCentre,
            width = headWidth.toFloat(),
            height = head.toFloat(),
            pointHalf = pointHalf.toFloat(),
            point = point.toFloat(),
            inset = pointInset.toFloat(),
        )

    if (soughtMark != null) {
        // Drawn before the outline and the fill, which cover the part of it
        // that lies inside the head: what is left once they are drawn is the
        // ring outside the white edge, the way a box-shadow sits behind an
        // element on the page.
        val ringPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = SOUGHT_COLOUR
                style = Paint.Style.STROKE
                strokeWidth = ring.toFloat()
            }
        val ringRadius = head / 2f + edge + ring / 2f

        canvas.drawRoundRect(
            middle - headWidth / 2f - edge - ring / 2f,
            headCentre - ringRadius,
            middle + headWidth / 2f + edge + ring / 2f,
            headCentre + ringRadius,
            ringRadius,
            ringRadius,
            ringPaint,
        )
    }

    val outline =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            // The white edge is what keeps the pin visible over a dark road or
            // a green park; without it a marker can sink into the tiles.
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = edge * 2f
        }

    canvas.drawPath(shape, outline)
    canvas.drawPath(shape, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = body })

    val faceInset = (head - faceSide) / 2f
    val first = middle - headWidth / 2f + faceInset + faceSide / 2f

    circles.forEachIndexed { index, circle ->
        drawFace(
            canvas = canvas,
            centreX = first + step * index,
            centreY = headCentre,
            radius = faceSide / 2f,
            ring = dpToPx(context, FACE_EDGE_DP).toFloat(),
            circle = circle,
            textSize =
            dpToPx(
                context,
                if (circle.picture == null && circles.size > 1 && index == circles.lastIndex) {
                    COUNTER_TEXT_DP
                } else {
                    INITIAL_TEXT_DP
                },
            ).toFloat(),
        )
    }

    return bitmap
}

/**
 * The head and the point as one outline.
 *
 * One path rather than two shapes, because the white edge is stroked around
 * it: two strokes would leave a line across the pin where the head and the
 * point meet. The head is a capsule so that a group of three keeps the same
 * round ends a single member has, instead of flattening into an oval.
 */
private fun pinShape(
    centreX: Float,
    centreY: Float,
    width: Float,
    height: Float,
    pointHalf: Float,
    point: Float,
    inset: Float,
): Path {
    val radius = height / 2f

    val head =
        Path().apply {
            addRoundRect(
                RectF(
                    centreX - width / 2f,
                    centreY - radius,
                    centreX + width / 2f,
                    centreY + radius,
                ),
                radius,
                radius,
                Path.Direction.CW,
            )
        }

    val tip =
        Path().apply {
            moveTo(centreX - pointHalf, centreY + radius - inset)
            lineTo(centreX + pointHalf, centreY + radius - inset)
            lineTo(centreX, centreY + radius + point)
            close()
        }

    head.op(tip, Path.Op.UNION)

    return head
}

/**
 * What is inside one circle of the head: the account picture, or the text that
 * stands in for it.
 *
 * The picture is cropped to a square before it is drawn round, so that a
 * portrait taken in any shape keeps its proportions instead of being squeezed
 * into the circle.
 */
private fun drawFace(
    canvas: Canvas,
    centreX: Float,
    centreY: Float,
    radius: Float,
    ring: Float,
    circle: Circle,
    textSize: Float,
) {
    val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

    canvas.drawCircle(centreX, centreY, radius, white)

    val picture = circle.picture

    // A picture that cannot be drawn is the same as no picture: the loader
    // hands out what its cache holds, and a bitmap it has since freed would
    // otherwise take the whole screen down rather than one face.
    if (picture == null || picture.isRecycled) {
        if (circle.text.isEmpty()) {
            return
        }

        val letter =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = circle.textColour
                this.textSize = textSize
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }

        val metrics = letter.fontMetrics

        canvas.drawText(
            circle.text,
            centreX,
            centreY - (metrics.ascent + metrics.descent) / 2f,
            letter,
        )

        return
    }

    val side = min(picture.width, picture.height)
    val source =
        Rect(
            (picture.width - side) / 2,
            (picture.height - side) / 2,
            (picture.width + side) / 2,
            (picture.height + side) / 2,
        )

    val target = RectF(centreX - radius, centreY - radius, centreX + radius, centreY + radius)
    val paint =
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            if (circle.fade) {
                // The same fading the colour gets: an old position should look
                // old whether the member has a picture or not.
                colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
            }
        }

    canvas.save()
    canvas.clipPath(Path().apply { addCircle(centreX, centreY, radius, Path.Direction.CW) })
    canvas.drawBitmap(picture, source, target, paint)
    canvas.restore()

    canvas.drawCircle(
        centreX,
        centreY,
        radius - ring / 2f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = ring
        },
    )
}

private fun dpToPx(context: Context, dp: Float): Int = TypedValue
    .applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics)
    .toInt()
