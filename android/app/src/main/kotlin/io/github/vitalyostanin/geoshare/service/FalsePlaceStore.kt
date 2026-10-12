package io.github.vitalyostanin.geoshare.service

import android.content.Context
import io.github.vitalyostanin.geoshare.collection.FALSE_PLACES_KEPT
import io.github.vitalyostanin.geoshare.collection.FalsePlace
import io.github.vitalyostanin.geoshare.collection.falsePlaceStands
import io.github.vitalyostanin.geoshare.log.AppLog
import java.io.File

/**
 * The group's false places as they survive the process being killed.
 *
 * The list lives in the database, and the database is reached over a network
 * the device may not have: a device started in a cellar, or one whose data has
 * run out, would otherwise begin with no places at all — and the first fix of
 * a session is exactly the one the false places exist to catch, because it has
 * nothing earlier to be compared with.
 *
 * A plain file of one line per place, for the same reason the trail is one:
 * four values, no schema, no migration, and every failure logged and
 * swallowed. Losing it costs one subscription's worth of waiting.
 */
class FalsePlaceStore(private val context: Context) {
    private val file: File get() = File(context.filesDir, FILE_NAME)

    fun read(): List<FalsePlace> = try {
        if (!file.exists()) {
            emptyList()
        } else {
            file.readLines().mapNotNull(::falsePlaceOf).take(FALSE_PLACES_KEPT)
        }
    } catch (error: Exception) {
        AppLog.w(TAG, "the false places could not be read", error)

        emptyList()
    }

    fun write(places: List<FalsePlace>) {
        try {
            file.writeText(places.joinToString("\n", transform = ::falsePlaceLine))
        } catch (error: Exception) {
            AppLog.w(TAG, "the false places could not be written", error)
        }
    }

    private companion object {
        const val TAG = "geoshare.falseplaces"
        const val FILE_NAME = "false-places.csv"
    }
}

private const val FALSE_PLACE_PARTS_IN_A_LINE = 4

/**
 * A line of the false places' file. The name is the last field, so that a
 * comma inside it cannot be mistaken for the end of a number; a line break
 * inside it, which the rules do not forbid, is written as a space, or it
 * would end the line and lose the place.
 */
internal fun falsePlaceLine(place: FalsePlace): String {
    val named = place.named.replace(Regex("[\\r\\n]+"), " ")

    return "${place.latitude},${place.longitude},${place.radiusMetres},$named"
}

/** The place a line of the file stands for, or null for a line that is not one or a place that cannot be. */
internal fun falsePlaceOf(line: String): FalsePlace? {
    val parts = line.split(",", limit = FALSE_PLACE_PARTS_IN_A_LINE)

    if (parts.size < FALSE_PLACE_PARTS_IN_A_LINE) {
        return null
    }

    val place =
        FalsePlace(
            latitude = parts[0].toDoubleOrNull() ?: return null,
            longitude = parts[1].toDoubleOrNull() ?: return null,
            radiusMetres = parts[2].toDoubleOrNull() ?: return null,
            named = parts[3],
        )

    return place.takeIf(::falsePlaceStands)
}
